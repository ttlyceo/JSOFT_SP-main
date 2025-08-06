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
package l2e.gameserver.model.olympiad;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party.messageType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.zone.type.OlympiadStadiumZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExOlympiadMode;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public abstract class AbstractOlympiadGame
{
	protected static final Logger _log = LoggerFactory.getLogger(AbstractOlympiadGame.class);
	
	protected static final String POINTS = "olympiad_points";
	protected static final String COMP_DONE = "competitions_done";
	protected static final String COMP_WON = "competitions_won";
	protected static final String COMP_LOST = "competitions_lost";
	protected static final String COMP_DRAWN = "competitions_drawn";
	protected static final String COMP_DONE_WEEK = "competitions_done_week";
	protected static final String COMP_DONE_WEEK_CLASSED = "competitions_done_week_classed";
	protected static final String COMP_DONE_WEEK_NON_CLASSED = "competitions_done_week_non_classed";
	protected static final String COMP_DONE_WEEK_TEAM = "competitions_done_week_team";
	
	protected long _startTime = 0;
	protected boolean _aborted = false;
	protected final int _stadiumID;
	protected OlympiadStadiumZone _zone;
	public boolean _hasEnded = false;

	protected AbstractOlympiadGame(int id)
	{
		_stadiumID = id;
	}

	public final boolean isAborted()
	{
		return _aborted;
	}

	public final int getStadiumId()
	{
		return _stadiumID;
	}

	protected boolean makeCompetitionStart()
	{
		_startTime = System.currentTimeMillis();
		return !_aborted;
	}

	protected final void addPointsToParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, points);
		final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_GAINED_S2_OLYMPIAD_POINTS);
		sm.addString(par.getName());
		sm.addNumber(points);
		broadcastPacket(sm);
		final Player player = par.getPlayer();
		if (player != null)
		{
			player.getListeners().onOlympiadFinishBattle(getType(), true);
			
			for (final var quest : QuestManager.getInstance().getQuests())
			{
				if ((quest != null) && quest.isOlympiadUse())
				{
					quest.notifyOlympiadWin(player, getType());
				}
			}
		}
	}

	protected final void removePointsFromParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, -points);
		final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_LOST_S2_OLYMPIAD_POINTS);
		sm.addString(par.getName());
		sm.addNumber(points);
		broadcastPacket(sm);
		final Player player = par.getPlayer();
		if (player != null)
		{
			player.getListeners().onOlympiadFinishBattle(getType(), false);
			
			for (final var quest : QuestManager.getInstance().getQuests())
			{
				if ((quest != null) && quest.isOlympiadUse())
				{
					quest.notifyOlympiadLose(player, getType());
				}
			}
		}
	}

	protected static SystemMessage checkDefaulted(Player player)
	{
		if ((player == null) || !player.isOnline())
		{
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_ENDS_THE_GAME);
		}

		if ((player.getClient() == null) || player.getClient().isDetached())
		{
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_ENDS_THE_GAME);
		}

		if (player.isInFightEvent() || player.inObserverMode() || player.checkInTournament())
		{
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}

		SystemMessage sm;
		if (player.isDead())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_OLYMPIAD_WHILE_DEAD);
			sm.addPcName(player);
			player.sendPacket(sm);
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		if (player.isSubClassActive())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_OLYMPIAD_WHILE_CHANGED_TO_SUB_CLASS);
			sm.addPcName(player);
			player.sendPacket(sm);
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		if (player.isCursedWeaponEquipped())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_JOIN_OLYMPIAD_POSSESSING_S2);
			sm.addPcName(player);
			sm.addItemName(player.getCursedWeaponEquippedId());
			player.sendPacket(sm);
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		if (!player.isInventoryUnder90(true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_OLYMPIAD_INVENTORY_SLOT_EXCEEDS_80_PERCENT);
			sm.addPcName(player);
			player.sendPacket(sm);
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		return null;
	}

	protected static final boolean portPlayerToArena(Participant par, Location loc, int id)
	{
		final var player = par.getPlayer();
		if ((player == null) || !player.isOnline())
		{
			return false;
		}

		try
		{
			player.setLastCords(player.getX(), player.getY(), player.getZ());
			if (player.isSitting())
			{
				player.standUp();
			}
			player.setTarget(null);

			player.setOlympiadGameId(id);
			player.setIsInOlympiadMode(true);
			player.setIsOlympiadStart(false);
			player.setOlympiadSide(par.getSide());
			player.olyBuff = 5;
			player.teleToLocation(loc, true, OlympiadGameManager.getInstance().getOlympiadTask(id).getZone().getReflection());
			player.sendPacket(new ExOlympiadMode(2));
			player.updateAndBroadcastStatus(1);
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	protected void setOlympiadGame(Participant par, AbstractOlympiadGame game)
	{
		if (par.getPlayer() != null)
		{
			par.getPlayer().setOlympiadGame(game);
		}
	}

	protected static final void removals(Player player, boolean removeParty)
	{
		try
		{
			if (player == null)
			{
				return;
			}
			
			if (removeParty)
			{
				final var party = player.getParty();
				if (party != null)
				{
					party.removePartyMember(player, messageType.Expelled);
				}
			}

			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			final var clan = player.getClan();
			if (clan != null)
			{
				clan.removeSkillEffects(player);
				if (clan.getCastleId() > 0)
				{
					CastleManager.getInstance().getCastleByOwner(clan).removeResidentialSkills(player);
				}
				if (clan.getFortId() > 0)
				{
					FortManager.getInstance().getFortByOwner(clan).removeResidentialSkills(player);
				}
				if (clan.getHideoutId() > 0)
				{
					final var hall = ClanHallManager.getInstance().getAbstractHallByOwner(clan);
					if (hall != null)
					{
						hall.removeResidentialSkills(player);
					}
				}
			}
			player.abortAttack();
			player.abortCast();

			player.setInvisible(false);

			if (player.isHero())
			{
				for (final var skill : SkillTreesParser.getInstance().getHeroSkillTree().values())
				{
					player.removeSkill(skill, false);
				}
			}
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());

			if (player.hasSummon())
			{
				final var summon = player.getSummon();
				summon.stopAllEffectsExceptThoseThatLastThroughDeath();
				summon.abortAttack();
				summon.abortCast();
				if (!summon.isDead())
				{
					summon.setCurrentHp(summon.getMaxHp());
					summon.setCurrentMp(summon.getMaxMp());
				}

				if (summon.isPet() || Config.ALLOW_UNSUMMON_ALL)
				{
					summon.unSummon(player);
				}
			}
			player.stopCubicsByOthers();

			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
				player.broadcastUserInfo(true);
			}

			player.checkItemRestriction();
			player.disableAutoShotsAll();
			player.resetReuse();

			final var item = player.getActiveWeaponInstance();
			if (item != null)
			{
				item.unChargeAllShots();
			}

			final var classSkills = SkillTreesParser.getInstance().getAllOriginalClassSkillIdTree(player.getClassId());
			final var originalSkill = SkillTreesParser.getInstance().getAllOriginalClassSkillTree(player.getClassId());
			for (final var skill : player.getAllSkills())
			{
				if (classSkills != null && !classSkills.isEmpty() && Config.CHECK_CLASS_SKILLS)
				{
					if (!skill.isItemSkill() && !classSkills.contains(skill.getId()))
					{
						player.addBlockSkill(skill, true);
					}
				}
			}
			
			if (Config.CHECK_CLASS_SKILLS && originalSkill != null)
			{
				for (final var skill : originalSkill.values())
				{
					if (skill != null)
					{
						final var newSk = SkillsParser.getInstance().getInfo(skill.getId(), skill.getLvl());
						if (newSk != null)
						{
							final var sk = player.getKnownSkill(skill.getId());
							if (sk != null)
							{
								if (sk.getLevel() < skill.getLvl())
								{
									player._olyRestoreSkills.add(sk);
									player.addSkill(newSk, false);
								}
							}
							else
							{
								player.addSkill(newSk, false);
								player._olyDeleteSkills.add(newSk);
							}
						}
					}
				}
			}
			player.sendSkillList(true);
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
	}
	
	protected static final void restorePlayer(Player player)
	{
		if (player.isDead())
		{
			player.setIsDead(false);
		}
		
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		player.getStatus().startHpMpRegeneration();
	}

	protected static final void cleanEffects(Player player)
	{
		try
		{
			player.setIsOlympiadStart(false);
			player.setTarget(null);
			player.abortAttack();
			player.abortCast();
			player.getAI().setIntention(CtrlIntention.IDLE);
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			player.clearSouls();
			player.clearCharges();
			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
			}
			final var summon = player.getSummon();
			if ((summon != null) && !summon.isDead())
			{
				summon.setTarget(null);
				summon.abortAttack();
				summon.abortCast();
				summon.stopAllEffectsExceptThoseThatLastThroughDeath();
			}

			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.getStatus().startHpMpRegeneration();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
	}

	protected static final void playerStatusBack(Player player)
	{
		try
		{
			if (player.isTransformed())
			{
				player.untransform();
			}

			player.setIsInOlympiadMode(false);
			player.setIsOlympiadStart(false);
			player.setOlympiadSide(-1);
			player.setOlympiadGameId(-1);
			player.setOlympiadGame(null);
			player.sendPacket(new ExOlympiadMode(0));

			final var clan = player.getClan();
			if (clan != null)
			{
				clan.addSkillEffects(player);
				if (clan.getCastleId() > 0)
				{
					CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(player);
				}
				if (clan.getFortId() > 0)
				{
					FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(player);
				}
				if (clan.getHideoutId() > 0)
				{
					final var hall = ClanHallManager.getInstance().getAbstractHallByOwner(clan);
					if (hall != null)
					{
						hall.giveResidentialSkills(player);
					}
				}
			}

			if (player.isHero() && !player.isTempHero())
			{
				for (final var skill : SkillTreesParser.getInstance().getHeroSkillTree().values())
				{
					player.addSkill(skill, false);
				}
			}
			if (Config.CHECK_CLASS_SKILLS)
			{
				player.cleanBlockSkills(true);
				if (!player._olyRestoreSkills.isEmpty())
				{
					for (final var sk : player._olyRestoreSkills)
					{
						if (sk != null)
						{
							player.addSkill(sk, true);
						}
					}
					player._olyRestoreSkills.clear();
				}
				
				if (!player._olyDeleteSkills.isEmpty())
				{
					for (final var sk : player._olyDeleteSkills)
					{
						if (sk != null)
						{
							player.removeSkill(sk, false);
						}
					}
					player._olyDeleteSkills.clear();
				}
			}
			player.sendSkillList(true);
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.getStatus().startHpMpRegeneration();
			if (player.hasSummon())
			{
				final var summon = player.getSummon();
				if (!summon.isDead())
				{
					summon.setCurrentHp(summon.getMaxHp());
					summon.setCurrentMp(summon.getMaxMp());
				}
			}

			if (Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0)
			{
				DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.OLYMPIAD_ID, player);
			}
		}
		catch (final Exception e)
		{
			_log.warn("portPlayersToArena()", e);
		}
	}

	protected static final void portPlayerBack(Player player)
	{
		if (player == null)
		{
			return;
		}

		if ((player.getLastX() == 0) && (player.getLastY() == 0))
		{
			return;
		}
		player.teleToLocation(player.getLastX(), player.getLastY(), player.getLastZ(), true, ReflectionManager.DEFAULT);
		player.setLastCords(0, 0, 0);
	}

	public static final void rewardParticipant(Player player, int[][] reward)
	{
		if ((player == null) || !player.isOnline() || (reward == null))
		{
			return;
		}
		
		if (Config.ALLOW_DAILY_TASKS)
		{
			if (player.getActiveDailyTasks() != null)
			{
				for (final var taskTemplate : player.getActiveDailyTasks())
				{
					if (taskTemplate.getType().equalsIgnoreCase("Olympiad") && !taskTemplate.isComplete())
					{
						final var task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
						if ((taskTemplate.getCurrentOlyMatchCount() < task.getOlyMatchCount()))
						{
							taskTemplate.setCurrentOlyMatchCount((taskTemplate.getCurrentOlyMatchCount() + 1));
							if (taskTemplate.isComplete())
							{
								final var vch = VoicedCommandHandler.getInstance().getHandler("missions");
								if (vch != null)
								{
									player.updateDailyStatus(taskTemplate);
									vch.useVoicedCommand("missions", player, null);
								}
							}
						}
					}
				}
			}
		}

		try
		{
			SystemMessage sm;
			ItemInstance item;
			final var iu = new InventoryUpdate();
			for (final int[] it : reward)
			{
				if ((it == null) || (it.length != 2))
				{
					continue;
				}

				item = player.getInventory().addItem("Olympiad", it[0], it[1], player, null);
				if (item == null)
				{
					continue;
				}

				iu.addModifiedItem(item);
				sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(it[0]);
				sm.addNumber(it[1]);
				player.sendPacket(sm);
			}
			player.sendPacket(iu);
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
	}
	
	public void setZone(OlympiadStadiumZone zone)
	{
		_zone = zone;
	}
	
	public void checkWinner()
	{
		if (haveWinner() && !_hasEnded)
		{
			_hasEnded = true;
			_zone.updateZoneStatusForCharactersInside(true);
			cleanEffects();
			validateWinner(_zone);
			restorePlayers();
		}
	}
	
	public abstract CompetitionType getType();

	public abstract String[] getPlayerNames();

	public abstract boolean containsParticipant(int playerId);
	
	public abstract boolean containsHwidParticipant(Player player);

	public abstract void sendOlympiadInfo(Creature player);

	public abstract void broadcastOlympiadInfo(OlympiadStadiumZone stadium);

	protected abstract void broadcastPacket(GameServerPacket packet);

	protected abstract boolean needBuffers();

	protected abstract boolean checkDefaulted();

	protected abstract void removals();

	protected abstract boolean portPlayersToArena(List<Location> spawns);
	
	protected abstract void confirmDlgInvite();

	protected abstract void cleanEffects();
	
	protected abstract void restorePlayers();

	protected abstract void portPlayersBack();

	protected abstract void playersStatusBack();

	protected abstract void clearPlayers();

	protected abstract void healPlayers();

	protected abstract void handleDisconnect(Player player);

	protected abstract void resetDamage();

	protected abstract void addDamage(Player player, int damage);

	protected abstract boolean checkBattleStatus();

	protected abstract boolean haveWinner();

	protected abstract void validateWinner(OlympiadStadiumZone stadium);

	protected abstract int getDivider();

	protected abstract int[][] getReward();
	
	protected abstract int[][] getLoseReward();

	protected abstract String getWeeklyMatchType();
}