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
package l2e.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.data.parser.InitialShortcutParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.Clan.SubPledge;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.ClassType;
import l2e.gameserver.model.base.PlayerClass;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.base.SubClass;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Duel;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AcquireSkillDone;
import l2e.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import l2e.gameserver.network.serverpackets.MagicSkillLaunched;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.ShortCutInit;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class VillageMasterInstance extends NpcInstance
{
	public VillageMasterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		setInstanceType(InstanceType.VillageMasterInstance);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/villagemaster/" + pom + ".htm";
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final String[] commandStr = command.split(" ");
		final String actualCommand = commandStr[0];
		
		String cmdParams = "";
		String cmdParams2 = "";
		
		if (commandStr.length >= 2)
		{
			cmdParams = commandStr[1];
		}
		if (commandStr.length >= 3)
		{
			cmdParams2 = commandStr[2];
		}
		
		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			if (!isValidName(cmdParams))
			{
				player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
				return;
			}
			
			ClanHolder.getInstance().createClan(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("create_academy"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			createSubPledge(player, cmdParams, null, Clan.SUBUNIT_ACADEMY, 5);
		}
		else if (actualCommand.equalsIgnoreCase("rename_pledge"))
		{
			if (cmdParams.isEmpty() || cmdParams2.isEmpty())
			{
				return;
			}
			
			renameSubPledge(player, Integer.parseInt(cmdParams), cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_royal"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			createSubPledge(player, cmdParams, cmdParams2, Clan.SUBUNIT_ROYAL1, 6);
		}
		else if (actualCommand.equalsIgnoreCase("create_knight"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			createSubPledge(player, cmdParams, cmdParams2, Clan.SUBUNIT_KNIGHT1, 7);
		}
		else if (actualCommand.equalsIgnoreCase("assign_subpl_leader"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			assignSubPledgeLeader(player, cmdParams, cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			if (player.getClan() == null)
			{
				player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
			}
			else
			{
				player.getClan().createAlly(player, cmdParams);
			}
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			player.getClan().dissolveAlly(player);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			dissolveClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			if (player.getName(null).equalsIgnoreCase(cmdParams))
			{
				return;
			}
			
			final Clan clan = player.getClan();
			final ClanMember member = clan.getClanMember(cmdParams);
			if (member == null)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
				sm.addString(cmdParams);
				player.sendPacket(sm);
				return;
			}
			
			if (!member.isOnline())
			{
				player.sendPacket(SystemMessageId.INVITED_USER_NOT_ONLINE);
				return;
			}
			
			if (member.getPlayerInstance().isAcademyMember())
			{
				player.sendPacket(SystemMessageId.RIGHT_CANT_TRANSFERRED_TO_ACADEMY_MEMBER);
				return;
			}
			
			if (Config.ALT_CLAN_LEADER_INSTANT_ACTIVATION)
			{
				clan.setNewLeader(member);
			}
			else
			{
				final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
				if (clan.getNewLeaderId() == 0)
				{
					clan.setNewLeaderId(member.getObjectId(), true);
					msg.setFile(player, player.getLang(), "data/html/scripts/village_master/Clan/9000-07-success.htm");
				}
				else
				{
					msg.setFile(player, player.getLang(), "data/html/scripts/village_master/Clan/9000-07-in-progress.htm");
				}
				player.sendPacket(msg);
			}
		}
		else if (actualCommand.equalsIgnoreCase("cancel_clan_leader_change"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			final Clan clan = player.getClan();
			final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
			if (clan.getNewLeaderId() != 0)
			{
				clan.setNewLeaderId(0, true);
				msg.setFile(player, player.getLang(), "data/html/scripts/village_master/Clan/9000-07-canceled.htm");
			}
			else
			{
				msg.setHtml(player, "<html><body>You don't have clan leader delegation applications submitted yet!</body></html>");
			}
			player.sendPacket(msg);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
		{
			recoverClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (player.getClan().levelUpClan(player))
			{
				player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 0, 0));
				player.broadcastPacket(new MagicSkillLaunched(player, 5103, 1));
			}
		}
		else if (actualCommand.equalsIgnoreCase("learn_clan_skills"))
		{
			showPledgeSkillList(player);
		}
		else if (command.startsWith("Subclass"))
		{
			if (player.isActionsDisabled() || player.isAllSkillsDisabled())
			{
				player.sendPacket(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
				return;
			}
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			if (player.getTransformation() != null)
			{
				html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_NoTransformed.htm");
				player.sendPacket(html);
				return;
			}
			
			if (player.hasSummon())
			{
				html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_NoSummon.htm");
				player.sendPacket(html);
				return;
			}
			
			if (!player.isInventoryUnder90(true))
			{
				player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_INVENTORY_FULL);
				return;
			}
			
			if (player.getWeightPenalty() >= 2)
			{
				player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_OVERWEIGHT);
				return;
			}
			
			if (!player.isInsideZone(ZoneId.PEACE) || player.getDuelState() != Duel.DUELSTATE_NODUEL)
			{
				player.sendMessage((new ServerMessage("CommunityGeneral.CANT_SUB", player.getLang())).toString());
				return;
			}
			
			int cmdChoice = 0;
			int paramOne = 0;
			int paramTwo = 0;
			try
			{
				cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
				
				int endIndex = command.indexOf(' ', 11);
				if (endIndex == -1)
				{
					endIndex = command.length();
				}
				
				if (command.length() > 11)
				{
					paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
					if (command.length() > endIndex)
					{
						paramTwo = Integer.parseInt(command.substring(endIndex).trim());
					}
				}
			}
			catch (final Exception NumberFormatException)
			{
				_log.warn(VillageMasterInstance.class.getName() + ": Wrong numeric values for command " + command);
			}
			
			Set<PlayerClass> subsAvailable = null;
			switch (cmdChoice)
			{
				case 0 :
					html.setFile(player, player.getLang(), getSubClassMenu(player.getRace()));
					break;
				case 1 :
					if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
					{
						html.setFile(player, player.getLang(), getSubClassFail());
						break;
					}
					
					subsAvailable = getAvailableSubClasses(player);
					if ((subsAvailable != null) && !subsAvailable.isEmpty())
					{
						html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_Add.htm");
						final StringBuilder content1 = StringUtil.startAppend(200);
						for (final PlayerClass subClass : subsAvailable)
						{
							StringUtil.append(content1, "<a action=\"bypass -h npc_%objectId%_Subclass 4 ", String.valueOf(subClass.ordinal()), "\" msg=\"1268;", ClassListParser.getInstance().getClass(subClass.ordinal()).getClassName(), "\">", ClassListParser.getInstance().getClass(subClass.ordinal()).getClientCode(), "</a><br>");
						}
						html.replace("%list%", content1.toString());
					}
					else
					{
						if ((player.getRace() == Race.Elf) || (player.getRace() == Race.DarkElf))
						{
							html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_Fail_Elves.htm");
							player.sendPacket(html);
						}
						else if (player.getRace() == Race.Kamael)
						{
							html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_Fail_Kamael.htm");
							player.sendPacket(html);
						}
						else
						{
							player.sendMessage((new ServerMessage("CommunityGeneral.NO_SUB", player.getLang())).toString());
						}
						return;
					}
					break;
				case 2 :
					if (player.getSubClasses().isEmpty())
					{
						html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ChangeNo.htm");
					}
					else
					{
						final StringBuilder content2 = StringUtil.startAppend(200);
						if (checkVillageMaster(player.getBaseClass()))
						{
							StringUtil.append(content2, "<a action=\"bypass -h npc_%objectId%_Subclass 5 0\">", ClassListParser.getInstance().getClass(player.getBaseClass()).getClientCode(), "</a><br>");
						}
						
						for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
							final SubClass subClass = subList.next();
							if (checkVillageMaster(subClass.getClassDefinition()))
							{
								StringUtil.append(content2, "<a action=\"bypass -h npc_%objectId%_Subclass 5 ", String.valueOf(subClass.getClassIndex()), "\">", ClassListParser.getInstance().getClass(subClass.getClassId()).getClientCode(), "</a><br>");
							}
						}
						
						if (content2.length() > 0)
						{
							html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_Change.htm");
							html.replace("%list%", content2.toString());
						}
						else
						{
							html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ChangeNotFound.htm");
						}
					}
					break;
				case 3 :
					if ((player.getSubClasses() == null) || player.getSubClasses().isEmpty())
					{
						html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ModifyEmpty.htm");
						break;
					}
					
					if (player.getTotalSubClasses() > 3)
					{
						html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ModifyCustom.htm");
						final StringBuilder content3 = StringUtil.startAppend(200);
						int classIndex = 1;
						
						for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
							final SubClass subClass = subList.next();
							
							StringUtil.append(content3, "Sub-class ", String.valueOf(classIndex++), "<br>", "<a action=\"bypass -h npc_%objectId%_Subclass 6 ", String.valueOf(subClass.getClassIndex()), "\">", ClassListParser.getInstance().getClass(subClass.getClassId()).getClientCode(), "</a><br>");
						}
						html.replace("%list%", content3.toString());
					}
					else
					{
						html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_Modify.htm");
						if (player.getSubClasses().containsKey(1))
						{
							html.replace("%sub1%", ClassListParser.getInstance().getClass(player.getSubClasses().get(1).getClassId()).getClientCode());
						}
						else
						{
							html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 1\">%sub1%</a><br>", "");
						}
						
						if (player.getSubClasses().containsKey(2))
						{
							html.replace("%sub2%", ClassListParser.getInstance().getClass(player.getSubClasses().get(2).getClassId()).getClientCode());
						}
						else
						{
							html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 2\">%sub2%</a><br>", "");
						}
						
						if (player.getSubClasses().containsKey(3))
						{
							html.replace("%sub3%", ClassListParser.getInstance().getClass(player.getSubClasses().get(3).getClassId()).getClientCode());
						}
						else
						{
							html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 3\">%sub3%</a><br>", "");
						}
					}
					break;
				case 4 :
					if (!player.checkFloodProtection("SUBCLASS", "subclass_change"))
					{
						_log.warn(VillageMasterInstance.class.getName() + ": Player " + player.getName(null) + " has performed a subclass change too fast");
						return;
					}
					
					final boolean added = addNewSubclass(player, paramOne);
					if (added)
					{
						html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_AddOk.htm");
					}
					else
					{
						html.setFile(player, player.getLang(), getSubClassFail());
					}
					break;
				case 5 :
					if (!player.checkFloodProtection("SUBCLASS", "subclass_change"))
					{
						_log.warn(VillageMasterInstance.class.getName() + ": Player " + player.getName(null) + " has performed a subclass change too fast");
						return;
					}
					
					if (Config.SUBCLASS_STORE_SKILL)
					{
						if (player.getBuffCount() < 1)
						{
							player.sendMessage("Take one Buff to your self first.");
							return;
						}
					}
					
					if (player.isInCombat())
					{
						player.sendMessage((new ServerMessage("CommunityGeneral.SUB_IN_COMBAT", player.getLang())).toString());
						return;
					}
					
					if (player.getClassIndex() == paramOne)
					{
						html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_Current.htm");
						break;
					}
					
					if (paramOne == 0)
					{
						if (!checkVillageMaster(player.getBaseClass()))
						{
							return;
						}
					}
					else
					{
						try
						{
							if (!checkVillageMaster(player.getSubClasses().get(paramOne).getClassDefinition()))
							{
								return;
							}
						}
						catch (final NullPointerException e)
						{
							return;
						}
					}
					
					player.setActiveClass(paramOne);
					player.sendPacket(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED);
					return;
				case 6 :
					if ((paramOne < 1) || (paramOne > Config.MAX_SUBCLASS))
					{
						return;
					}
					
					if (player.isInCombat())
					{
						player.sendMessage((new ServerMessage("CommunityGeneral.SUB_IN_COMBAT", player.getLang())).toString());
						return;
					}
					
					subsAvailable = getAvailableSubClasses(player);
					
					if ((subsAvailable == null) || subsAvailable.isEmpty())
					{
						player.sendMessage((new ServerMessage("CommunityGeneral.NO_SUB", player.getLang())).toString());
						return;
					}
					
					final StringBuilder content6 = StringUtil.startAppend(200);
					for (final PlayerClass subClass : subsAvailable)
					{
						StringUtil.append(content6, "<a action=\"bypass -h npc_%objectId%_Subclass 7 ", String.valueOf(paramOne), " ", String.valueOf(subClass.ordinal()), "\" msg=\"1445;", "\">", ClassListParser.getInstance().getClass(subClass.ordinal()).getClientCode(), "</a><br>");
					}
					
					switch (paramOne)
					{
						case 1 :
							html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ModifyChoice1.htm");
							break;
						case 2 :
							html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ModifyChoice2.htm");
							break;
						case 3 :
							html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ModifyChoice3.htm");
							break;
						default :
							html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ModifyChoice.htm");
					}
					html.replace("%list%", content6.toString());
					break;
				case 7 :
					if (!player.checkFloodProtection("SUBCLASS", "subclass_change"))
					{
						_log.warn(VillageMasterInstance.class.getName() + ": Player " + player.getName(null) + " has performed a subclass change too fast");
						return;
					}
					
					if (player.isInCombat())
					{
						player.sendMessage((new ServerMessage("CommunityGeneral.SUB_IN_COMBAT", player.getLang())).toString());
						return;
					}
					
					if (!isValidNewSubClass(player, paramTwo))
					{
						return;
					}
					
					if (player.modifySubClass(paramOne, paramTwo))
					{
						player.abortCast();
						player.stopAllEffectsExceptThoseThatLastThroughDeath();
						player.stopAllEffectsNotStayOnSubclassChange();
						player.stopCubics(true);
						player.setActiveClass(paramOne);
						InitialShortcutParser.getInstance().registerAllShortcuts(player);
						player.sendPacket(new ShortCutInit(player));
						html.setFile(player, player.getLang(), "data/html/villagemaster/SubClass_ModifyOk.htm");
						html.replace("%name%", ClassListParser.getInstance().getClass(paramTwo).getClientCode());
						
						player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS);
					}
					else
					{
						player.setActiveClass(0);
						player.sendMessage((new ServerMessage("CommunityGeneral.CANT_ADD_SUB", player.getLang())).toString());
						return;
					}
					break;
			}
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	public boolean addNewSubclass(Player player, int classId)
	{
		boolean allowAddition = true;
		
		if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
		{
			allowAddition = false;
		}
		
		if (player.getLevel() < 75)
		{
			allowAddition = false;
		}
		
		if (allowAddition)
		{
			if (!player.getSubClasses().isEmpty())
			{
				for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					final SubClass subClass = subList.next();
					
					if (subClass.getLevel() < 75)
					{
						allowAddition = false;
						break;
					}
				}
			}
		}
		
		if (allowAddition && !Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
		{
			allowAddition = checkQuests(player);
		}
		
		if (allowAddition && isValidNewSubClass(player, classId))
		{
			if (!player.addSubClass(classId, player.getTotalSubClasses() + 1))
			{
				return false;
			}
			player.setActiveClass(player.getTotalSubClasses());
			InitialShortcutParser.getInstance().registerAllShortcuts(player);
			player.sendPacket(new ShortCutInit(player));
			player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS);
			return true;
		}
		return false;
	}
	
	protected String getSubClassMenu(Race pRace)
	{
		if ((Config.ALT_GAME_SUBCLASS_EVERYWHERE || Config.ALT_GAME_SUBCLASS_ALL_CLASSES) || (pRace != Race.Kamael))
		{
			return "data/html/villagemaster/SubClass.htm";
		}
		return "data/html/villagemaster/SubClass_NoOther.htm";
	}
	
	protected String getSubClassFail()
	{
		return "data/html/villagemaster/SubClass_Fail.htm";
	}
	
	protected boolean checkQuests(Player player)
	{
		if (player.isNoble() || player.getVarInt("subclassBuy", 0) > 0)
		{
			return true;
		}
		
		QuestState qs = player.getQuestState("_234_FatesWhisper");
		if ((qs == null) || !qs.isCompleted())
		{
			return false;
		}
		
		qs = player.getQuestState("_235_MimirsElixir");
		if ((qs == null) || !qs.isCompleted())
		{
			return false;
		}
		
		return true;
	}
	
	public final Set<PlayerClass> getAvailableSubClasses(Player player)
	{
		final int currentBaseId = player.getBaseClass();
		final ClassId baseCID = ClassId.getClassId(currentBaseId);
		final int baseClassId;
		
		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().ordinal();
		}
		else
		{
			baseClassId = currentBaseId;
		}
		
		final Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		
		if ((availSubs != null) && !availSubs.isEmpty())
		{
			for (final Iterator<PlayerClass> availSub = availSubs.iterator(); availSub.hasNext();)
			{
				final PlayerClass pclass = availSub.next();
				
				if (!checkVillageMaster(pclass))
				{
					availSub.remove();
					continue;
				}
				final int availClassId = pclass.ordinal();
				final ClassId cid = ClassId.getClassId(availClassId);
				SubClass prevSubClass;
				ClassId subClassId;
				for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					prevSubClass = subList.next();
					subClassId = ClassId.getClassId(prevSubClass.getClassId());
					
					if (subClassId.equalsOrChildOf(cid))
					{
						availSub.remove();
						break;
					}
				}
			}
		}
		return availSubs;
	}
	
	private final boolean isValidNewSubClass(Player player, int classId)
	{
		if (!checkVillageMaster(classId))
		{
			return false;
		}
		
		final ClassId cid = ClassId.values()[classId];
		SubClass sub;
		ClassId subClassId;
		for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
		{
			sub = subList.next();
			subClassId = ClassId.values()[sub.getClassId()];
			
			if (subClassId.equalsOrChildOf(cid))
			{
				return false;
			}
		}
		final int currentBaseId = player.getBaseClass();
		final ClassId baseCID = ClassId.getClassId(currentBaseId);
		final int baseClassId;
		
		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().ordinal();
		}
		else
		{
			baseClassId = currentBaseId;
		}
		
		final Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		if ((availSubs == null) || availSubs.isEmpty())
		{
			return false;
		}
		
		boolean found = false;
		for (final PlayerClass pclass : availSubs)
		{
			if (pclass.ordinal() == classId)
			{
				found = true;
				break;
			}
		}
		return found;
	}
	
	public final Race getVillageMasterRace()
	{
		final String npcClass = getTemplate().getClientClass().toLowerCase();
		
		if (npcClass.contains("human"))
		{
			return Race.Human;
		}
		
		if (npcClass.contains("darkelf"))
		{
			return Race.DarkElf;
		}
		
		if (npcClass.contains("elf"))
		{
			return Race.Elf;
		}
		
		if (npcClass.contains("orc"))
		{
			return Race.Orc;
		}
		
		if (npcClass.contains("dwarf"))
		{
			return Race.Dwarf;
		}
		
		return Race.Kamael;
	}
	
	public final ClassType getVillageMasterTeachType()
	{
		final String npcClass = getTemplate().getClientClass().toLowerCase();
		
		if (npcClass.contains("sanctuary") || npcClass.contains("clergyman"))
		{
			return ClassType.Priest;
		}
		
		if (npcClass.contains("mageguild") || npcClass.contains("patriarch"))
		{
			return ClassType.Mystic;
		}
		
		return ClassType.Fighter;
	}
	
	protected boolean checkVillageMasterRace(PlayerClass pclass)
	{
		return true;
	}
	
	protected boolean checkVillageMasterTeachType(PlayerClass pclass)
	{
		return true;
	}
	
	public final boolean checkVillageMaster(int classId)
	{
		return checkVillageMaster(PlayerClass.values()[classId]);
	}
	
	public final boolean checkVillageMaster(PlayerClass pclass)
	{
		if (Config.ALT_GAME_SUBCLASS_EVERYWHERE)
		{
			return true;
		}
		
		return checkVillageMasterRace(pclass) && checkVillageMasterTeachType(pclass);
	}
	
	private static final Iterator<SubClass> iterSubClasses(Player player)
	{
		return player.getSubClasses().values().iterator();
	}
	
	private static final void dissolveClan(Player player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY);
			return;
		}
		if (clan.isAtWar())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR);
			return;
		}
		if ((clan.getCastleId() != 0) || (clan.getHideoutId() != 0) || (clan.getFortId() != 0))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE);
			return;
		}
		
		for (final Castle castle : CastleManager.getInstance().getCastles())
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getId()))
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
				return;
			}
		}
		for (final Fort fort : FortManager.getInstance().getForts())
		{
			if (FortSiegeManager.getInstance().checkIsRegistered(clan, fort.getId()))
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
				return;
			}
		}
		
		if (player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
			return;
		}
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.DISSOLUTION_IN_PROGRESS);
			return;
		}
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + (Config.ALT_CLAN_DISSOLVE_DAYS * 3600000L));
		clan.updateClanInDB();
		ClanHolder.getInstance().scheduleRemoveClan(clan.getId());
		player.deathPenalty(null, false, false, false);
	}
	
	private static final void recoverClan(Player player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		final Clan clan = player.getClan();
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
	}
	
	private static final void createSubPledge(Player player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == Clan.SUBUNIT_ACADEMY)
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY);
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT);
			}
			
			return;
		}
		if (!Util.isAlphaNumeric(clanName) || !isValidName(clanName) || (2 > clanName.length()))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if (clanName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_TOO_LONG);
			return;
		}
		
		for (final Clan tempClan : ClanHolder.getInstance().getClans())
		{
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == Clan.SUBUNIT_ACADEMY)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
					sm.addString(clanName);
					player.sendPacket(sm);
					sm = null;
				}
				else
				{
					player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME);
				}
				
				return;
			}
		}
		
		if (pledgeType != Clan.SUBUNIT_ACADEMY)
		{
			if ((clan.getClanMember(leaderName) == null) || (clan.getClanMember(leaderName).getPledgeType() != 0))
			{
				if (pledgeType >= Clan.SUBUNIT_KNIGHT1)
				{
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
				}
				else if (pledgeType >= Clan.SUBUNIT_ROYAL1)
				{
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
				}
				
				return;
			}
		}
		
		final int leaderId = pledgeType != Clan.SUBUNIT_ACADEMY ? clan.getClanMember(leaderName).getObjectId() : 0;
		
		if (clan.createSubPledge(player, pledgeType, leaderId, clanName) == null)
		{
			return;
		}
		
		SystemMessage sm;
		if (pledgeType == Clan.SUBUNIT_ACADEMY)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= Clan.SUBUNIT_KNIGHT1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= Clan.SUBUNIT_ROYAL1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_CREATED);
		}
		player.sendPacket(sm);
		
		if (pledgeType != Clan.SUBUNIT_ACADEMY)
		{
			final ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			final Player leaderPlayer = leaderSubPledge.getPlayerInstance();
			if (leaderPlayer != null)
			{
				leaderPlayer.setPledgeClass(ClanMember.calculatePledgeClass(leaderPlayer));
				leaderPlayer.sendUserInfo();
			}
		}
	}
	
	private static final void renameSubPledge(Player player, int pledgeType, String pledgeName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(pledgeType);
		
		if (subPledge == null)
		{
			return;
		}
		if (!Util.isAlphaNumeric(pledgeName) || !isValidName(pledgeName) || (2 > pledgeName.length()))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if (pledgeName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_TOO_LONG);
			return;
		}
		
		subPledge.setName(pledgeName);
		clan.updateSubPledgeInDB(subPledge.getId());
		clan.broadcastClanStatus();
		player.sendMessage("Pledge name changed.");
	}
	
	private static final void assignSubPledgeLeader(Player player, String clanName, String leaderName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		if (leaderName.length() > 16)
		{
			player.sendPacket(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS);
			return;
		}
		if (player.getName(null).equals(leaderName))
		{
			player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}
		
		final Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(clanName);
		
		if ((null == subPledge) || (subPledge.getId() == Clan.SUBUNIT_ACADEMY))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if ((clan.getClanMember(leaderName) == null) || (clan.getClanMember(leaderName).getPledgeType() != 0))
		{
			if (subPledge.getId() >= Clan.SUBUNIT_KNIGHT1)
			{
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			}
			else if (subPledge.getId() >= Clan.SUBUNIT_ROYAL1)
			{
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			}
			
			return;
		}
		subPledge.setLeaderId(clan.getClanMember(leaderName).getObjectId());
		clan.updateSubPledgeInDB(subPledge.getId());
		
		final ClanMember leaderSubPledge = clan.getClanMember(leaderName);
		final Player leaderPlayer = leaderSubPledge.getPlayerInstance();
		if (leaderPlayer != null)
		{
			leaderPlayer.setPledgeClass(ClanMember.calculatePledgeClass(leaderPlayer));
			leaderPlayer.sendUserInfo();
		}
		
		clan.broadcastClanStatus();
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_BEEN_SELECTED_AS_CAPTAIN_OF_S2);
		sm.addString(leaderName);
		sm.addString(clanName);
		clan.broadcastToOnlineMembers(sm);
		sm = null;
	}
	
	public static final void showPledgeSkillList(Player player)
	{
		if (!player.isClanLeader())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(player, player.getLang(), "data/html/villagemaster/NotClanLeader.htm");
			player.sendPacket(html);
			player.sendActionFailed();
			return;
		}
		
		final List<SkillLearn> skills = SkillTreesParser.getInstance().getAvailablePledgeSkills(player.getClan());
		final ExAcquirableSkillListByClass asl = new ExAcquirableSkillListByClass(AcquireSkillType.PLEDGE);
		int counts = 0;
		
		for (final SkillLearn s : skills)
		{
			asl.addSkill(s.getId(), s.getGetLevel(), s.getLvl(), s.getLvl(), s.getLevelUpSp(), s.getSocialClass().ordinal());
			counts++;
		}
		
		if (counts == 0)
		{
			if (player.getClan().getLevel() < 8)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
				if (player.getClan().getLevel() < 5)
				{
					sm.addNumber(5);
				}
				else
				{
					sm.addNumber(player.getClan().getLevel() + 1);
				}
				player.sendPacket(sm);
			}
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(player, player.getLang(), "data/html/villagemaster/NoMoreSkills.htm");
				player.sendPacket(html);
			}
			player.sendPacket(AcquireSkillDone.STATIC);
		}
		else
		{
			player.sendPacket(asl);
		}
		player.sendActionFailed();
	}
	
	private static boolean isValidName(String name)
	{
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.CLAN_NAME_TEMPLATE);
		}
		catch (final PatternSyntaxException e)
		{
			_log.warn("ERROR: Wrong pattern for clan name!");
			pattern = Pattern.compile(".*");
		}
		return pattern.matcher(name).matches();
	}
}