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
package l2e.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.listener.Listener;
import l2e.commons.listener.ListenerList;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.MercTicketManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.SiegeGuardManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.listener.other.OnSiegeStatusListener;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.SiegeClan.SiegeClanType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ControlTowerInstance;
import l2e.gameserver.model.actor.instance.FlameTowerInstance;
import l2e.gameserver.model.actor.templates.sieges.SiegeSpawn;
import l2e.gameserver.model.actor.templates.sieges.SiegeTemplate;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CastleSiegeInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.taskmanager.SiegeTaskManager;

public class Siege implements Siegable
{
	protected static final Logger _log = LoggerFactory.getLogger(Siege.class);
	
	public static final byte OWNER = -1;
	public static final byte DEFENDER = 0;
	public static final byte ATTACKER = 1;
	public static final byte DEFENDER_NOT_APPROWED = 2;
	
	private final OnSiegeStatusList _listeners = new OnSiegeStatusList();

	public static enum TeleportWhoType
	{
		All, Attacker, DefenderNotOwner, Owner, Spectator
	}

	private int _controlTowerCount;
	private int _controlTowerMaxCount;
	private int _flameTowerCount;
	private int _flameTowerMaxCount;
	
	private class OnSiegeStatusList extends ListenerList<Siege>
	{
		public void onStart(Siege siege)
		{
			if (!getListeners().isEmpty())
			{
				for (final Listener<Siege> listener : getListeners())
				{
					((OnSiegeStatusListener) listener).onStart(Siege.this);
				}
			}
		}
		
		public void onEnd(Siege siege, Clan winClan, Clan defClan)
		{
			if (!getListeners().isEmpty())
			{
				for (final Listener<Siege> listener : getListeners())
				{
					((OnSiegeStatusListener) listener).onEnd(Siege.this, winClan, defClan);
				}
			}
		}
	}

	private final List<SiegeClan> _attackerClans = new CopyOnWriteArrayList<>();
	private final List<SiegeClan> _defenderClans = new CopyOnWriteArrayList<>();
	private final List<SiegeClan> _defenderWaitingClans = new CopyOnWriteArrayList<>();

	private List<ControlTowerInstance> _controlTowers = new ArrayList<>();
	private List<FlameTowerInstance> _flameTowers = new ArrayList<>();
	private final Castle _castle;
	private boolean _isInProgress = false;
	private boolean _isNormalSide = true;
	protected long _siegeStartTime;
	protected long _siegeCloseChangeDataTime;
	protected long _siegeCloseRegistrationTime;
	protected long _siegeEndTime;
	private SiegeGuardManager _siegeGuardManager;
	protected int _firstOwnerClanId = -1;

	public Siege(Castle castle, long siegeStartTime)
	{
		_castle = castle;
		_siegeGuardManager = new SiegeGuardManager(getCastle());
		final var tpl = _castle.getTemplate();
		if (tpl != null && tpl.isEnableSiege())
		{
			_siegeStartTime = siegeStartTime;
			startAutoTask();
		}
	}

	@Override
	public void endSiege()
	{
		if (getIsInProgress())
		{
			var sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_ENDED);
			sm.addCastleId(getCastle().getId());
			Announcements.getInstance().announceToAll(sm);

			Clan clanWon = null;
			Clan clanDef = null;
			if (getCastle().getOwnerId() > 0)
			{
				final var clan = ClanHolder.getInstance().getClan(getCastle().getOwnerId());
				sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_VICTORIOUS_OVER_S2_S_SIEGE);
				sm.addString(clan.getName());
				sm.addCastleId(getCastle().getId());
				Announcements.getInstance().announceToAll(sm);
				final var isAchActive = AchievementManager.getInstance().isActive();
				if (clan.getId() == _firstOwnerClanId)
				{
					clanDef = clan;
					clan.increaseBloodAllianceCount(getCastle().getTemplate().getBloodAllianceCount());
					getCastle().getTemplate().checkCastleDefenceReward(clan, getCastle().getId());
					for (final var member : clan.getOnlineMembers(0))
					{
						if (member != null && member.isOnline())
						{
							member.getListeners().onParticipateInCastleSiege(this);
							if (isAchActive)
							{
								member.getCounters().addAchivementInfo("castleSiegesDefend", 0, -1, false, false, false);
							}
							
							if (Config.ALLOW_DAILY_TASKS)
							{
								if (member.getActiveDailyTasks() != null)
								{
									for (final var taskTemplate : member.getActiveDailyTasks())
									{
										if (taskTemplate.getType().equalsIgnoreCase("Siege") && !taskTemplate.isComplete())
										{
											final var task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
											if (task.getSiegeCastle() && !task.isAttackSiege())
											{
												taskTemplate.setIsComplete(true);
												member.updateDailyStatus(taskTemplate);
												final var vch = VoicedCommandHandler.getInstance().getHandler("missions");
												if (vch != null)
												{
													vch.useVoicedCommand("missions", member, null);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				else
				{
					clanWon = clan;
					getCastle().getTemplate().checkCastleCaptureReward(clan, getCastle().getId());
					getCastle().setTicketBuyCount(0);
					for (final var member : clan.getMembers())
					{
						if (member != null)
						{
							final Player player = member.getPlayerInstance();
							if (player != null && player.isOnline())
							{
								player.getListeners().onParticipateInCastleSiege(this);
								if (isAchActive)
								{
									player.getCounters().addAchivementInfo("castleSiegesWon", 0, -1, false, false, false);
								}
								if (player.isNoble())
								{
									Hero.getInstance().setCastleTaken(player.getObjectId(), getCastle().getId());
								}
							}
						}
					}
				}
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_S1_DRAW);
				sm.addCastleId(getCastle().getId());
				Announcements.getInstance().announceToAll(sm);
			}

			for (final var attackerClan : getAttackerClans())
			{
				final var clan = ClanHolder.getInstance().getClan(attackerClan.getClanId());
				if (clan == null)
				{
					continue;
				}
				clan.clearSiegeKills();
				clan.clearSiegeDeaths();
			}

			for (final var defenderClan : getDefenderClans())
			{
				final var clan = ClanHolder.getInstance().getClan(defenderClan.getClanId());
				if (clan == null)
				{
					continue;
				}
				clan.clearSiegeKills();
				clan.clearSiegeDeaths();
			}

			_listeners.onEnd(this, clanWon, clanDef);
			getCastle().updateClansReputation();
			removeFlags();
			teleportPlayer(Siege.TeleportWhoType.Attacker, TeleportWhereType.TOWN);
			teleportPlayer(Siege.TeleportWhoType.DefenderNotOwner, TeleportWhereType.TOWN);
			teleportPlayer(Siege.TeleportWhoType.Spectator, TeleportWhereType.TOWN);
			_isInProgress = false;
			updatePlayerSiegeStateFlags(true);
			saveCastleSiege();
			clearSiegeClan();
			removeControlTower();
			removeFlameTower();
			_siegeGuardManager.unspawnSiegeGuard();
			if (getCastle().getOwnerId() > 0)
			{
				_siegeGuardManager.removeMercs();
			}
			getCastle().spawnDoor();
			getCastle().getZone().setIsActive(false);
			getCastle().getZone().updateZoneStatusForCharactersInside();
			getCastle().getZone().setSiegeInstance(null);
		}
	}

	private void removeDefender(SiegeClan sc)
	{
		if (sc != null)
		{
			getDefenderClans().remove(sc);
		}
	}

	private void removeAttacker(SiegeClan sc)
	{
		if (sc != null)
		{
			getAttackerClans().remove(sc);
		}
	}

	private void addDefender(SiegeClan sc, SiegeClanType type)
	{
		if (sc == null)
		{
			return;
		}
		sc.setType(type);
		getDefenderClans().add(sc);
	}

	private void addAttacker(SiegeClan sc)
	{
		if (sc == null)
		{
			return;
		}
		sc.setType(SiegeClanType.ATTACKER);
		getAttackerClans().add(sc);
	}

	public synchronized void midVictory()
	{
		if (getIsInProgress())
		{
			if (getCastle().getOwnerId() > 0)
			{
				_siegeGuardManager.removeMercs();
			}

			if (getDefenderClans().isEmpty() && (getAttackerClans().size() == 1))
			{
				final var sc_newowner = getAttackerClan(getCastle().getOwnerId());
				removeAttacker(sc_newowner);
				addDefender(sc_newowner, SiegeClanType.OWNER);
				endSiege();
				return;
			}

			if (getCastle().getOwnerId() > 0)
			{
				final int allyId = ClanHolder.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
				if (getDefenderClans().isEmpty())
				{
					if (allyId != 0)
					{
						boolean allinsamealliance = true;
						for (final var sc : getAttackerClans())
						{
							if (sc != null)
							{
								if (ClanHolder.getInstance().getClan(sc.getClanId()).getAllyId() != allyId)
								{
									allinsamealliance = false;
								}
							}
						}
						if (allinsamealliance)
						{
							final var sc_newowner = getAttackerClan(getCastle().getOwnerId());
							removeAttacker(sc_newowner);
							addDefender(sc_newowner, SiegeClanType.OWNER);
							endSiege();
							return;
						}
					}
				}

				for (final var sc : getDefenderClans())
				{
					if (sc != null)
					{
						removeDefender(sc);
						addAttacker(sc);
					}
				}

				final var sc_newowner = getAttackerClan(getCastle().getOwnerId());
				removeAttacker(sc_newowner);
				addDefender(sc_newowner, SiegeClanType.OWNER);

				for (final var clan : ClanHolder.getInstance().getClanAllies(allyId))
				{
					final var sc = getAttackerClan(clan.getId());
					if (sc != null)
					{
						removeAttacker(sc);
						addDefender(sc, SiegeClanType.DEFENDER);
					}
				}
				teleportPlayer(TeleportWhoType.Attacker, TeleportWhereType.SIEGEFLAG);
				teleportPlayer(TeleportWhoType.Spectator, TeleportWhereType.TOWN);
				
				removeDefenderFlags();
				getCastle().removeUpgrade();
				getCastle().spawnDoor(true);
				removeControlTower();
				removeFlameTower();
				_controlTowerCount = 0;
				_controlTowerMaxCount = 0;
				_flameTowerCount = 0;
				_flameTowerMaxCount = 0;
				spawnControlTower(getCastle().getId());
				spawnFlameTower(getCastle().getId());
				updatePlayerSiegeStateFlags(false);
			}
		}
	}

	@Override
	public void startSiege()
	{
		if (!getIsInProgress())
		{
			_firstOwnerClanId = getCastle().getOwnerId();

			if (getAttackerClans().isEmpty())
			{
				SystemMessage sm;
				if (_firstOwnerClanId <= 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SIEGE_WAS_CANCELED_BECAUSE_NO_CLANS_PARTICIPATED);
					final var ownerClan = ClanHolder.getInstance().getClan(_firstOwnerClanId);
					ownerClan.increaseBloodAllianceCount(getCastle().getTemplate().getBloodAllianceCount());
				}
				sm.addCastleId(getCastle().getId());
				Announcements.getInstance().announceToAll(sm);
				saveCastleSiege();
				return;
			}

			_isNormalSide = true;
			_isInProgress = true;

			loadSiegeClan(true);
			teleportPlayer(Siege.TeleportWhoType.Attacker, TeleportWhereType.TOWN);
			_controlTowerCount = 0;
			_controlTowerMaxCount = 0;
			spawnControlTower(getCastle().getId());
			spawnFlameTower(getCastle().getId());
			getCastle().spawnDoor();
			spawnSiegeGuard();
			MercTicketManager.getInstance().deleteTickets(getCastle().getId());
			getCastle().getZone().setSiegeInstance(this);
			getCastle().getZone().setIsActive(true);
			getCastle().getZone().updateZoneStatusForCharactersInside();
			updatePlayerSiegeStateFlags(false);

			final int siegeMinutes = getCastle().getTemplate().getSiegeTime();
			_siegeEndTime = _siegeStartTime + (siegeMinutes * 60000L);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_STARTED);
			sm.addCastleId(getCastle().getId());
			Announcements.getInstance().announceToAll(sm);
			SiegeTaskManager.getInstance().addSiegeTask(getCastle().getId(), new SiegeTemplate(this, false), _siegeEndTime);
			if ((siegeMinutes / 60) > 0)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HOURS_UNTIL_SIEGE_CONCLUSION);
				sm.addNumber((siegeMinutes / 60));
				announceToPlayer(sm, true);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_MINUTES_UNTIL_SIEGE_CONCLUSION);
				sm.addNumber(siegeMinutes);
				announceToPlayer(sm, true);
			}
			_listeners.onStart(this);
		}
	}

	public void announceToPlayer(SystemMessage message, boolean bothSides)
	{
		for (final var siegeClans : getDefenderClans())
		{
			final var clan = ClanHolder.getInstance().getClan(siegeClans.getClanId());
			for (final Player member : clan.getOnlineMembers(0))
			{
				if (member != null)
				{
					member.sendPacket(message);
				}
			}
		}

		if (bothSides)
		{
			for (final var siegeClans : getAttackerClans())
			{
				final var clan = ClanHolder.getInstance().getClan(siegeClans.getClanId());
				for (final Player member : clan.getOnlineMembers(0))
				{
					if (member != null)
					{
						member.sendPacket(message);
					}
				}
			}
		}
	}

	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		for (final var siegeclan : getAttackerClans())
		{
			if (siegeclan == null)
			{
				continue;
			}

			final var clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
			for (final var member : clan.getOnlineMembers(0))
			{
				if (member == null)
				{
					continue;
				}

				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setSiegeSide(0);
					member.setIsInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 1);
					member.setSiegeSide(getCastle().getId());
					if (checkIfInZone(member))
					{
						member.setIsInSiege(true);
						member.startFameTask(Config.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, Config.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				member.sendUserInfo();
				member.broadcastRelationChanged();
			}
		}
		for (final var siegeclan : getDefenderClans())
		{
			if (siegeclan == null)
			{
				continue;
			}

			final var clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
			for (final var member : clan.getOnlineMembers(0))
			{
				if (member == null)
				{
					continue;
				}

				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setSiegeSide(0);
					member.setIsInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 2);
					member.setSiegeSide(getCastle().getId());
					if (checkIfInZone(member))
					{
						member.setIsInSiege(true);
						member.startFameTask(Config.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, Config.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				member.sendUserInfo();
				member.broadcastRelationChanged();
			}
		}
	}

	public void approveSiegeDefenderClan(int clanId)
	{
		if (clanId <= 0)
		{
			return;
		}
		saveSiegeClan(ClanHolder.getInstance().getClan(clanId), DEFENDER, true);
		loadSiegeClan(false);
	}

	public boolean checkIfInZone(GameObject object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}

	public boolean checkIfInZone(int x, int y, int z)
	{
		return (getIsInProgress() && (getCastle().checkIfInZone(x, y, z)));
	}

	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		return (getAttackerClan(clan) != null);
	}

	@Override
	public boolean checkIsDefender(Clan clan)
	{
		return (getDefenderClan(clan) != null);
	}

	public boolean checkIsDefenderWaiting(Clan clan)
	{
		return (getDefenderWaitingClan(clan) != null);
	}

	public void clearSiegeClan()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=?");
			statement.setInt(1, getCastle().getId());
			statement.execute();
			
			if (getCastle().getOwnerId() > 0)
			{
				statement.close();
				statement = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?");
				statement.setInt(1, getCastle().getOwnerId());
				statement.execute();
			}

			getAttackerClans().clear();
			getDefenderClans().clear();
			getDefenderWaitingClans().clear();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: clearSiegeClan(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void clearSiegeWaitingClan()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and type = 2");
			statement.setInt(1, getCastle().getId());
			statement.execute();
			getDefenderWaitingClans().clear();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: clearSiegeWaitingClan(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	@Override
	public List<Player> getAttackersInZone()
	{
		final List<Player> players = new ArrayList<>();
		for (final var siegeclan : getAttackerClans())
		{
			final var clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
			for (final var player : clan.getOnlineMembers(0))
			{
				if (player == null)
				{
					continue;
				}

				if (player.isInSiege())
				{
					players.add(player);
				}
			}
		}
		return players;
	}

	public List<Player> getDefendersButNotOwnersInZone()
	{
		final List<Player> players = new ArrayList<>();
		for (final var siegeclan : getDefenderClans())
		{
			final var clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
			if (clan.getId() == getCastle().getOwnerId())
			{
				continue;
			}
			for (final var player : clan.getOnlineMembers(0))
			{
				if (player == null)
				{
					continue;
				}

				if (player.isInSiege())
				{
					players.add(player);
				}
			}
		}
		return players;
	}

	public List<Player> getPlayersInZone()
	{
		return getCastle().getZone().getPlayersInside();
	}

	public List<Player> getOwnersInZone()
	{
		final List<Player> players = new ArrayList<>();
		for (final var siegeclan : getDefenderClans())
		{
			final var clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
			if (clan.getId() != getCastle().getOwnerId())
			{
				continue;
			}
			for (final var player : clan.getOnlineMembers(0))
			{
				if (player == null)
				{
					continue;
				}

				if (player.isInSiege())
				{
					players.add(player);
				}
			}
		}
		return players;
	}

	public List<Player> getSpectatorsInZone()
	{
		final List<Player> players = new ArrayList<>();
		for (final var player : getCastle().getZone().getPlayersInside())
		{
			if (player == null)
			{
				continue;
			}

			if (!player.isInSiege())
			{
				players.add(player);
			}
		}
		return players;
	}

	public void killedCT(Npc ct)
	{
		_controlTowerCount--;
		if (_controlTowerCount < 0)
		{
			_controlTowerCount = 0;
		}
	}

	public void killedFlag(Npc flag)
	{
		if (flag == null)
		{
			return;
		}
		for (final var clan : getAttackerClans())
		{
			if (clan.removeFlag(flag))
			{
				return;
			}
		}
	}

	public void listRegisterClan(Player player)
	{
		final var tpl = _castle.getTemplate();
		if (tpl == null || !tpl.isEnableSiege())
		{
			player.sendPacket(SystemMessageId.NOT_SIEGE_REGISTRATION_TIME2);
			return;
		}
		player.sendPacket(new CastleSiegeInfo(getCastle()));
	}

	public void registerAttacker(Player player)
	{
		registerAttacker(player, false);
	}

	public void registerAttacker(Player player, boolean force)
	{
		if (player.getClan() == null)
		{
			return;
		}
		int allyId = 0;
		if (getCastle().getOwnerId() != 0)
		{
			allyId = ClanHolder.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
		}
		if (allyId != 0)
		{
			if ((player.getClan().getAllyId() == allyId) && !force)
			{
				player.sendPacket(SystemMessageId.CANNOT_ATTACK_ALLIANCE_CASTLE);
				return;
			}
		}
		if (force)
		{
			if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), getCastle().getId()))
			{
				player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
			}
			else
			{
				saveSiegeClan(player.getClan(), ATTACKER, false);
			}
			return;
		}

		if (checkIfCanRegister(player, ATTACKER))
		{
			saveSiegeClan(player.getClan(), ATTACKER, false);
		}
	}

	public void registerDefender(Player player)
	{
		registerDefender(player, false);
	}

	public void registerDefender(Player player, boolean force)
	{
		if (getCastle().getOwnerId() <= 0)
		{
			player.sendMessage("You cannot register as a defender because " + getCastle().getName(null) + " is owned by NPC.");
			return;
		}

		if (force)
		{
			if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), getCastle().getId()))
			{
				player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
			}
			else
			{
				saveSiegeClan(player.getClan(), DEFENDER_NOT_APPROWED, false);
			}
			return;
		}

		if (checkIfCanRegister(player, DEFENDER_NOT_APPROWED))
		{
			saveSiegeClan(player.getClan(), DEFENDER_NOT_APPROWED, false);
		}
	}

	public void removeSiegeClan(int clanId)
	{
		if (clanId <= 0)
		{
			return;
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and clan_id=?");
			statement.setInt(1, getCastle().getId());
			statement.setInt(2, clanId);
			statement.execute();

			loadSiegeClan(false);
		}
		catch (final Exception e)
		{
			_log.warn("Exception: removeSiegeClan(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void removeSiegeClan(Clan clan)
	{
		if ((clan == null) || (clan.getCastleId() == getCastle().getId()) || !SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getId()))
		{
			return;
		}
		removeSiegeClan(clan.getId());
	}

	public void removeSiegeClan(Player player)
	{
		removeSiegeClan(player.getClan());
	}

	public void startAutoTask()
	{
		correctSiegeDateTime();

		if (_siegeStartTime < System.currentTimeMillis())
		{
			return;
		}
		_log.info(getCastle().getName(null) + " Siege: Will begin at " + new Date(_siegeStartTime));

		loadSiegeClan(false);
		
		_siegeCloseChangeDataTime = _siegeStartTime - (getCastle().getTemplate().getCloseChangeDataTime() * 60000L);
		_siegeCloseRegistrationTime = _siegeStartTime - (getCastle().getTemplate().getCloseRegistrationTime() * 60000L);

		final var tpl = getCastle().getTemplate();
		if (tpl != null && tpl.isEnableSiege())
		{
			SiegeTaskManager.getInstance().addSiegeTask(getCastle().getId(), new SiegeTemplate(this, true), _siegeStartTime);
		}
	}

	public void teleportPlayer(TeleportWhoType teleportWho, TeleportWhereType teleportWhere)
	{
		List<Player> players;
		switch (teleportWho)
		{
			case Owner :
				players = getOwnersInZone();
				break;
			case Attacker :
				players = getAttackersInZone();
				break;
			case DefenderNotOwner :
				players = getDefendersButNotOwnersInZone();
				break;
			case Spectator :
				players = getSpectatorsInZone();
				break;
			default :
				players = getPlayersInZone();
		}

		for (final var player : players)
		{
			if (player.canOverrideCond(PcCondOverride.CASTLE_CONDITIONS) || player.isJailed())
			{
				continue;
			}
			player.teleToLocation(teleportWhere, true, ReflectionManager.DEFAULT);
		}
	}

	private void addAttacker(int clanId)
	{
		getAttackerClans().add(new SiegeClan(clanId, SiegeClanType.ATTACKER));
	}

	private void addDefender(int clanId)
	{
		getDefenderClans().add(new SiegeClan(clanId, SiegeClanType.DEFENDER));
	}

	private void addDefender(int clanId, SiegeClanType type)
	{
		getDefenderClans().add(new SiegeClan(clanId, type));
	}

	private void addDefenderWaiting(int clanId)
	{
		getDefenderWaitingClans().add(new SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING));
	}

	private boolean checkIfCanRegister(Player player, byte typeId)
	{
		final var castle = getCastle();
		if (isRegistrationTimeOver())
		{
			final var sm = SystemMessage.getSystemMessage(SystemMessageId.DEADLINE_FOR_SIEGE_S1_PASSED);
			sm.addCastleId(castle.getId());
			player.sendPacket(sm);
		}
		else if (getIsInProgress() || castle == null || castle.getTemplate() == null || !castle.getTemplate().isEnableSiege())
		{
			player.sendPacket(SystemMessageId.NOT_SIEGE_REGISTRATION_TIME2);
		}
		else if ((player.getClan() == null) || (player.getClan().getLevel() < castle.getTemplate().getMinSiegeClanLevel()))
		{
			player.sendPacket(SystemMessageId.ONLY_CLAN_LEVEL_5_ABOVE_MAY_SIEGE);
		}
		else if (player.getClan().getId() == castle.getOwnerId())
		{
			player.sendPacket(SystemMessageId.CLAN_THAT_OWNS_CASTLE_IS_AUTOMATICALLY_REGISTERED_DEFENDING);
		}
		else if (player.getClan().getCastleId() > 0)
		{
			player.sendPacket(SystemMessageId.CLAN_THAT_OWNS_CASTLE_CANNOT_PARTICIPATE_OTHER_SIEGE);
		}
		else if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), castle.getId()))
		{
			player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
		}
		else if (checkIfAlreadyRegisteredForSameDay(player.getClan()))
		{
			player.sendPacket(SystemMessageId.APPLICATION_DENIED_BECAUSE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE);
		}
		else if ((typeId == ATTACKER) && (getAttackerClans().size() >= castle.getTemplate().getAttackerClansLimit()))
		{
			player.sendPacket(SystemMessageId.ATTACKER_SIDE_FULL);
		}
		else if (((typeId == DEFENDER) || (typeId == DEFENDER_NOT_APPROWED) || (typeId == OWNER)) && ((getDefenderClans().size() + getDefenderWaitingClans().size()) >= castle.getTemplate().getDefenderClansLimit()))
		{
			player.sendPacket(SystemMessageId.DEFENDER_SIDE_FULL);
		}
		else
		{
			return true;
		}
		return false;
	}

	public boolean checkIfAlreadyRegisteredForSameDay(Clan clan)
	{
		final Calendar castleSiege = Calendar.getInstance();
		castleSiege.setTimeInMillis(getSiegeStartTime());
		for (final var siege : SiegeManager.getInstance().getSieges())
		{
			if (siege == this)
			{
				continue;
			}
			
			final Calendar anySiege = Calendar.getInstance();
			anySiege.setTimeInMillis(siege.getSiegeStartTime());
			
			if (anySiege.get(Calendar.DAY_OF_WEEK) == castleSiege.get(Calendar.DAY_OF_WEEK))
			{
				if (siege.checkIsAttacker(clan))
				{
					return true;
				}
				if (siege.checkIsDefender(clan))
				{
					return true;
				}
				if (siege.checkIsDefenderWaiting(clan))
				{
					return true;
				}
			}
		}
		return false;
	}

	public void correctSiegeDateTime()
	{
		boolean corrected = false;

		if (getSiegeStartTime() < System.currentTimeMillis())
		{
			corrected = true;
			setNextSiegeDate();
		}

		if (!corrected && Config.ALLOW_CHECK_SEVEN_SIGN_STATUS)
		{
			final Calendar castleSiege = Calendar.getInstance();
			castleSiege.setTimeInMillis(getSiegeStartTime());
			if (!SevenSigns.getInstance().isDateInSealValidPeriod(castleSiege))
			{
				corrected = true;
				setNextSiegeDate();
			}
		}

		if (corrected)
		{
			saveSiegeDate();
		}
		else
		{
			_siegeCloseChangeDataTime = _siegeStartTime - (getCastle().getTemplate().getCloseChangeDataTime() * 60000L);
			_siegeCloseRegistrationTime = _siegeStartTime - (getCastle().getTemplate().getCloseRegistrationTime() * 60000L);
		}
	}

	private void loadSiegeClan(boolean checkForts)
	{
		final List<Integer> clanList = new ArrayList<>();
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan_id,type FROM siege_clans where castle_id=?");
			getAttackerClans().clear();
			getDefenderClans().clear();
			getDefenderWaitingClans().clear();

			if (getCastle().getOwnerId() > 0)
			{
				addDefender(getCastle().getOwnerId(), SiegeClanType.OWNER);
				clanList.add(getCastle().getOwnerId());
			}

			statement.setInt(1, getCastle().getId());
			rs = statement.executeQuery();
			int typeId;
			while (rs.next())
			{
				typeId = rs.getInt("type");
				if (typeId == DEFENDER)
				{
					addDefender(rs.getInt("clan_id"));
				}
				else if (typeId == ATTACKER)
				{
					addAttacker(rs.getInt("clan_id"));
				}
				else if (typeId == DEFENDER_NOT_APPROWED)
				{
					addDefenderWaiting(rs.getInt("clan_id"));
				}
				
				if (checkForts)
				{
					clanList.add(rs.getInt("clan_id"));
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception: loadSiegeClan(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
		
		if (checkForts && !clanList.isEmpty())
		{
			for (final int clanId : clanList)
			{
				final Clan clan = ClanHolder.getInstance().getClan(clanId);
				if (clan == null)
				{
					continue;
				}
				
				for (final Fort fort : FortManager.getInstance().getForts())
				{
					if (fort != null && fort.getSiege().getAttackerClan(clan) != null)
					{
						if (fort.getSiege().getIsInProgress() || (fort.getSiege().getSiegeStartTime() < (System.currentTimeMillis() + 7200000L)))
						{
							fort.getSiege().removeSiegeClan(clan);
						}
					}
				}
			}
		}
	}

	private void removeControlTower()
	{
		if ((_controlTowers != null) && !_controlTowers.isEmpty())
		{
			for (final var ct : _controlTowers)
			{
				if (ct != null)
				{
					try
					{
						ct.deleteMe();
					}
					catch (final Exception e)
					{
						_log.warn("Exception: removeControlTower(): " + e.getMessage(), e);
					}
				}
			}
			_controlTowers.clear();
			_controlTowers = null;
		}
	}

	private void removeFlameTower()
	{
		if ((_flameTowers != null) && !_flameTowers.isEmpty())
		{
			for (final var ct : _flameTowers)
			{
				if (ct != null)
				{
					try
					{
						ct.deleteMe();
					}
					catch (final Exception e)
					{
						_log.warn("Exception: removeFlamelTower(): " + e.getMessage(), e);
					}
				}
			}
			_flameTowers.clear();
			_flameTowers = null;
		}
	}

	private void removeFlags()
	{
		for (final var sc : getAttackerClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
		for (final var sc : getDefenderClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}

	private void removeDefenderFlags()
	{
		for (final var sc : getDefenderClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}

	private void saveCastleSiege()
	{
		setNextSiegeDate();
		saveSiegeDate();
		startAutoTask();
	}

	public void saveSiegeDate()
	{
		final var tpl = getCastle().getTemplate();
		if (tpl != null && tpl.isEnableSiege())
		{
			SiegeTaskManager.getInstance().addSiegeTask(getCastle().getId(), new SiegeTemplate(this, true), _siegeStartTime);
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE castle SET siegeDate = ? WHERE id = ?");
			statement.setLong(1, getSiegeStartTime());
			statement.setInt(2, getCastle().getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: saveSiegeDate(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void saveSiegeClan(Clan clan, byte typeId, boolean isUpdateRegistration)
	{
		final var castle = getCastle();
		if (clan.getCastleId() > 0 || castle == null || castle.getTemplate() == null)
		{
			return;
		}

		if ((typeId == DEFENDER) || (typeId == DEFENDER_NOT_APPROWED) || (typeId == OWNER))
		{
			if ((getDefenderClans().size() + getDefenderWaitingClans().size()) >= castle.getTemplate().getDefenderClansLimit())
			{
				return;
			}
		}
		else
		{
			if (getAttackerClans().size() >= castle.getTemplate().getAttackerClansLimit())
			{
				return;
			}
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if (!isUpdateRegistration)
			{
				statement = con.prepareStatement("INSERT INTO siege_clans (clan_id,castle_id,type,castle_owner) values (?,?,?,0)");
				statement.setInt(1, clan.getId());
				statement.setInt(2, castle.getId());
				statement.setInt(3, typeId);
				statement.execute();
			}
			else
			{
				statement = con.prepareStatement("UPDATE siege_clans SET type = ? WHERE castle_id = ? AND clan_id = ?");
				statement.setInt(1, typeId);
				statement.setInt(2, castle.getId());
				statement.setInt(3, clan.getId());
				statement.execute();
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception: saveSiegeClan(Clan clan, int typeId, boolean isUpdateRegistration): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		if ((typeId == DEFENDER) || (typeId == OWNER))
		{
			addDefender(clan.getId());
		}
		else if (typeId == ATTACKER)
		{
			addAttacker(clan.getId());
		}
		else if (typeId == DEFENDER_NOT_APPROWED)
		{
			addDefenderWaiting(clan.getId());
		}
	}

	private void setNextSiegeDate()
	{
		final var castle = getCastle();
		if (castle == null || castle.getTemplate() == null || !castle.getTemplate().isEnableSiege())
		{
			return;
		}
		
		final String siegeDate = castle.getTemplate().getSiegeDate();
		if (siegeDate != null && !siegeDate.isEmpty())
		{
			SchedulingPattern cronTime;
			try
			{
				cronTime = new SchedulingPattern(siegeDate);
			}
			catch (final InvalidPatternException e)
			{
				return;
			}
			
			final long nextTime = cronTime.next(System.currentTimeMillis());
			if (Config.ALLOW_CHECK_SEVEN_SIGN_STATUS)
			{
				final Calendar castleSiege = Calendar.getInstance();
				castleSiege.setTimeInMillis(nextTime);
				if (!SevenSigns.getInstance().isDateInSealValidPeriod(castleSiege))
				{
					castleSiege.add(Calendar.DAY_OF_MONTH, 7);
				}
				setSiegeStartTime(castleSiege.getTimeInMillis());
			}
			else
			{
				setSiegeStartTime(nextTime);
			}
		}
		else
		{
			final Calendar castleSiege = Calendar.getInstance();
			castleSiege.setTimeInMillis(getSiegeStartTime());
			while (castleSiege.getTimeInMillis() < System.currentTimeMillis())
			{
				if ((castleSiege.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) && (castleSiege.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY))
				{
					castleSiege.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
				}
				
				if ((castleSiege.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY))
				{
					castleSiege.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				}
				castleSiege.set(Calendar.HOUR_OF_DAY, 20);
				castleSiege.set(Calendar.MINUTE, 0);
				castleSiege.set(Calendar.SECOND, 0);
				castleSiege.add(Calendar.DAY_OF_MONTH, 7);
			}
			
			if (Config.ALLOW_CHECK_SEVEN_SIGN_STATUS)
			{
				if (!SevenSigns.getInstance().isDateInSealValidPeriod(castleSiege))
				{
					castleSiege.add(Calendar.DAY_OF_MONTH, 7);
				}
			}
			setSiegeStartTime(castleSiege.getTimeInMillis());
		}
		_siegeCloseChangeDataTime = _siegeStartTime - (getCastle().getTemplate().getCloseChangeDataTime() * 60000L);
		_siegeCloseRegistrationTime = _siegeStartTime - (getCastle().getTemplate().getCloseRegistrationTime() * 60000L);
		final var sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ANNOUNCED_SIEGE_TIME);
		sm.addCastleId(getCastle().getId());
		GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(sm));
	}

	private void spawnControlTower(int id)
	{
		if (_controlTowers == null)
		{
			_controlTowers = new ArrayList<>();
		}

		final var list = SiegeManager.getInstance().getControlTowerSpawnList(id);
		if (list.isEmpty())
		{
			return;
		}
		
		for (final var sp : list)
		{
			final var template = NpcsParser.getInstance().getTemplate(sp.getNpcId());
			if (template != null)
			{
				final var ct = new ControlTowerInstance(IdFactory.getInstance().getNextId(), template);
				ct.setCurrentHpMp(sp.getHp(), ct.getMaxMp());
				ct.spawnMe(sp.getLocation());
				_controlTowerCount++;
				_controlTowerMaxCount++;
				_controlTowers.add(ct);
			}
		}
	}

	private void spawnFlameTower(int id)
	{
		if (_flameTowers == null)
		{
			_flameTowers = new ArrayList<>();
		}
		
		final var list = SiegeManager.getInstance().getFlameTowerSpawnList(id);
		if (list.isEmpty())
		{
			_flameTowerCount = 1;
			return;
		}

		for (final SiegeSpawn sp : list)
		{
			final var template = NpcsParser.getInstance().getTemplate(sp.getNpcId());
			if (template != null)
			{
				final var ct = new FlameTowerInstance(IdFactory.getInstance().getNextId(), template);
				ct.setCurrentHpMp(sp.getHp(), ct.getMaxMp());
				ct.spawnMe(sp.getLocation());
				_flameTowerCount++;
				_flameTowerMaxCount++;
				_flameTowers.add(ct);
			}
		}
		
		if (_flameTowerCount == 0)
		{
			_flameTowerCount = 1;
		}
	}

	private void spawnSiegeGuard()
	{
		getSiegeGuardManager().spawnSiegeGuard();
		if (!getSiegeGuardManager().getSiegeGuardSpawn().isEmpty() && !_controlTowers.isEmpty())
		{
			ControlTowerInstance closestCt;
			int x, y, z;
			double distance;
			double distanceClosest = 0;
			for (final Spawner spawn : getSiegeGuardManager().getSiegeGuardSpawn())
			{
				if (spawn == null)
				{
					continue;
				}

				closestCt = null;
				distanceClosest = Integer.MAX_VALUE;

				x = spawn.getX();
				y = spawn.getY();
				z = spawn.getZ();

				for (final ControlTowerInstance ct : _controlTowers)
				{
					if (ct == null)
					{
						continue;
					}

					distance = ct.getDistanceSq(x, y, z);

					if (distance < distanceClosest)
					{
						closestCt = ct;
						distanceClosest = distance;
					}
				}
				if (closestCt != null)
				{
					closestCt.registerGuard(spawn);
				}
			}
		}
	}

	@Override
	public final SiegeClan getAttackerClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		return getAttackerClan(clan.getId());
	}

	@Override
	public final SiegeClan getAttackerClan(int clanId)
	{
		for (final var sc : getAttackerClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		return null;
	}

	@Override
	public final List<SiegeClan> getAttackerClans()
	{
		if (_isNormalSide)
		{
			return _attackerClans;
		}
		return _defenderClans;
	}

	public final Castle getCastle()
	{
		return _castle;
	}

	@Override
	public final SiegeClan getDefenderClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		return getDefenderClan(clan.getId());
	}

	@Override
	public final SiegeClan getDefenderClan(int clanId)
	{
		for (final var sc : getDefenderClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		return null;
	}

	@Override
	public final List<SiegeClan> getDefenderClans()
	{
		if (_isNormalSide)
		{
			return _defenderClans;
		}
		return _attackerClans;
	}

	public final SiegeClan getDefenderWaitingClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		return getDefenderWaitingClan(clan.getId());
	}

	public final SiegeClan getDefenderWaitingClan(int clanId)
	{
		for (final var sc : getDefenderWaitingClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		return null;
	}

	public final List<SiegeClan> getDefenderWaitingClans()
	{
		return _defenderWaitingClans;
	}

	public final boolean getIsInProgress()
	{
		return _isInProgress;
	}

	public final boolean isRegistrationTimeOver()
	{
		return _siegeCloseRegistrationTime < System.currentTimeMillis();
	}

	public final boolean isTimeChangeDataOver()
	{
		return _siegeCloseChangeDataTime < System.currentTimeMillis();
	}
	
	public final void setIsTimeChangeDataOver(long val)
	{
		_siegeCloseChangeDataTime = val;
	}

	@Override
	public final long getSiegeStartTime()
	{
		return _siegeStartTime;
	}

	public final void setSiegeStartTime(long val)
	{
		_siegeStartTime = val;
	}
	
	@Override
	public List<Npc> getFlag(Clan clan)
	{
		if (clan != null)
		{
			final var sc = getAttackerClan(clan);
			if (sc != null)
			{
				return sc.getFlag();
			}
		}
		return null;
	}

	public final SiegeGuardManager getSiegeGuardManager()
	{
		if (_siegeGuardManager == null)
		{
			_siegeGuardManager = new SiegeGuardManager(getCastle());
		}
		return _siegeGuardManager;
	}

	public int getControlTowerCount()
	{
		return _controlTowerCount;
	}

	public int getControlTowerMaxCount()
	{
		return _controlTowerMaxCount;
	}

	public int getFlameTowerMaxCount()
	{
		return _flameTowerMaxCount;
	}

	public void disableTraps()
	{
		_flameTowerCount--;
	}

	public boolean isTrapsActive()
	{
		return _flameTowerCount > 0;
	}

	@Override
	public boolean giveFame()
	{
		return true;
	}

	@Override
	public int getFameFrequency()
	{
		return Config.CASTLE_ZONE_FAME_TASK_FREQUENCY;
	}

	@Override
	public int getFameAmount()
	{
		return Config.CASTLE_ZONE_FAME_AQUIRE_POINTS;
	}

	@Override
	public void updateSiege()
	{
	}

	protected int getArtifactCount(int casleId)
	{
		return ((casleId == 7) || (casleId == 9)) ? 2 : 1;
	}
	
	public <T extends Listener<Siege>> boolean addListener(T listener)
	{
		return _listeners.add(listener);
	}
	
	public <T extends Listener<Siege>> boolean removeListener(T listener)
	{
		return _listeners.remove(listener);
	}
}