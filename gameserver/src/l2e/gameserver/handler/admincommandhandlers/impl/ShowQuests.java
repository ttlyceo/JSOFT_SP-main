/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 *
 */
package l2e.gameserver.handler.admincommandhandlers.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExShowQuestMark;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.QuestList;

public class ShowQuests implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_charquestmenu", "admin_setcharquest"
	};

	private static final String[] _states =
	{
	        "CREATED", "STARTED", "COMPLETED"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final String[] cmdParams = command.split(" ");
		Player target = null;
		GameObject targetObject = null;
		final String[] val = new String[4];
		val[0] = null;
		
		if (cmdParams.length > 1)
		{
			target = GameObjectsStorage.getPlayer(cmdParams[1]);
			if (cmdParams.length > 2)
			{
				if (cmdParams[2].equals("0"))
				{
					val[0] = "var";
					val[1] = "Start";
				}
				if (cmdParams[2].equals("1"))
				{
					val[0] = "var";
					val[1] = "Started";
				}
				if (cmdParams[2].equals("2"))
				{
					val[0] = "var";
					val[1] = "Completed";
				}
				if (cmdParams[2].equals("3"))
				{
					val[0] = "full";
				}
				if (cmdParams[2].indexOf("_") != -1)
				{
					val[0] = "name";
					val[1] = cmdParams[2];
				}
				if (cmdParams.length > 3)
				{
					if (cmdParams[3].equals("custom"))
					{
						val[0] = "custom";
						val[1] = cmdParams[2];
					}
				}
			}
		}
		else
		{
			targetObject = activeChar.getTarget();
			
			if ((targetObject != null) && targetObject.isPlayer())
			{
				target = targetObject.getActingPlayer();
			}
		}
		
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}
		
		if (command.startsWith("admin_charquestmenu"))
		{
			if (val[0] != null)
			{
				showQuestMenu(target, activeChar, val);
			}
			else
			{
				showFirstQuestMenu(target, activeChar);
			}
		}
		else if (command.startsWith("admin_setcharquest"))
		{
			if (cmdParams.length >= 5)
			{
				val[0] = cmdParams[2];
				val[1] = cmdParams[3];
				val[2] = cmdParams[4];
				if (cmdParams.length == 6)
				{
					val[3] = cmdParams[5];
				}
				setQuestVar(target, activeChar, val);
			}
			else
			{
				return false;
			}
		}
		return true;
	}
	
	private static void showFirstQuestMenu(Player target, Player actor)
	{
		final var replyMSG = new StringBuilder("<html><body><table width=270><tr><td width=45><button value=\"Main\" action=\"bypass -h admin_admin\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=180><center>Player: " + target.getName(actor.getLang()) + "</center></td><td width=45><button value=\"Back\" action=\"bypass -h admin_admin6\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		final var adminReply = new NpcHtmlMessage(5);
		final var ID = target.getObjectId();
		
		replyMSG.append("Quest Menu for <font color=\"LEVEL\">" + target.getName(actor.getLang()) + "</font> (ID:" + ID + ")<br><center>");
		replyMSG.append("<table width=250><tr><td><button value=\"CREATED\" action=\"bypass -h admin_charquestmenu " + target.getName(actor.getLang()) + " 0\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"STARTED\" action=\"bypass -h admin_charquestmenu " + target.getName(actor.getLang()) + " 1\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"COMPLETED\" action=\"bypass -h admin_charquestmenu " + target.getName(actor.getLang()) + " 2\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><br><button value=\"All\" action=\"bypass -h admin_charquestmenu " + target.getName(actor.getLang()) + " 3\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><br><br>Manual Edit by Quest number:<br></td></tr>");
		replyMSG.append("<tr><td><edit var=\"qn\" width=50 height=15><br><button value=\"Edit\" action=\"bypass -h admin_charquestmenu " + target.getName(actor.getLang()) + " $qn custom\" width=50 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("</table></center></body></html>");
		adminReply.setHtml(actor, replyMSG.toString());
		actor.sendPacket(adminReply);
	}
	
	private static void showQuestMenu(Player target, Player actor, String[] val)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			
			final var ID = target.getObjectId();
			
			final var replyMSG = new StringBuilder("<html><body>");
			final var adminReply = new NpcHtmlMessage(5);
			
			if (val[0].equals("full"))
			{
				replyMSG.append("<table width=250><tr><td>Full Quest List for <font color=\"LEVEL\">" + target.getName(actor.getLang()) + "</font> (ID:" + ID + ")</td></tr>");
				statement = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId='" + ID + "' AND var='<state>' ORDER by name");
				statement.execute();
				rset = statement.getResultSet();
				while (rset.next())
				{
					replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName(actor.getLang()) + " " + rset.getString(1) + "\">" + rset.getString(1) + "</a></td></tr>");
				}
				replyMSG.append("</table></body></html>");
			}
			else if (val[0].equals("name"))
			{
				final var qs = target.getQuestState(val[1]);
				final var state = (qs != null) ? _states[qs.getState()] : "CREATED";
				replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName(actor.getLang()) + "</font><br>Quest: <font color=\"LEVEL\">" + val[1] + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
				replyMSG.append("<center><table width=250><tr><td>Var</td><td>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
				statement = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId='" + ID + "' and name='" + val[1] + "'");
				statement.execute();
				rset = statement.getResultSet();
				while (rset.next())
				{
					final var var_name = rset.getString(1);
					if (var_name.equals("<state>"))
					{
						continue;
					}
					replyMSG.append("<tr><td>" + var_name + "</td><td>" + rset.getString(2) + "</td><td><edit var=\"var" + var_name + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + val[1] + " " + var_name + " $var" + var_name + "\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + val[1] + " " + var_name + " delete\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
				}
				replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
				replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + val[1] + " state COMPLETED 1\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + val[1] + " state COMPLETED 0\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
				replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + val[1] + " state DELETE\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				replyMSG.append("</center></body></html>");
			}
			else if (val[0].equals("var"))
			{
				replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName(actor.getLang()) + "</font><br>Quests with state: <font color=\"LEVEL\">" + val[1] + "</font><br>");
				replyMSG.append("<table width=250>");
				statement = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId='" + ID + "' and var='<state>' and value='" + val[1] + "'");
				statement.execute();
				rset = statement.getResultSet();
				while (rset.next())
				{
					replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName(actor.getLang()) + " " + rset.getString(1) + "\">" + rset.getString(1) + "</a></td></tr>");
				}
				replyMSG.append("</table></body></html>");
			}
			else if (val[0].equals("custom"))
			{
				var exqdb = true;
				var exqch = true;
				final var qnumber = Integer.parseInt(val[1]);
				String state = null;
				String qname = null;
				QuestState qs = null;
				
				final var quest = QuestManager.getInstance().getQuest(qnumber);
				if (quest != null)
				{
					qname = quest.getName();
					qs = target.getQuestState(qname);
				}
				else
				{
					exqdb = false;
				}
				
				if (qs != null)
				{
					state = _states[qs.getState()];
				}
				else
				{
					exqch = false;
					state = "N/A";
				}
				
				if (exqdb)
				{
					if (exqch)
					{
						replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName(actor.getLang()) + "</font><br>Quest: <font color=\"LEVEL\">" + qname + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
						replyMSG.append("<center><table width=250><tr><td>Var</td><td>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
						statement = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId='" + ID + "' and name='" + qname + "'");
						statement.execute();
						rset = statement.getResultSet();
						while (rset.next())
						{
							final String var_name = rset.getString(1);
							if (var_name.equals("<state>"))
							{
								continue;
							}
							replyMSG.append("<tr><td>" + var_name + "</td><td>" + rset.getString(2) + "</td><td><edit var=\"var" + var_name + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + qname + " " + var_name + " $var" + var_name + "\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + qname + " " + var_name + " delete\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
						}
						replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
						replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + qname + " state COMPLETED 1\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
						replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + qname + " state COMPLETED 0\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
						replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + qname + " state DELETE\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("</center></body></html>");
					}
					else
					{
						replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName(actor.getLang()) + "</font><br>Quest: <font color=\"LEVEL\">" + qname + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
						replyMSG.append("<center>Start this Quest for player:<br>");
						replyMSG.append("<button value=\"Create Quest\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + qnumber + " state CREATE\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>");
						replyMSG.append("<font color=\"ee0000\">Only for Unrepeateble quests:</font><br>");
						replyMSG.append("<button value=\"Create & Complete\" action=\"bypass -h admin_setcharquest " + target.getName(actor.getLang()) + " " + qnumber + " state CC\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>");
						replyMSG.append("</center></body></html>");
					}
				}
				else
				{
					replyMSG.append("<center><font color=\"ee0000\">Quest with number </font><font color=\"LEVEL\">" + qnumber + "</font><font color=\"ee0000\"> doesn't exist!</font></center></body></html>");
				}
			}
			adminReply.setHtml(actor, replyMSG.toString());
			actor.sendPacket(adminReply);
		}
		catch (final Exception e)
		{
			actor.sendMessage("There was an error.");
			_log.warn(ShowQuests.class.getSimpleName() + ": " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private static void setQuestVar(Player target, Player actor, String[] val)
	{
		var qs = target.getQuestState(val[0]);
		final String[] outval = new String[3];
		
		if (val[1].equals("state"))
		{
			if (val[2].equals("COMPLETED"))
			{
				qs.exitQuest((val[3].equals("1")) ? true : false);
			}
			else if (val[2].equals("DELETE"))
			{
				Quest.deleteQuestInDb(qs, true);
				target.sendPacket(new QuestList(target));
				target.sendPacket(new ExShowQuestMark(qs.getQuest().getId(), qs.getCond()));
			}
			else if (val[2].equals("CREATE"))
			{
				qs = QuestManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
				qs.setState(State.STARTED);
				qs.set("cond", "1");
				target.sendPacket(new QuestList(target));
				target.sendPacket(new ExShowQuestMark(qs.getQuest().getId(), qs.getCond()));
				val[0] = qs.getQuest().getName();
			}
			else if (val[2].equals("CC"))
			{
				qs = QuestManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
				qs.exitQuest(false);
				target.sendPacket(new QuestList(target));
				target.sendPacket(new ExShowQuestMark(qs.getQuest().getId(), qs.getCond()));
				val[0] = qs.getQuest().getName();
			}
		}
		else
		{
			if (val[2].equals("delete"))
			{
				qs.unset(val[1]);
			}
			else
			{
				qs.set(val[1], val[2]);
			}
			target.sendPacket(new QuestList(target));
			target.sendPacket(new ExShowQuestMark(qs.getQuest().getId(), qs.getCond()));
		}
		actor.sendMessage("");
		outval[0] = "name";
		outval[1] = val[0];
		showQuestMenu(target, actor, outval);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}