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

import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.StringUtil;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeSkillList;

public class Skills implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_show_skills", "admin_remove_skills", "admin_skill_list", "admin_skill_index", "admin_add_skill", "admin_remove_skill", "admin_get_skills", "admin_reset_skills", "admin_give_all_skills", "admin_give_all_skills_fs", "admin_give_all_clan_skills", "admin_remove_all_skills", "admin_add_clan_skill", "admin_setskill", "admin_refresh_skills"
	};
	
	private static Skill[] adminSkills;
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);

		if (command.equals("admin_show_skills"))
		{
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_remove_skills"))
		{
			try
			{
				final String val = command.substring(20);
				removeSkillsPage(activeChar, Integer.parseInt(val));
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.startsWith("admin_skill_list"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/skills.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_skill_index"))
		{
			try
			{
				final String val = command.substring(18);
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/skills/" + val + ".htm");
				activeChar.sendPacket(adminhtm);
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.startsWith("admin_add_skill"))
		{
			try
			{
				final String val = command.substring(15);
				adminAddSkill(activeChar, val);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //add_skill <skill_id> <level>");
			}
		}
		else if (command.startsWith("admin_remove_skill"))
		{
			try
			{
				final String id = command.substring(19);
				final int idval = Integer.parseInt(id);
				adminRemoveSkill(activeChar, idval);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //remove_skill <skill_id>");
			}
		}
		else if (command.equals("admin_get_skills"))
		{
			adminGetSkills(activeChar);
		}
		else if (command.equals("admin_reset_skills"))
		{
			adminResetSkills(activeChar);
		}
		else if (command.equals("admin_give_all_skills"))
		{
			adminGiveAllSkills(activeChar, false);
		}
		else if (command.equals("admin_give_all_skills_fs"))
		{
			adminGiveAllSkills(activeChar, true);
		}
		else if (command.equals("admin_give_all_clan_skills"))
		{
			adminGiveAllClanSkills(activeChar);
		}
		else if (command.equals("admin_remove_all_skills"))
		{
			final GameObject target = activeChar.getTarget();
			if ((target == null) || !target.isPlayer())
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return false;
			}
			final Player player = target.getActingPlayer();
			for (final Skill skill : player.getAllSkills())
			{
				player.removeSkill(skill);
			}
			activeChar.sendMessage("You have removed all skills from " + player.getName(null) + ".");
			player.sendMessage("Admin removed all skills from you.");
			player.sendSkillList(false);
			player.broadcastUserInfo(true);
		}
		else if (command.startsWith("admin_add_clan_skill"))
		{
			try
			{
				final String[] val = command.split(" ");
				adminAddClanSkill(activeChar, Integer.parseInt(val[1]), Integer.parseInt(val[2]));
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //add_clan_skill <skill_id> <level>");
			}
		}
		else if (command.startsWith("admin_setskill"))
		{
			final String[] split = command.split(" ");
			final int id = Integer.parseInt(split[1]);
			final int lvl = Integer.parseInt(split[2]);
			final Skill skill = SkillsParser.getInstance().getInfo(id, lvl);
			activeChar.addSkill(skill);
			activeChar.sendSkillList(false);
			activeChar.sendMessage("You added yourself skill " + skill.getName(activeChar.getLang()) + "(" + id + ") level " + lvl);
		}
		else if (command.startsWith("admin_refresh_skills"))
		{
			final GameObject target = activeChar.getTarget();
			if ((target == null) || !target.isPlayer())
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return false;
			}
			final Player player = target.getActingPlayer();
			if (player != null)
			{
				player.resetReuse();
				player.sendSkillList(true);
				player.sendMessage("Your skills reuse refreshed!");
			}
		}
		return true;
	}

	private void adminGiveAllSkills(Player activeChar, boolean includedByFs)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		final Player player = target.getActingPlayer();
		activeChar.sendMessage("You gave " + player.giveAvailableSkills(includedByFs, true) + " skills to " + player.getName(null));
		player.sendSkillList(false);
	}

	private void adminGiveAllClanSkills(Player activeChar)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		final Player player = target.getActingPlayer();
		final Clan clan = player.getClan();
		if (clan == null)
		{
			activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
			return;
		}

		if (!player.isClanLeader())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
			sm.addPcName(player);
			activeChar.sendPacket(sm);
		}

		final List<SkillLearn> skills = SkillTreesParser.getInstance().getAvailablePledgeSkills(clan);
		final SkillsParser st = SkillsParser.getInstance();
		for (final SkillLearn s : skills)
		{
			clan.addNewSkill(st.getInfo(s.getId(), s.getLvl()));
		}

		clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
		for (final Player member : clan.getOnlineMembers(0))
		{
			member.sendSkillList(false);
		}

		activeChar.sendMessage("You gave " + skills.size() + " skills to " + player.getName(null) + "'s clan " + clan.getName() + ".");
		player.sendMessage("Your clan received " + skills.size() + " skills.");
	}

	private void removeSkillsPage(Player activeChar, int page)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}

		final Player player = target.getActingPlayer();
		final Skill[] skills = player.getAllSkills().toArray(new Skill[player.getAllSkills().size()]);

		final int maxSkillsPerPage = 10;
		int maxPages = skills.length / maxSkillsPerPage;
		if (skills.length > (maxSkillsPerPage * maxPages))
		{
			maxPages++;
		}

		if (page > maxPages)
		{
			page = maxPages;
		}

		final int skillsStart = maxSkillsPerPage * page;
		int skillsEnd = skills.length;
		if ((skillsEnd - skillsStart) > maxSkillsPerPage)
		{
			skillsEnd = skillsStart + maxSkillsPerPage;
		}

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final StringBuilder replyMSG = StringUtil.startAppend(500 + (maxPages * 50) + (((skillsEnd - skillsStart) + 1) * 50), "<html><body>" + "<table width=260><tr>" + "<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "<td width=180><center>Character Selection Menu</center></td>" + "<td width=40><button value=\"Back\" action=\"bypass -h admin_show_skills\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "</tr></table>" + "<br><br>" + "<center>Editing <font color=\"LEVEL\">", player.getName(null), "</font></center>" + "<br><table width=270><tr><td>Lv: ", String.valueOf(player.getLevel()), " ", ClassListParser.getInstance().getClass(player.getClassId()).getClientCode(), "</td></tr></table>" + "<br><table width=270><tr><td>Note: Dont forget that modifying players skills can</td></tr>" + "<tr><td>ruin the game...</td></tr></table>" + "<br><center>Click on the skill you wish to remove:</center>" + "<br>" + "<center><table width=270><tr>");

		for (int x = 0; x < maxPages; x++)
		{
			final int pagenr = x + 1;
			StringUtil.append(replyMSG, "<td><a action=\"bypass -h admin_remove_skills ", String.valueOf(x), "\">Page ", String.valueOf(pagenr), "</a></td>");
		}

		replyMSG.append("</tr></table></center>" + "<br><table width=270>" + "<tr><td width=80>Name:</td><td width=60>Level:</td><td width=40>Id:</td></tr>");

		for (int i = skillsStart; i < skillsEnd; i++)
		{
			StringUtil.append(replyMSG, "<tr><td width=80><a action=\"bypass -h admin_remove_skill ", String.valueOf(skills[i].getId()), "\">", skills[i].getName(null), "</a></td><td width=60>", String.valueOf(skills[i].getLevel()), "</td><td width=40>", String.valueOf(skills[i].getId()), "</td></tr>");
		}

		replyMSG.append("</table>" + "<br><center><table>" + "Remove skill by ID :" + "<tr><td>Id: </td>" + "<td><edit var=\"id_to_remove\" width=110></td></tr>" + "</table></center>" + "<center><button value=\"Remove skill\" action=\"bypass -h admin_remove_skill $id_to_remove\" width=110 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" + "<br><center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" + "</body></html>");
		adminReply.setHtml(activeChar, replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showMainPage(Player activeChar)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		final Player player = target.getActingPlayer();
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/charskills.htm");
		adminReply.replace("%name%", player.getName(null));
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%class%", ClassListParser.getInstance().getClass(player.getClassId()).getClientCode());
		activeChar.sendPacket(adminReply);
	}

	private void adminGetSkills(Player activeChar)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		final Player player = target.getActingPlayer();
		if (player.getName(null).equals(activeChar.getName(null)))
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		}
		else
		{
			final Skill[] skills = player.getAllSkills().toArray(new Skill[player.getAllSkills().size()]);
			adminSkills = activeChar.getAllSkills().toArray(new Skill[activeChar.getAllSkills().size()]);
			for (final Skill skill : adminSkills)
			{
				activeChar.removeSkill(skill);
			}
			for (final Skill skill : skills)
			{
				activeChar.addSkill(skill, true);
			}
			activeChar.sendMessage("You now have all the skills of " + player.getName(null) + ".");
			activeChar.sendSkillList(false);
		}
		showMainPage(activeChar);
	}

	private void adminResetSkills(Player activeChar)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		final Player player = target.getActingPlayer();
		if (adminSkills == null)
		{
			activeChar.sendMessage("You must get the skills of someone in order to do this.");
		}
		else
		{
			final Skill[] skills = player.getAllSkills().toArray(new Skill[player.getAllSkills().size()]);
			for (final Skill skill : skills)
			{
				player.removeSkill(skill);
			}
			for (final Skill skill : activeChar.getAllSkills())
			{
				player.addSkill(skill, true);
			}
			for (final Skill skill : skills)
			{
				activeChar.removeSkill(skill);
			}
			for (final Skill skill : adminSkills)
			{
				activeChar.addSkill(skill, true);
			}
			player.sendMessage("[GM]" + activeChar.getName(null) + " updated your skills.");
			activeChar.sendMessage("You now have all your skills back.");
			adminSkills = null;
			activeChar.sendSkillList(false);
			player.sendSkillList(false);
		}
		showMainPage(activeChar);
	}

	private void adminAddSkill(Player activeChar, String val)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			showMainPage(activeChar);
			return;
		}
		final Player player = target.getActingPlayer();
		final StringTokenizer st = new StringTokenizer(val);
		if (st.countTokens() != 2)
		{
			showMainPage(activeChar);
		}
		else
		{
			Skill skill = null;
			try
			{
				final String id = st.nextToken();
				final String level = st.nextToken();
				final int idval = Integer.parseInt(id);
				final int levelval = Integer.parseInt(level);
				skill = SkillsParser.getInstance().getInfo(idval, levelval);
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
			if (skill != null)
			{
				final String name = skill.getName(activeChar.getLang());
				player.sendMessage("Admin gave you the skill " + name + ".");
				player.addSkill(skill, true);
				player.sendSkillList(false);
				activeChar.sendMessage("You gave the skill " + name + " to " + player.getName(null) + ".");
				if (Config.DEBUG)
				{
					_log.info("[GM]" + activeChar.getName(null) + " gave skill " + name + " to " + player.getName(null) + ".");
				}
				activeChar.sendSkillList(false);
			}
			else
			{
				activeChar.sendMessage("Error: there is no such skill.");
			}
			showMainPage(activeChar);
		}
	}

	private void adminRemoveSkill(Player activeChar, int idval)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		final Player player = target.getActingPlayer();
		final Skill skill = SkillsParser.getInstance().getInfo(idval, player.getSkillLevel(idval));
		if (skill != null)
		{
			final String skillname = skill.getName(activeChar.getLang());
			player.sendMessage("Admin removed the skill " + skillname + " from your skills list.");
			player.removeSkill(skill);
			activeChar.sendMessage("You removed the skill " + skillname + " from " + player.getName(null) + ".");
			if (Config.DEBUG)
			{
				_log.info("[GM]" + activeChar.getName(null) + " removed skill " + skill.getName(null) + " from " + player.getName(null) + ".");
			}
			activeChar.sendSkillList(false);
		}
		else
		{
			activeChar.sendMessage("Error: there is no such skill.");
		}
		removeSkillsPage(activeChar, 0);
	}

	private void adminAddClanSkill(Player activeChar, int id, int level)
	{
		final GameObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			showMainPage(activeChar);
			return;
		}
		final Player player = target.getActingPlayer();
		if (!player.isClanLeader())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
			sm.addString(player.getName(null));
			activeChar.sendPacket(sm);
			showMainPage(activeChar);
			return;
		}
		if ((id < 370) || (id > 391) || (level < 1) || (level > 3))
		{
			activeChar.sendMessage("Usage: //add_clan_skill <skill_id> <level>");
			showMainPage(activeChar);
			return;
		}

		final Skill skill = SkillsParser.getInstance().getInfo(id, level);
		if (skill == null)
		{
			activeChar.sendMessage("Error: there is no such skill.");
			return;
		}

		final String skillname = skill.getName(activeChar.getLang());
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
		sm.addSkillName(skill);
		player.sendPacket(sm);
		final Clan clan = player.getClan();
		clan.broadcastToOnlineMembers(sm);
		clan.addNewSkill(skill);
		activeChar.sendMessage("You gave the Clan Skill: " + skillname + " to the clan " + clan.getName() + ".");

		clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
		for (final Player member : clan.getOnlineMembers(0))
		{
			member.sendSkillList(false);
		}
		showMainPage(activeChar);
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}