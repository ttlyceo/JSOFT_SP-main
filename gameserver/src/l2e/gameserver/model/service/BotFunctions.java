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
package l2e.gameserver.model.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBuffer;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.service.buffer.SingleBuff;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class BotFunctions
{
	protected static final Logger _log = LoggerFactory.getLogger(BotFunctions.class);
	
	protected static boolean ALLOW_AUTO_CB_TELEPORT_BY_ID;
	private static boolean ALLOW_AUTO_CB_TELEPORT_BY_COORDS;
	private static boolean ALLOW_AUTO_CB_TELEPORT_TO_RAID;
	private static boolean ALLOW_AUTO_GOTO_TELEPORT;
	private static boolean ALLOW_AUTO_VITALITY;
	private static boolean ALLOW_AUTO_BUFF_CB_SETS;
	private boolean ALLOW_LICENCE;
	private static long ADENA_MIN_LIMIT;
	
	protected BotFunctions()
	{
		load();
	}
	
	private final void load()
	{
		final Properties botSettings = new Properties();
		final File file = new File(Config.BOT_FILE);
		try (
		    InputStream is = new FileInputStream(file))
		{
			botSettings.load(is);
		}
		catch (final Exception e)
		{
		}
		
		ALLOW_AUTO_CB_TELEPORT_BY_ID = Boolean.parseBoolean(botSettings.getProperty("AllowAutoCBTeleById", "False"));
		ALLOW_AUTO_CB_TELEPORT_BY_COORDS = Boolean.parseBoolean(botSettings.getProperty("AllowAutoCBTeleByCoords", "False"));
		ALLOW_AUTO_CB_TELEPORT_TO_RAID = Boolean.parseBoolean(botSettings.getProperty("AllowAutoCBTeleToRaid", "False"));
		ALLOW_AUTO_GOTO_TELEPORT = Boolean.parseBoolean(botSettings.getProperty("AllowAutoGotoTeleport", "False"));
		ALLOW_AUTO_VITALITY = Boolean.parseBoolean(botSettings.getProperty("AllowAutoVitality", "False"));
		ALLOW_AUTO_BUFF_CB_SETS = Boolean.parseBoolean(botSettings.getProperty("AllowAutoBuffSets", "False"));
		ALLOW_LICENCE = Config.USER_NAME.equalsIgnoreCase("rsidle2020") || Config.USER_NAME.equalsIgnoreCase("LordWinter") || Config.USER_NAME.equalsIgnoreCase("Gravity");
		ADENA_MIN_LIMIT = Long.parseLong(botSettings.getProperty("AdenaMinLimit", "50000"));
	}
	
	public boolean isAutoBuffEnable(Player player)
	{
		if (!ALLOW_AUTO_BUFF_CB_SETS || !ALLOW_LICENCE)
		{
			return false;
		}
		
		if (!player.getVarB("autoBuff@", false) || player.getParty() == null)
		{
			return false;
		}
		
		if (!player.getParty().isLeader(player))
		{
			player.sendMessage("Only party leader can use this function!");
			return false;
		}
		return true;
	}
	
	public void getAutoBuffSet(Player player)
	{
		if (!isAutoBuffEnable(player))
		{
			return;
		}
		
		boolean canUse = true;
		for (final Player member : player.getParty().getMembers())
		{
			if (member != null)
			{
				if (!member.isSameAddress(player))
				{
					continue;
				}
				
				if (!checkCondition(member, true))
				{
					canUse = false;
				}
			}
		}
		
		if (!canUse)
		{
			player.sendMessage("Wrong conditions!!!");
			return;
		}
		
		for (final Player member : player.getParty().getMembers())
		{
			if (member != null)
			{
				if (!member.isSameAddress(player))
				{
					continue;
				}
					
				final int groupId = getPlayerGroupSet(member);
				if (groupId != -1)
				{
					final ArrayList<SingleBuff> buffs = CommunityBuffer.getInstance().getSetBuffs(groupId);
					if (buffs != null && !buffs.isEmpty())
					{
						getBuffsToPlayer(member, buffs);
					}
				}
			}
		}
	}
	
	private static void getBuffsToPlayer(Player player, ArrayList<SingleBuff> buff_sets)
	{
		for (final SingleBuff singleBuff : buff_sets)
		{
			final int skillLvl = player.hasPremiumBonus() ? singleBuff.getPremiumLevel() : singleBuff.getLevel();
			final Skill skill = SkillsParser.getInstance().getInfo(singleBuff.getSkillId(), skillLvl);
			if (skill != null)
			{
				final int buffTime = CommunityBuffer.getInstance().getBuffTime(player, skill.getId(), skill.getLevel());
				if (buffTime > 0 && skill.hasEffects())
				{
					final Env env = new Env();
					env.setCharacter(player);
					env.setTarget(player);
					env.setSkill(skill);
					
					Effect ef;
					for (final EffectTemplate et : skill.getEffectTemplates())
					{
						ef = et.getEffect(env);
						if (ef != null)
						{
							ef.setAbnormalTime(buffTime * 60);
							ef.scheduleEffect(true);
						}
					}
				}
				else
				{
					skill.getEffects(player, player, false);
				}
			}
		}
	}
	
	private static int getPlayerGroupSet(Player player)
	{
		for (final int groupId : CommunityBuffer.getInstance().getBuffClasses().keySet())
		{
			final List<Integer> classes = CommunityBuffer.getInstance().getBuffClasses().get(groupId);
			if (classes != null && !classes.isEmpty())
			{
				if (classes.contains(player.getClassId().getId()))
				{
					return groupId;
				}
			}
		}
		return -1;
	}
	
	public void getAutoVitality(Player player)
	{
		if (!ALLOW_AUTO_VITALITY || !ALLOW_LICENCE)
		{
			return;
		}
		
		if (!player.getVarB("autoVitality@", false) || player.getParty() == null)
		{
			return;
		}
		
		if (!player.getParty().isLeader(player))
		{
			player.sendMessage("Only party leader can use this function!");
			return;
		}
			
		for (final Player member : player.getParty().getMembers())
		{
			if (member != null)
			{
				if (!Util.checkIfInRange(1000, player, member, true) || member.getObjectId() == player.getObjectId())
				{
					continue;
				}
					
				if (!checkCondition(member, false) || !member.isSameAddress(player))
				{
					continue;
				}
				getVitalityToPlayer(member);
			}
		}
	}
	
	private static void getVitalityToPlayer(Player player)
	{
		final Quest quest = QuestManager.getInstance().getQuest("GiftOfVitality");
		if (quest != null)
		{
			QuestState st = player.getQuestState(quest.getName());
			if (st == null)
			{
				st = quest.newQuestState(player);
			}
			
			final long reuse = st.get("reuse") != null ? Long.parseLong(st.get("reuse")) : 0;
			if (reuse > System.currentTimeMillis())
			{
				final long remainingTime = (reuse - System.currentTimeMillis()) / 1000;
				final int hours = (int) (remainingTime / 3600);
				final int minutes = (int) ((remainingTime % 3600) / 60);
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AVAILABLE_AFTER_S1_S2_HOURS_S3_MINUTES);
				sm.addSkillName(23179);
				sm.addNumber(hours);
				sm.addNumber(minutes);
				player.sendPacket(sm);
			}
			else
			{
				player.doCast(new SkillHolder(23179, 1).getSkill());
				player.doCast(new SkillHolder(23180, 1).getSkill());
				st.setState(State.STARTED);
				st.set("reuse", String.valueOf(System.currentTimeMillis() + (5 * 3600000)));
			}
		}
	}
	
	public boolean isAutoTpToRaidEnable(Player player)
	{
		if (!ALLOW_AUTO_CB_TELEPORT_TO_RAID || !ALLOW_LICENCE)
		{
			return false;
		}
		
		if (!player.getVarB("autoTeleport@", false) || player.getParty() == null)
		{
			return false;
		}
		return true;
	}
	
	public void getAutoTeleportToRaid(Player player, Location loc1, Location loc2)
	{
		getAutoTeleportById(player, loc1, loc2, 0);
	}
	
	public boolean isAutoTpGotoEnable(Player player)
	{
		if (!ALLOW_AUTO_GOTO_TELEPORT || !ALLOW_LICENCE)
		{
			return false;
		}
		
		if (!player.getVarB("autoTeleport@", false) || player.getParty() == null)
		{
			return false;
		}
		return true;
	}
	
	public void getAutoGotoTeleport(Player player, Location loc1, Location loc2)
	{
		getAutoTeleportById(player, loc1, loc2, 1000);
	}
	
	public boolean isAutoTpByCoordsEnable(Player player)
	{
		if (!ALLOW_AUTO_CB_TELEPORT_BY_COORDS || !ALLOW_LICENCE)
		{
			return false;
		}
		
		if (!player.getVarB("autoTeleport@", false) || player.getParty() == null)
		{
			return false;
		}
		return true;
	}
	
	public void getAutoTeleportByCoords(Player player, Location loc1, Location loc2)
	{
		getAutoTeleportById(player, loc1, loc2, 0);
	}
	
	public boolean isAutoTpByIdEnable(Player player)
	{
		if (!ALLOW_AUTO_CB_TELEPORT_BY_ID || !ALLOW_LICENCE)
		{
			return false;
		}
		
		if (!player.getVarB("autoTeleport@", false) || player.getParty() == null)
		{
			return false;
		}
		return true;
	}
	
	public void getAutoTeleportById(Player player, Location loc1, Location loc2, int distance)
	{
		if (!player.getParty().isLeader(player))
		{
			player.sendMessage("Only party leader can use this function!");
			return;
		}
		
		boolean canUse = true;
		for (final Player member : player.getParty().getMembers())
		{
			if (member != null)
			{
				if (!member.isSameAddress(player))
				{
					continue;
				}
				
				if (distance > 0)
				{
					if (!Util.checkIfInRange(distance, loc1.getX(), loc1.getY(), loc1.getZ(), member, true))
					{
						canUse = false;
					}
				}
				
				if (!checkCondition(member, false))
				{
					canUse = false;
				}
			}
		}
		
		if (!canUse)
		{
			player.sendMessage("Wrong conditions!!!");
			return;
		}
		
		for (final Player member : player.getParty().getMembers())
		{
			if (member != null)
			{
				if (!member.isSameAddress(player))
				{
					continue;
				}
				final Location l = Location.findAroundPosition(loc2.getX(), loc2.getY(), loc2.getZ(), 40, 60, player.getReflection());
				member.teleToLocation(l.getX(), l.getY(), l.getZ(), true, member.getReflection());
			}
		}
	}
	
	public void getAutoTeleToTown(Player player)
	{
		if (!ALLOW_LICENCE)
		{
			return;
		}
		
		if (player.getParty() == null)
		{
			return;
		}
		
		if (!player.getParty().isLeader(player))
		{
			player.sendMessage("Only party leader can use this function!");
			return;
		}
			
		for (final Player member : player.getParty().getMembers())
		{
			if (member != null)
			{
				if (member.getObjectId() == player.getObjectId())
				{
					continue;
				}
					
				if (!member.isDead() || !member.isSameAddress(player))
				{
					continue;
				}
				getTeleToTown(member);
			}
		}
	}
	
	public void getAutoTransferAdena(Player player)
	{
		if (!ALLOW_LICENCE)
		{
			return;
		}
		
		if (player.getParty() == null)
		{
			return;
		}
		
		if (!player.getParty().isLeader(player))
		{
			player.sendMessage("Only party leader can use this function!");
			return;
		}
			
		for (final Player member : player.getParty().getMembers())
		{
			if (member != null)
			{
				if (member.getObjectId() == player.getObjectId())
				{
					continue;
				}
					
				if (!member.isSameAddress(player))
				{
					continue;
				}
				getTransferAdena(player, member);
			}
		}
	}
	
	private static void getTransferAdena(Player player, Player member)
	{
		final long amount = member.getAdena();
		if (amount <= ADENA_MIN_LIMIT)
		{
			return;
		}
		
		final long transferAmount = member.getAdena() - ADENA_MIN_LIMIT;
		if (transferAmount <= 0)
		{
			return;
		}
		
		member.destroyItemByItemId("TransferAdena", 57, transferAmount, member, true);
		player.getInventory().addItem("TransferAdena", 57, transferAmount, player, true);
	}
	
	private static void getTeleToTown(Player player)
	{
		final Location l = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
		if (l != null)
		{
			player.setIsIn7sDungeon(false);
			player.setIsPendingRevive(true);
			player.teleToLocation(l, true, ReflectionManager.DEFAULT);
		}
	}
	
	public static boolean checkCondition(Player player, boolean isBuff)
	{
		if (player == null)
		{
			return false;
		}
		
		if (player.isInCombat() || player.isCombatFlagEquipped() || player.isBlocked() || player.isCursedWeaponEquipped() || player.isInDuel() || player.isFlying() || player.isJailed() || player.isInOlympiadMode() || player.inObserverMode() || player.isAlikeDead() || player.isInSiege() || player.isDead())
		{
			return false;
		}
		
		if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
		{
			return false;
		}
		
		if (player.isInsideZone(ZoneId.PVP))
		{
			if (player.isInsideZone(ZoneId.FUN_PVP))
			{
				final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
				if (zone != null)
				{
					if ((isBuff && zone.canUseCbBuffs()) || (!isBuff && zone.canUseCbTeleports()))
					{
						return true;
					}
				}
			}
			return false;
		}
		
		if (isBuff && Config.ALLOW_COMMUNITY_PEACE_ZONE)
		{
			if (!player.isInsideZone(ZoneId.PEACE))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean isAutoDropEnable(Player player)
	{
		if (!ALLOW_LICENCE || player.getParty() == null)
		{
			return false;
		}
		
		final Player leader = player.getParty().getLeader();
		if (leader != null)
		{
			if (leader.getVarB("autoDrop@", false) && leader.isSameAddress(player) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, player, leader, true))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean canAutoDropEnable(Player player)
	{
		if (!ALLOW_LICENCE || player.getParty() == null)
		{
			return false;
		}
		
		for (final Player member : player.getParty().getMembers())
		{
			if (member != null && !member.isSameAddress(player))
			{
				return false;
			}
		}
		return true;
	}
	
	public void checkAutoDropFunction(Player player)
	{
		if (!ALLOW_LICENCE || player == null || player.getParty() == null)
		{
			return;
		}
		
		final Player leader = player.getParty().getLeader();
		
		if (leader != null && (leader.getVarB("autoSpoil@", false) || leader.getVarB("autoDrop@", false)) && !canAutoDropEnable(leader))
		{
			leader.setVar("autoSpoil@", "0");
			leader.setVar("autoDrop@", "0");
		}
	}
	
	public boolean isAutoSpoilEnable(Player player)
	{
		if (!ALLOW_LICENCE || player.getParty() == null)
		{
			return false;
		}
		
		final Player leader = player.getParty().getLeader();
		if (leader != null)
		{
			if (leader.getVarB("autoSpoil@", false) && leader.isSameAddress(player) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, player, leader, true))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isAllowLicence()
	{
		return ALLOW_LICENCE;
	}

	public static final BotFunctions getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final BotFunctions _instance = new BotFunctions();
	}
}