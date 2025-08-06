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
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.listener.Listener;
import l2e.commons.listener.ListenerList;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.FortSiegeGuardManager;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.RewardManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.listener.other.OnFortSiegeStatusListener;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.CombatFlag;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.SiegeClan.SiegeClanType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.BackupPowerUnitInstance;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.FortCommanderInstance;
import l2e.gameserver.model.actor.instance.PowerControlUnitInstance;
import l2e.gameserver.model.actor.templates.daily.DailyTaskTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.player.PlayerTaskTemplate;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.model.spawn.SpawnFortSiege;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class FortSiege implements Siegable
{
	protected static final Logger _log = LoggerFactory.getLogger(FortSiege.class);
	
	public static enum TeleportWhoType
	{
		All, Attacker, Owner,
	}

	private static final String DELETE_FORT_SIEGECLANS_BY_CLAN_ID = "DELETE FROM fortsiege_clans WHERE fort_id = ? AND clan_id = ?";
	private static final String DELETE_FORT_SIEGECLANS = "DELETE FROM fortsiege_clans WHERE fort_id = ?";

	public class ScheduleEndSiegeTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!getIsInProgress())
			{
				return;
			}

			try
			{
				_siegeEnd = null;
				endSiege();
			}
			catch (final Exception e)
			{
				_log.warn("Exception: ScheduleEndSiegeTask() for Fort: " + _fort.getName() + " " + e.getMessage(), e);
			}
		}
	}
	
	private final OnFortSiegeStatusList _listeners = new OnFortSiegeStatusList();
	
	private class OnFortSiegeStatusList extends ListenerList<FortSiege>
	{
		public void onStart(FortSiege siege)
		{
			if (!getListeners().isEmpty())
			{
				for (final Listener<FortSiege> listener : getListeners())
				{
					((OnFortSiegeStatusListener) listener).onStart(FortSiege.this);
				}
			}
		}
		
		public void onEnd(FortSiege siege, Clan winClan, Clan defClan)
		{
			if (!getListeners().isEmpty())
			{
				for (final Listener<FortSiege> listener : getListeners())
				{
					((OnFortSiegeStatusListener) listener).onEnd(FortSiege.this, winClan, defClan);
				}
			}
		}
	}

	public class ScheduleStartSiegeTask implements Runnable
	{
		private final Fort _fortInst;
		private final int _time;

		public ScheduleStartSiegeTask(int time)
		{
			_fortInst = _fort;
			_time = time;
		}

		@Override
		public void run()
		{
			if (getIsInProgress())
			{
				return;
			}

			try
			{
				final SystemMessage sm;
				if (_time == 3600)
				{
					ThreadPoolManager.getInstance().schedule(new ScheduleStartSiegeTask(600), 3000000);
				}
				else if (_time == 600)
				{
					getFort().despawnSuspiciousMerchant();
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_MINUTES_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(10);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().schedule(new ScheduleStartSiegeTask(300), 300000);
				}
				else if (_time == 300)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_MINUTES_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(5);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().schedule(new ScheduleStartSiegeTask(60), 240000);
				}
				else if (_time == 60)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_MINUTES_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(1);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().schedule(new ScheduleStartSiegeTask(30), 30000);
				}
				else if (_time == 30)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(30);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().schedule(new ScheduleStartSiegeTask(10), 20000);
				}
				else if (_time == 10)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(10);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().schedule(new ScheduleStartSiegeTask(5), 5000);
				}
				else if (_time == 5)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(5);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().schedule(new ScheduleStartSiegeTask(1), 4000);
				}
				else if (_time == 1)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(1);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().schedule(new ScheduleStartSiegeTask(0), 1000);
				}
				else if (_time == 0)
				{
					_fortInst.getSiege().startSiege();
				}
				else
				{
					_log.warn("Exception: ScheduleStartSiegeTask(): unknown siege time: " + String.valueOf(_time));
				}
			}
			catch (final Exception e)
			{
				_log.warn("Exception: ScheduleStartSiegeTask() for Fort: " + _fortInst.getName() + " " + e.getMessage(), e);
			}
		}
	}

	public class ScheduleSuspiciousMerchantSpawn implements Runnable
	{
		@Override
		public void run()
		{
			if (getIsInProgress())
			{
				return;
			}

			try
			{
				_fort.spawnSuspiciousMerchant();
			}
			catch (final Exception e)
			{
				_log.warn("Exception: ScheduleSuspicoiusMerchantSpawn() for Fort: " + _fort.getName() + " " + e.getMessage(), e);
			}
		}
	}

	public class ScheduleSiegeRestore implements Runnable
	{
		@Override
		public void run()
		{
			if (!getIsInProgress())
			{
				return;
			}

			try
			{
				_siegeRestore = null;
				resetSiege();
				announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.BARRACKS_FUNCTION_RESTORED));
			}
			catch (final Exception e)
			{
				_log.warn("Exception: ScheduleSiegeRestore() for Fort: " + _fort.getName() + " " + e.getMessage(), e);
			}
		}
	}

	private final List<SiegeClan> _attackerClans = new CopyOnWriteArrayList<>();

	protected List<Spawner> _commanders = new CopyOnWriteArrayList<>();
	protected List<Spawner> _powerUnits = new CopyOnWriteArrayList<>();
	protected List<Spawner> _controlUnits = new CopyOnWriteArrayList<>();
	protected List<Spawner> _mainMachines = new CopyOnWriteArrayList<>();
	protected final Fort _fort;
	private boolean _isControlDisabled = true;
	private boolean _isControlDoorsOpen = false;
	private boolean _isInProgress = false;
	private FortSiegeGuardManager _siegeGuardManager;
	ScheduledFuture<?> _siegeEnd = null;
	ScheduledFuture<?> _siegeRestore = null;
	ScheduledFuture<?> _siegeStartTask = null;
	protected int _firstOwnerClanId = -1;

	public FortSiege(Fort fort)
	{
		_fort = fort;

		checkAutoTask();
		FortSiegeManager.getInstance().addSiege(this);
	}

	@Override
	public void endSiege()
	{
		if (getIsInProgress())
		{
			_isInProgress = false;
			_isControlDisabled = true;
			_isControlDoorsOpen = false;
			removeFlags();
			unSpawnFlags();

			updatePlayerSiegeStateFlags(true);

			Clan clanWon = null;
			Clan clanDef = null;
			int ownerId = -1;
			
			final var isAchActive = AchievementManager.getInstance().isActive();
			if (getFort().getOwnerClan() != null)
			{
				ownerId = getFort().getOwnerClan().getId();
				if (ownerId == _firstOwnerClanId)
				{
					clanDef = getFort().getOwnerClan();
					RewardManager.getInstance().checkFortDefenceReward(getFort().getOwnerClan(), getFort().getId());
					
					for (final Player member : getFort().getOwnerClan().getOnlineMembers(0))
					{
						if (member != null && member.isOnline())
						{
							if (isAchActive)
							{
								member.getCounters().addAchivementInfo("fortSiegesDefend", 0, -1, false, false, false);
							}
							
							if (Config.ALLOW_DAILY_TASKS)
							{
								if (member.getActiveDailyTasks() != null)
								{
									for (final PlayerTaskTemplate taskTemplate : member.getActiveDailyTasks())
									{
										if (taskTemplate.getType().equalsIgnoreCase("Siege") && !taskTemplate.isComplete())
										{
											final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
											if (task.getSiegeFort() && !task.isAttackSiege())
											{
												taskTemplate.setIsComplete(true);
												member.updateDailyStatus(taskTemplate);
												final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler("missions");
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
					RewardManager.getInstance().checkFortCaptureReward(getFort().getOwnerClan(), getFort().getId());
					clanWon = getFort().getOwnerClan();
				}
			}
			getFort().getZone().banishForeigners(ownerId);
			getFort().getZone().setIsActive(false);
			getFort().getZone().updateZoneStatusForCharactersInside();
			getFort().getZone().setSiegeInstance(null);
			_listeners.onEnd(this, clanWon, clanDef);
			saveFortSiege();
			clearSiegeClan();
			removeCommanders();
			removePowerUnits();
			removeControlUnits();
			removeMainMachine();

			getFort().spawnNpcCommanders();
			getSiegeGuardManager().unspawnSiegeGuard();
			getFort().resetDoors();

			ThreadPoolManager.getInstance().schedule(new ScheduleSuspiciousMerchantSpawn(), FortSiegeManager.getInstance().getSuspiciousMerchantRespawnDelay() * 60 * 1000L);
			setSiegeDateTime(true);

			if (_siegeEnd != null)
			{
				_siegeEnd.cancel(true);
				_siegeEnd = null;
			}
			if (_siegeRestore != null)
			{
				_siegeRestore.cancel(true);
				_siegeRestore = null;
			}

			if ((getFort().getOwnerClan() != null) && (getFort().getFlagPole().getMeshIndex() == 0))
			{
				getFort().setVisibleFlag(true);
			}

			_log.info("Siege of " + getFort().getName() + " fort finished.");
		}
	}

	@Override
	public void startSiege()
	{
		if (!getIsInProgress())
		{
			if (_siegeStartTask != null)
			{
				_siegeStartTask.cancel(true);
				getFort().despawnSuspiciousMerchant();
			}
			_siegeStartTask = null;

			if (getAttackerClans().isEmpty())
			{
				return;
			}

			_isInProgress = true;
			_isControlDisabled = true;
			_isControlDoorsOpen = false;
			
			loadSiegeClan();
			teleportPlayer(FortSiege.TeleportWhoType.Attacker, TeleportWhereType.TOWN);
			if (getFort().getOwnerClan() != null)
			{
				_firstOwnerClanId = getFort().getOwnerClan().getId();
			}
			getFort().despawnNpcCommanders();
			getFort().despawnSpecialEnvoys();
			spawnCommanders();
			spawnControlUnits();
			getFort().resetDoors();
			spawnSiegeGuard();
			getFort().setVisibleFlag(false);
			getFort().getZone().setSiegeInstance(this);
			getFort().getZone().setIsActive(true);
			getFort().getZone().updateZoneStatusForCharactersInside();
			updatePlayerSiegeStateFlags(false);

			_siegeEnd = ThreadPoolManager.getInstance().schedule(new ScheduleEndSiegeTask(), FortSiegeManager.getInstance().getSiegeLength() * 60 * 1000L);

			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_S1_HAS_BEGUN);
			sm.addCastleId(getFort().getId());
			announceToPlayer(sm);
			saveFortSiege();
			_listeners.onStart(this);
			_log.info("Siege of " + getFort().getName() + " fort started.");
		}
	}

	public void announceToPlayer(SystemMessage sm)
	{
		Clan clan;
		for (final SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
			for (final Player member : clan.getOnlineMembers(0))
			{
				if (member != null)
				{
					member.sendPacket(sm);
				}
			}
		}
		if (getFort().getOwnerClan() != null)
		{
			clan = ClanHolder.getInstance().getClan(getFort().getOwnerClan().getId());
			for (final Player member : clan.getOnlineMembers(0))
			{
				if (member != null)
				{
					member.sendPacket(sm);
				}
			}
		}
	}

	public void announceToPlayer(SystemMessage sm, String s)
	{
		sm.addString(s);
		announceToPlayer(sm);
	}

	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		Clan clan;
		for (final SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
			for (final Player member : clan.getOnlineMembers(0))
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
					member.setSiegeSide(getFort().getId());
					if (checkIfInZone(member))
					{
						member.setIsInSiege(true);
						member.startFameTask(Config.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, Config.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				member.broadcastUserInfo(true);
			}
		}
		if (getFort().getOwnerClan() != null)
		{
			clan = ClanHolder.getInstance().getClan(getFort().getOwnerClan().getId());
			for (final Player member : clan.getOnlineMembers(0))
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
					member.setSiegeSide(getFort().getId());
					if (checkIfInZone(member))
					{
						member.setIsInSiege(true);
						member.startFameTask(Config.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, Config.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				member.broadcastUserInfo(true);
			}
		}
	}

	public boolean checkIfInZone(GameObject object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}

	public boolean checkIfInZone(int x, int y, int z)
	{
		return (getIsInProgress() && (getFort().checkIfInZone(x, y, z)));
	}

	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		return (getAttackerClan(clan) != null);
	}

	@Override
	public boolean checkIsDefender(Clan clan)
	{
		if ((clan != null) && (getFort().getOwnerClan() == clan))
		{
			return true;
		}

		return false;
	}

	public void clearSiegeClan()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=?");
			statement.setInt(1, getFort().getId());
			statement.execute();
			statement.close();
			
			if (getFort().getOwnerClan() != null)
			{
				statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE clan_id=?");
				statement.setInt(1, getFort().getOwnerClan().getId());
				statement.execute();
			}

			getAttackerClans().clear();

			if (getIsInProgress())
			{
				endSiege();
			}

			if (_siegeStartTask != null)
			{
				_siegeStartTask.cancel(true);
				_siegeStartTask = null;
			}
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

	private void clearSiegeDate()
	{
		getFort().getSiegeDate().setTimeInMillis(0);
	}

	@Override
	public List<Player> getAttackersInZone()
	{
		final List<Player> players = new LinkedList<>();
		for (final SiegeClan siegeclan : getAttackerClans())
		{
			final Clan clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
			for (final Player player : clan.getOnlineMembers(0))
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
		return getFort().getZone().getPlayersInside();
	}

	public List<Player> getOwnersInZone()
	{
		final List<Player> players = new LinkedList<>();
		if (getFort().getOwnerClan() != null)
		{
			final Clan clan = ClanHolder.getInstance().getClan(getFort().getOwnerClan().getId());
			if (clan != getFort().getOwnerClan())
			{
				return null;
			}

			for (final Player player : clan.getOnlineMembers(0))
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

	public void killedCommander(FortCommanderInstance instance)
	{
		if ((_commanders != null) && (getFort() != null) && (_commanders.size() != 0))
		{
			final Spawner spawn = instance.getSpawn();
			if (spawn != null)
			{
				final List<SpawnFortSiege> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getId());
				for (final SpawnFortSiege spawn2 : commanders)
				{
					if (spawn2.getId() == spawn.getId())
					{
						NpcStringId npcString = null;
						switch (spawn2.getId())
						{
							case 1 :
								npcString = NpcStringId.YOU_MAY_HAVE_BROKEN_OUR_ARROWS_BUT_YOU_WILL_NEVER_BREAK_OUR_WILL_ARCHERS_RETREAT;
								break;
							case 2 :
								npcString = NpcStringId.AIIEEEE_COMMAND_CENTER_THIS_IS_GUARD_UNIT_WE_NEED_BACKUP_RIGHT_AWAY;
								break;
							case 3 :
								npcString = NpcStringId.AT_LAST_THE_MAGIC_FIELD_THAT_PROTECTS_THE_FORTRESS_HAS_WEAKENED_VOLUNTEERS_STAND_BACK;
								break;
							case 4 :
								npcString = NpcStringId.I_FEEL_SO_MUCH_GRIEF_THAT_I_CANT_EVEN_TAKE_CARE_OF_MYSELF_THERE_ISNT_ANY_REASON_FOR_ME_TO_STAY_HERE_ANY_LONGER;
								break;
						}
						if (npcString != null)
						{
							instance.broadcastPacket(new NpcSay(instance.getObjectId(), Say2.NPC_SHOUT, instance.getId(), npcString));
						}
					}
				}
				_commanders.remove(spawn);
				if (_commanders.isEmpty())
				{
					checkCommanders();
				}
				else if (_siegeRestore == null)
				{
					getFort().getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.SEIZED_BARRACKS));
					_siegeRestore = ThreadPoolManager.getInstance().schedule(new ScheduleSiegeRestore(), FortSiegeManager.getInstance().getCountDownLength() * 60 * 1000L);
				}
				else
				{
					getFort().getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.SEIZED_BARRACKS));
				}
			}
			else
			{
				_log.warn("FortSiege.killedCommander(): killed commander, but commander not registered for fortress. NpcId: " + instance.getId() + " FortId: " + getFort().getId());
			}
		}
	}
	
	public void killedControlUnit(BackupPowerUnitInstance instance)
	{
		if ((_controlUnits != null) && (getFort() != null) && (_controlUnits.size() != 0))
		{
			final Spawner spawn = instance.getSpawn();
			if (spawn != null)
			{
				_controlUnits.remove(spawn);
			}
		}
	}
	
	public void killedPowerUnit(PowerControlUnitInstance instance)
	{
		if ((_powerUnits != null) && (getFort() != null) && (_powerUnits.size() != 0))
		{
			final Spawner spawn = instance.getSpawn();
			if (spawn != null)
			{
				_powerUnits.remove(spawn);
			}
		}
	}
	
	public void checkCommanders()
	{
		if (_commanders.isEmpty() && _isControlDisabled)
		{
			spawnFlag(getFort().getId());
			
			if (_siegeRestore != null)
			{
				_siegeRestore.cancel(true);
			}
			
			for (final DoorInstance door : getFort().getDoors())
			{
				if (door.getIsShowHp())
				{
					continue;
				}
				
				door.openMe();
			}
			getFort().getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.ALL_BARRACKS_OCCUPIED));
		}
	}

	public void killedFlag(Npc flag)
	{
		if (flag == null)
		{
			return;
		}

		for (final SiegeClan clan : getAttackerClans())
		{
			if (clan.removeFlag(flag))
			{
				return;
			}
		}
	}

	public boolean registerAttacker(Player player, boolean force)
	{
		if (player.getClan() == null)
		{
			return false;
		}

		if (force || checkIfCanRegister(player))
		{
			saveSiegeClan(player.getClan());

			if (getAttackerClans().size() == 1)
			{
				if (!force)
				{
					player.reduceAdena("siege", 250000, null, true);
				}
				startAutoTask(true);
			}
			return true;
		}
		return false;
	}

	private void removeSiegeClan(int clanId)
	{
		final String query = (clanId != 0) ? DELETE_FORT_SIEGECLANS_BY_CLAN_ID : DELETE_FORT_SIEGECLANS;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			statement.setInt(1, getFort().getId());
			if (clanId != 0)
			{
				statement.setInt(2, clanId);
			}
			statement.execute();

			loadSiegeClan();
			if (getAttackerClans().isEmpty())
			{
				if (getIsInProgress())
				{
					endSiege();
				}
				else
				{
					saveFortSiege();
				}

				if (_siegeStartTask != null)
				{
					_siegeStartTask.cancel(true);
					_siegeStartTask = null;
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception on removeSiegeClan: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void removeSiegeClan(Clan clan)
	{
		if ((clan == null) || (clan.getFortId() == getFort().getId()) || !FortSiegeManager.getInstance().checkIsRegistered(clan, getFort().getId()))
		{
			return;
		}

		removeSiegeClan(clan.getId());
	}

	public void checkAutoTask()
	{
		if (_siegeStartTask != null)
		{
			return;
		}

		final long delay = getFort().getSiegeDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

		if (delay < 0)
		{
			saveFortSiege();
			clearSiegeClan();
			ThreadPoolManager.getInstance().execute(new ScheduleSuspiciousMerchantSpawn());
		}
		else
		{
			loadSiegeClan();
			if (getAttackerClans().isEmpty())
			{
				ThreadPoolManager.getInstance().schedule(new ScheduleSuspiciousMerchantSpawn(), delay);
			}
			else
			{
				if (delay > 3600000)
				{
					ThreadPoolManager.getInstance().execute(new ScheduleSuspiciousMerchantSpawn());
					_siegeStartTask = ThreadPoolManager.getInstance().schedule(new FortSiege.ScheduleStartSiegeTask(3600), delay - 3600000);
				}
				if (delay > 600000)
				{
					ThreadPoolManager.getInstance().execute(new ScheduleSuspiciousMerchantSpawn());
					_siegeStartTask = ThreadPoolManager.getInstance().schedule(new FortSiege.ScheduleStartSiegeTask(600), delay - 600000);
				}
				else if (delay > 300000)
				{
					_siegeStartTask = ThreadPoolManager.getInstance().schedule(new FortSiege.ScheduleStartSiegeTask(300), delay - 300000);
				}
				else if (delay > 60000)
				{
					_siegeStartTask = ThreadPoolManager.getInstance().schedule(new FortSiege.ScheduleStartSiegeTask(60), delay - 60000);
				}
				else
				{
					_siegeStartTask = ThreadPoolManager.getInstance().schedule(new FortSiege.ScheduleStartSiegeTask(60), 0);
				}

				_log.info("Siege of " + getFort().getName() + " fort: " + getFort().getSiegeDate().getTime());
			}
		}
	}

	public void startAutoTask(boolean setTime)
	{
		if (_siegeStartTask != null)
		{
			return;
		}

		if (setTime)
		{
			setSiegeDateTime(false);
		}

		if (getFort().getOwnerClan() != null)
		{
			getFort().getOwnerClan().broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.A_FORTRESS_IS_UNDER_ATTACK));
		}

		_siegeStartTask = ThreadPoolManager.getInstance().schedule(new FortSiege.ScheduleStartSiegeTask(3600), 0);
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
			default :
				players = getPlayersInZone();
		}

		for (final Player player : players)
		{
			if (player.canOverrideCond(PcCondOverride.FORTRESS_CONDITIONS) || player.isJailed())
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

	public boolean checkIfCanRegister(Player player)
	{
		boolean b = true;
		if ((player.getClan() == null) || (player.getClan().getLevel() < FortSiegeManager.getInstance().getSiegeClanMinLevel()))
		{
			b = false;
			player.sendMessage("Only clans with Level " + FortSiegeManager.getInstance().getSiegeClanMinLevel() + " and higher may register for a fortress siege.");
		}
		else if ((player.getClanPrivileges() & Clan.CP_CS_MANAGE_SIEGE) != Clan.CP_CS_MANAGE_SIEGE)
		{
			b = false;
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
		}
		else if (player.getClan() == getFort().getOwnerClan())
		{
			b = false;
			player.sendPacket(SystemMessageId.CLAN_THAT_OWNS_CASTLE_IS_AUTOMATICALLY_REGISTERED_DEFENDING);
		}
		else if ((getFort().getOwnerClan() != null) && (player.getClan().getCastleId() > 0) && (player.getClan().getCastleId() == getFort().getContractedCastleId()))
		{
			b = false;
			player.sendPacket(SystemMessageId.CANT_REGISTER_TO_SIEGE_DUE_TO_CONTRACT);
		}
		else if ((getFort().getTimeTillRebelArmy() > 0) && (getFort().getTimeTillRebelArmy() <= 7200))
		{
			b = false;
			player.sendMessage("You cannot register for the fortress siege 2 hours prior to rebel army attack.");
		}
		else if (getFort().getSiege().getAttackerClans().isEmpty() && (player.getInventory().getAdena() < 250000))
		{
			b = false;
			player.sendMessage("You need 250,000 adena to register");
		}
		else if ((System.currentTimeMillis() < TerritoryWarManager.getInstance().getTWStartTimeInMillis()) && TerritoryWarManager.getInstance().getIsRegistrationOver())
		{
			b = false;
			player.sendMessage("This is not a good time. You cannot register.");
		}
		else if ((System.currentTimeMillis() > TerritoryWarManager.getInstance().getTWStartTimeInMillis()) && TerritoryWarManager.getInstance().isTWChannelOpen())
		{
			b = false;
			player.sendMessage("This is not a good time. You cannot register.");
		}
		else
		{
			for (final Fort fort : FortManager.getInstance().getForts())
			{
				if (fort.getSiege().getAttackerClan(player.getClanId()) != null)
				{
					b = false;
					player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
					break;
				}
				if ((fort.getOwnerClan() == player.getClan()) && (fort.getSiege().getIsInProgress() || (fort.getSiege()._siegeStartTask != null)))
				{
					b = false;
					player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
					break;
				}
			}
			
			for (final Castle castle : CastleManager.getInstance().getCastles())
			{
				if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), castle.getId()) && castle.getSiege().getIsInProgress())
				{
					b = false;
					player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
					break;
				}
			}
		}
		return b;
	}

	public boolean checkIfAlreadyRegisteredForSameDay(Clan clan)
	{
		final Calendar fortSiege = Calendar.getInstance();
		fortSiege.setTimeInMillis(getSiegeStartTime());
		for (final FortSiege siege : FortSiegeManager.getInstance().getSieges())
		{
			if (siege == this)
			{
				continue;
			}

			final Calendar anySiege = Calendar.getInstance();
			anySiege.setTimeInMillis(siege.getSiegeStartTime());
			
			if (anySiege.get(Calendar.DAY_OF_WEEK) == fortSiege.get(Calendar.DAY_OF_WEEK))
			{
				if (siege.checkIsAttacker(clan))
				{
					return true;
				}
				if (siege.checkIsDefender(clan))
				{
					return true;
				}
			}
		}

		return false;
	}

	private void setSiegeDateTime(boolean merchant)
	{
		final Calendar newDate = Calendar.getInstance();
		if (merchant)
		{
			newDate.add(Calendar.MINUTE, FortSiegeManager.getInstance().getSuspiciousMerchantRespawnDelay());
		}
		else
		{
			newDate.add(Calendar.MINUTE, 60);
		}
		getFort().setSiegeDate(newDate);
		saveSiegeDate();
	}

	private void loadSiegeClan()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			
			getAttackerClans().clear();
			statement = con.prepareStatement("SELECT clan_id FROM fortsiege_clans WHERE fort_id=?");
			statement.setInt(1, getFort().getId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				addAttacker(rs.getInt("clan_id"));
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
	}

	private void removePowerUnits()
	{
		if ((_powerUnits != null) && !_powerUnits.isEmpty())
		{
			for (final Spawner spawn : _powerUnits)
			{
				if (spawn != null)
				{
					spawn.stopRespawn();
					if (spawn.getLastSpawn() != null)
					{
						spawn.getLastSpawn().deleteMe();
					}
				}
			}
			_powerUnits.clear();
		}
	}
	
	private void removeControlUnits()
	{
		if ((_controlUnits != null) && !_controlUnits.isEmpty())
		{
			for (final Spawner spawn : _controlUnits)
			{
				if (spawn != null)
				{
					spawn.stopRespawn();
					if (spawn.getLastSpawn() != null)
					{
						spawn.getLastSpawn().deleteMe();
					}
				}
			}
			_controlUnits.clear();
		}
	}
	
	private void removeMainMachine()
	{
		if ((_mainMachines != null) && !_mainMachines.isEmpty())
		{
			for (final Spawner spawn : _mainMachines)
			{
				if (spawn != null)
				{
					spawn.stopRespawn();
					if (spawn.getLastSpawn() != null)
					{
						spawn.getLastSpawn().deleteMe();
					}
				}
			}
			_mainMachines.clear();
		}
	}
	
	private void removeCommanders()
	{
		if ((_commanders != null) && !_commanders.isEmpty())
		{
			for (final Spawner spawn : _commanders)
			{
				if (spawn != null)
				{
					spawn.stopRespawn();
					if (spawn.getLastSpawn() != null)
					{
						spawn.getLastSpawn().deleteMe();
					}
				}
			}
			_commanders.clear();
		}
	}

	private void removeFlags()
	{
		for (final SiegeClan sc : getAttackerClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}

	private void saveFortSiege()
	{
		clearSiegeDate();
		saveSiegeDate();
	}

	private void saveSiegeDate()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE fort SET siegeDate = ? WHERE id = ?");
			statement.setLong(1, getSiegeStartTime());
			statement.setInt(2, getFort().getId());
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

	private void saveSiegeClan(Clan clan)
	{
		if (getAttackerClans().size() >= FortSiegeManager.getInstance().getAttackerMaxClans())
		{
			return;
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO fortsiege_clans (clan_id,fort_id) values (?,?)");
			statement.setInt(1, clan.getId());
			statement.setInt(2, getFort().getId());
			statement.execute();

			addAttacker(clan.getId());
		}
		catch (final Exception e)
		{
			_log.warn("Exception: saveSiegeClan(Clan clan): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void spawnCommanders()
	{
		try
		{
			_commanders.clear();
			Spawner spawnDat;
			NpcTemplate template1;
			for (final SpawnFortSiege _sp : FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getId()))
			{
				template1 = NpcsParser.getInstance().getTemplate(_sp.getId());
				if (template1 != null)
				{
					spawnDat = new Spawner(template1);
					spawnDat.setAmount(1);
					spawnDat.setX(_sp.getLocation().getX());
					spawnDat.setY(_sp.getLocation().getY());
					spawnDat.setZ(_sp.getLocation().getZ());
					spawnDat.setHeading(_sp.getLocation().getHeading());
					spawnDat.setRespawnDelay(60);
					spawnDat.doSpawn();
					spawnDat.stopRespawn();
					_commanders.add(spawnDat);
				}
				else
				{
					_log.warn("FortSiege.spawnCommander: Data missing in NPC table for ID: " + _sp.getId() + ".");
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("FortSiege.spawnCommander: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	private void spawnControlUnits()
	{
		if (FortSiegeManager.getInstance().getControlUnitSpawnList(getFort().getId()) == null || FortSiegeManager.getInstance().getControlUnitSpawnList(getFort().getId()).isEmpty())
		{
			return;
		}
		
		try
		{
			_controlUnits.clear();
			Spawner spawnDat;
			NpcTemplate template1;
			for (final SpawnFortSiege _sp : FortSiegeManager.getInstance().getControlUnitSpawnList(getFort().getId()))
			{
				template1 = NpcsParser.getInstance().getTemplate(_sp.getId());
				if (template1 != null)
				{
					spawnDat = new Spawner(template1);
					spawnDat.setAmount(1);
					spawnDat.setX(_sp.getLocation().getX());
					spawnDat.setY(_sp.getLocation().getY());
					spawnDat.setZ(_sp.getLocation().getZ());
					spawnDat.setHeading(_sp.getLocation().getHeading());
					spawnDat.setRespawnDelay(60);
					spawnDat.doSpawn();
					spawnDat.stopRespawn();
					_controlUnits.add(spawnDat);
				}
				else
				{
					_log.warn("FortSiege.spawnControlUnit: Data missing in NPC table for ID: " + _sp.getId() + ".");
				}
			}
			_isControlDisabled = false;
			_isControlDoorsOpen = false;
		}
		catch (final Exception e)
		{
			_log.warn("FortSiege.spawnControlUnit: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	public void spawnPowerUnits()
	{
		if (FortSiegeManager.getInstance().getPowerUnitSpawnList(getFort().getId()) == null || FortSiegeManager.getInstance().getPowerUnitSpawnList(getFort().getId()).isEmpty())
		{
			return;
		}
		
		try
		{
			_powerUnits.clear();
			Spawner spawnDat;
			NpcTemplate template1;
			for (final SpawnFortSiege _sp : FortSiegeManager.getInstance().getPowerUnitSpawnList(getFort().getId()))
			{
				template1 = NpcsParser.getInstance().getTemplate(_sp.getId());
				if (template1 != null)
				{
					spawnDat = new Spawner(template1);
					spawnDat.setAmount(1);
					spawnDat.setX(_sp.getLocation().getX());
					spawnDat.setY(_sp.getLocation().getY());
					spawnDat.setZ(_sp.getLocation().getZ());
					spawnDat.setHeading(_sp.getLocation().getHeading());
					spawnDat.setRespawnDelay(60);
					spawnDat.doSpawn();
					spawnDat.stopRespawn();
					_powerUnits.add(spawnDat);
				}
				else
				{
					_log.warn("FortSiege.spawnPowerUnit: Data missing in NPC table for ID: " + _sp.getId() + ".");
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("FortSiege.spawnPowerUnit: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	public void spawnMainMachine()
	{
		if (FortSiegeManager.getInstance().getMainMachineSpawnList(getFort().getId()) == null || FortSiegeManager.getInstance().getMainMachineSpawnList(getFort().getId()).isEmpty())
		{
			return;
		}
		
		try
		{
			_mainMachines.clear();
			Spawner spawnDat;
			NpcTemplate template1;
			for (final SpawnFortSiege _sp : FortSiegeManager.getInstance().getMainMachineSpawnList(getFort().getId()))
			{
				template1 = NpcsParser.getInstance().getTemplate(_sp.getId());
				if (template1 != null)
				{
					spawnDat = new Spawner(template1);
					spawnDat.setAmount(1);
					spawnDat.setX(_sp.getLocation().getX());
					spawnDat.setY(_sp.getLocation().getY());
					spawnDat.setZ(_sp.getLocation().getZ());
					spawnDat.setHeading(_sp.getLocation().getHeading());
					spawnDat.setRespawnDelay(60);
					spawnDat.doSpawn();
					spawnDat.stopRespawn();
					_mainMachines.add(spawnDat);
				}
				else
				{
					_log.warn("FortSiege.spawnMainMachine: Data missing in NPC table for ID: " + _sp.getId() + ".");
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("FortSiege.spawnMainMachine: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}

	private void spawnFlag(int Id)
	{
		for (final CombatFlag cf : FortSiegeManager.getInstance().getFlagList(Id))
		{
			cf.spawnMe();
		}
	}

	private void unSpawnFlags()
	{
		if (FortSiegeManager.getInstance().getFlagList(getFort().getId()) == null)
		{
			return;
		}

		for (final CombatFlag cf : FortSiegeManager.getInstance().getFlagList(getFort().getId()))
		{
			cf.unSpawnMe();
		}
	}

	private void spawnSiegeGuard()
	{
		getSiegeGuardManager().spawnSiegeGuard();
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
		for (final SiegeClan sc : getAttackerClans())
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
		return _attackerClans;
	}

	public final Fort getFort()
	{
		return _fort;
	}

	public final boolean getIsInProgress()
	{
		return _isInProgress;
	}

	@Override
	public final long getSiegeStartTime()
	{
		return getFort().getSiegeDate().getTimeInMillis();
	}

	@Override
	public List<Npc> getFlag(Clan clan)
	{
		if (clan != null)
		{
			final SiegeClan sc = getAttackerClan(clan);
			if (sc != null)
			{
				return sc.getFlag();
			}
		}

		return null;
	}

	public final FortSiegeGuardManager getSiegeGuardManager()
	{
		if (_siegeGuardManager == null)
		{
			_siegeGuardManager = new FortSiegeGuardManager(getFort());
		}

		return _siegeGuardManager;
	}

	public void resetSiege()
	{
		removeCommanders();
		removePowerUnits();
		removeControlUnits();
		removeMainMachine();
		_isControlDisabled = true;
		_isControlDoorsOpen = false;
		spawnCommanders();
		spawnControlUnits();
		getFort().resetDoors();
	}

	public List<Spawner> getCommanders()
	{
		return _commanders;
	}
	
	public List<Spawner> getControlUnits()
	{
		return _controlUnits;
	}
	
	public List<Spawner> getPowerUnits()
	{
		return _powerUnits;
	}
	
	public List<Spawner> getMainMachine()
	{
		return _mainMachines;
	}

	public void disablePower(boolean disable)
	{
		_isControlDisabled = disable;
	}
	
	public void setOpenControlDoors(boolean open)
	{
		_isControlDoorsOpen = open;
	}
	
	public boolean isControlDoorsOpen()
	{
		return _isControlDoorsOpen;
	}
	
	public void openControlDoors(int id)
	{
		switch (id)
		{
			case 102 :
				DoorParser.getInstance().getDoor(19240003).openMe();
				DoorParser.getInstance().getDoor(19240004).openMe();
				break;
			case 104 :
				DoorParser.getInstance().getDoor(23210002).openMe();
				DoorParser.getInstance().getDoor(23210003).openMe();
				break;
			case 107 :
				DoorParser.getInstance().getDoor(25190002).openMe();
				DoorParser.getInstance().getDoor(25190003).openMe();
				break;
			case 109 :
				DoorParser.getInstance().getDoor(24150009).openMe();
				DoorParser.getInstance().getDoor(24150010).openMe();
				break;
			case 110 :
				DoorParser.getInstance().getDoor(22160002).openMe();
				DoorParser.getInstance().getDoor(22160003).openMe();
				break;
			case 112 :
				DoorParser.getInstance().getDoor(20220017).openMe();
				DoorParser.getInstance().getDoor(20220018).openMe();
				break;
			case 113 :
				DoorParser.getInstance().getDoor(18200004).openMe();
				DoorParser.getInstance().getDoor(18200005).openMe();
				break;
			case 116 :
				DoorParser.getInstance().getDoor(22200010).openMe();
				DoorParser.getInstance().getDoor(22200011).openMe();
				break;
			case 117 :
				DoorParser.getInstance().getDoor(23170006).openMe();
				DoorParser.getInstance().getDoor(23170007).openMe();
				break;
			case 118 :
				DoorParser.getInstance().getDoor(23200004).openMe();
				DoorParser.getInstance().getDoor(23200005).openMe();
				break;
			default :
				break;
		}
		_isControlDoorsOpen = true;
	}
	
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		return null;
	}

	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		return null;
	}

	@Override
	public List<SiegeClan> getDefenderClans()
	{
		return null;
	}

	@Override
	public boolean giveFame()
	{
		return true;
	}

	@Override
	public int getFameFrequency()
	{
		return Config.FORTRESS_ZONE_FAME_TASK_FREQUENCY;
	}

	@Override
	public int getFameAmount()
	{
		return Config.FORTRESS_ZONE_FAME_AQUIRE_POINTS;
	}

	@Override
	public void updateSiege()
	{
	}
	
	public <T extends Listener<FortSiege>> boolean addListener(T listener)
	{
		return _listeners.add(listener);
	}
	
	public <T extends Listener<FortSiege>> boolean removeListener(T listener)
	{
		return _listeners.remove(listener);
	}
}