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
package l2e.gameserver.instancemanager.games.krateiscube.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import l2e.commons.util.NpcUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.games.krateiscube.KrateisCubeManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.KrateisMatchManagerInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.ExPVPMatchCCMyRecord;
import l2e.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import l2e.gameserver.network.serverpackets.ExPVPMatchCCRetire;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Created by LordWinter
 */
public class Arena
{
	private final int _id;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _minPlayers;
	private final int _maxPlayers;
	private KrateisMatchManagerInstance _manager = null;
	private final Map<Player, KrateiCubePlayer> _players = new ConcurrentHashMap<>();
	private final List<Skill> _buffs = new ArrayList<>();
	private final List<DoorInstance> _doorListA = new ArrayList<>();
	private final List<DoorInstance> _doorListB = new ArrayList<>();
	private final List<Location> _waitLoc = new ArrayList<>();
	private final List<Location> _battleLoc = new ArrayList<>();
	private final List<Location> _watcherLoc = new ArrayList<>();
	private final List<String> _spawnGroups = new ArrayList<>();
	private final List<KrateisReward> _rewards = new ArrayList<>();
	private final List<Npc> _watchers = new ArrayList<>();
	
	private StatsSet _params;
	
	private ScheduledFuture<?> _eventTask = null;
	private ScheduledFuture<?> _timeTask = null;
	private ScheduledFuture<?> _watcherTask = null;
	private ScheduledFuture<?> _doorTask = null;
	
	private boolean _doorsRotation = false;
	private boolean _watcherRotation = false;
	private boolean _isBattleNow = false;
	private boolean _isActiveNow = false;

	public Arena(int id, int manager, int minLevel, int maxLevel, int minPlayers, int maxPlayers)
	{
		_id = id;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_minPlayers = minPlayers;
		_maxPlayers = maxPlayers;
		_rewards.clear();
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn.getLastSpawn() != null && spawn.getId() == manager)
			{
				_manager = (KrateisMatchManagerInstance) spawn.getLastSpawn();
				_manager.setArenaId(getId());
			}
		}
	}

	public int getId()
	{
		return _id;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	public int getMinPlayers()
	{
		return _minPlayers;
	}
	
	public int getMaxPlayers()
	{
		return _maxPlayers;
	}
	
	public KrateisMatchManagerInstance getManager()
	{
		return _manager;
	}
	
	public boolean isBattleNow()
	{
		return _isBattleNow;
	}
	
	public void setIsBattleNow(boolean value)
	{
		_isBattleNow = value;
	}
	
	public boolean isActiveNow()
	{
		return _isActiveNow;
	}
	
	public void setReward(KrateisReward ucReward)
	{
		_rewards.add(ucReward);
	}
	
	public List<KrateisReward> getRewards()
	{
		return _rewards;
	}
	
	public void addBuff(Skill skill)
	{
		_buffs.add(skill);
	}
	
	public List<Skill> getBuffList()
	{
		return _buffs;
	}
	
	public void addDoorA(DoorInstance door)
	{
		_doorListA.add(door);
	}
	
	public List<DoorInstance> getDoorListA()
	{
		return _doorListA;
	}
	
	public void addDoorB(DoorInstance door)
	{
		_doorListB.add(door);
	}
	
	public List<DoorInstance> getDoorListB()
	{
		return _doorListB;
	}
	
	public List<Location> getWaitLoc()
	{
		return _waitLoc;
	}
	
	public void addWaitLoc(Location loc)
	{
		_waitLoc.add(loc);
	}
	
	public List<Location> getBattleLoc()
	{
		return _battleLoc;
	}
	
	public void addBattleLoc(Location loc)
	{
		_battleLoc.add(loc);
	}
	
	public List<Location> getWatcherLoc()
	{
		return _watcherLoc;
	}
	
	public void addWatcherLoc(Location loc)
	{
		_watcherLoc.add(loc);
	}
	
	public List<String> getSpawnGroups()
	{
		return _spawnGroups;
	}
	
	public void addSpawnGroup(String spawn)
	{
		_spawnGroups.add(spawn);
	}
	
	public void addParam(StatsSet params)
	{
		_params = params;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
	
	public boolean addRegisterPlayer(Player player)
	{
		if (!_players.containsKey(player))
		{
			final KrateiCubePlayer pl = _players.computeIfAbsent(player, data -> new KrateiCubePlayer(player));
			_players.put(player, pl);
			pl.setIsRegister(true);
			player.setArena(this);
			return true;
		}
		return false;
	}
	
	public boolean removePlayer(Player player)
	{
		if (_players.containsKey(player))
		{
			_players.remove(player);
			player.setArena(null);
			return true;
		}
		return false;
	}
	
	public boolean isRegisterPlayer(Player player)
	{
		return _players.containsKey(player);
	}
	
	public Map<Player, KrateiCubePlayer> getPlayers()
	{
		return _players;
	}
	
	public void startEvent()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
		}
		
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		
		if (_watcherTask != null)
		{
			_watcherTask.cancel(false);
		}
		
		if (_doorTask != null)
		{
			_doorTask.cancel(false);
		}
		
		_doorsRotation = true;
		changeDoorStatus(true);
		if (!getSpawnGroups().isEmpty())
		{
			for (final String spawn : getSpawnGroups())
			{
				if (spawn != null)
				{
					SpawnParser.getInstance().spawnGroup(spawn);
				}
			}
		}
		
		if (!getWatcherLoc().isEmpty())
		{
			for (final Location loc : getWatcherLoc())
			{
				if (loc != null)
				{
					final Npc npc = NpcUtils.spawnSingle(18601, loc);
					npc.setIsImmobilized(true);
					npc.setArena(this);
					_watchers.add(npc);
				}
			}
			_watcherRotation = true;
		}
		teleportAllPlayers();
		final long time = 1200000;
		_eventTask = ThreadPoolManager.getInstance().schedule(() -> endEvent(), time);
		_timeTask = ThreadPoolManager.getInstance().schedule(() -> timeLeftInfo(), time - 11000);
		_watcherTask = ThreadPoolManager.getInstance().schedule(() -> changeWatchers(), getParams().getInteger("watcherRotationTime") * 1000);
		_isActiveNow = true;
	}
	
	public void endEvent()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(true);
			_eventTask = null;
		}
		
		if (_watcherTask != null)
		{
			_watcherTask.cancel(true);
			_watcherTask = null;
		}
		
		if (_doorTask != null)
		{
			_doorTask.cancel(true);
			_doorTask = null;
		}
		_watcherRotation = false;
		changeDoorStatus(false);
		
		if (!getSpawnGroups().isEmpty())
		{
			for (final String spawn : getSpawnGroups())
			{
				if (spawn != null)
				{
					SpawnParser.getInstance().despawnGroup(spawn);
				}
			}
		}
		
		if (!_watchers.isEmpty())
		{
			for (final Npc npc : _watchers)
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
			}
			_watchers.clear();
		}
		
		final Set<Player> participants = getPlayers().keySet();
		if (participants != null && !participants.isEmpty())
		{
			for (final Player player : participants)
			{
				if (player != null)
				{
					removeAllEffects(player);
					player.teleToLocation(-70381, -70937, -1428, 0, true, player.getReflection());
					player.setArena(null);
					player.sendPacket(ExPVPMatchCCRetire.STATIC);
					player.setCanRevive(true);
				}
			}
			checkRewardPlayers(getPlayers());
		}
		_players.clear();
		_isBattleNow = false;
		_isActiveNow = false;
		KrateisCubeManager.getInstance().setIsActivate(false);
	}
	
	private void changeWatchers()
	{
		if (_watcherTask != null)
		{
			_watcherTask.cancel(true);
			_watcherTask = null;
		}
		
		if (!_watchers.isEmpty())
		{
			for (final Npc npc : _watchers)
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
			}
			_watchers.clear();
		}
		
		if (!getWatcherLoc().isEmpty())
		{
			for (final Location loc : getWatcherLoc())
			{
				if (loc != null)
				{
					final Npc npc = NpcUtils.spawnSingle(_watcherRotation ? 18602 : 18601, loc);
					npc.setIsImmobilized(true);
					npc.setArena(this);
					_watchers.add(npc);
				}
			}
		}
		
		if (_watcherRotation)
		{
			_watcherRotation = false;
		}
		else
		{
			_watcherRotation = true;
		}
		_watcherTask = ThreadPoolManager.getInstance().schedule(() -> changeWatchers(), getParams().getInteger("watcherRotationTime") * 1000);
	}
	
	public void chaneWatcher(Npc npc)
	{
		if (_watchers.contains(npc))
		{
			npc.deleteMe();
			final Npc newNpc = NpcUtils.spawnSingle(npc.getId() == 18602 ? 18601 : 18602, npc.getLocation());
			newNpc.setIsImmobilized(true);
			newNpc.setArena(this);
			_watchers.add(newNpc);
			_watchers.remove(npc);
		}
	}
	
	private Map<String, Integer> getParticipants()
	{
		final Map<String, Integer> participants = new HashMap<>();
		if (!getPlayers().isEmpty())
		{
			for (final Player player : getPlayers().keySet())
			{
				if (player != null && !participants.containsKey(player.getName(null)))
				{
					participants.put(player.getName(null), getPoints(player));
				}
			}
			
			if (participants.isEmpty())
			{
				return null;
			}
		}
		final Map<String, Integer> sortedMap = participants.entrySet().stream().sorted(Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		return sortedMap;
	}
	
	private void teleportAllPlayers()
	{
		if (!getPlayers().isEmpty())
		{
			final Map<String, Integer> participants = getParticipants();
			if (participants == null)
			{
				return;
			}
			
			final ExPVPMatchCCRecord p = new ExPVPMatchCCRecord(participants, 1);
			for (final Player player : getPlayers().keySet())
			{
				if (player != null)
				{
					player.teleToLocation(getBattleLoc().get(Rnd.get(getBattleLoc().size())), true, player.getReflection());
					addEffects(player);
					player.sendPacket(new ExPVPMatchCCMyRecord(getPoints(player)));
					player.sendPacket(p);
					player.setCanRevive(false);
				}
			}
		}
	}
	
	public void teleportToBattle(Player player)
	{
		if (player != null)
		{
			if (getPlayers().containsKey(player))
			{
				player.teleToLocation(getBattleLoc().get(Rnd.get(getBattleLoc().size())), true, player.getReflection());
			}
		}
		else
		{
			if (!getPlayers().isEmpty())
			{
				for (final Player pl : getPlayers().keySet())
				{
					if (pl != null)
					{
						teleportToWaitingRoom(pl, false);
					}
				}
			}
		}
	}
	
	public void teleportToWaitingRoom(Player player, boolean restoreEffects)
	{
		if (getPlayers().containsKey(player))
		{
			if (restoreEffects)
			{
				respawnEffects(player);
			}
			player.teleToLocation(getWaitLoc().get(Rnd.get(getWaitLoc().size())), true, player.getReflection());
		}
	}
	
	private void changeDoorStatus(boolean active)
	{
		if (_doorTask != null)
		{
			_doorTask.cancel(false);
		}
		
		if (_doorsRotation)
		{
			_doorsRotation = false;
		}
		else
		{
			_doorsRotation = true;
		}
		
		if (!getDoorListA().isEmpty() && !getDoorListB().isEmpty())
		{
			final List<DoorInstance> doorsA = (_doorsRotation) ? getDoorListA() : getDoorListB();
			final List<DoorInstance> doorsB = (_doorsRotation) ? getDoorListB() : getDoorListA();
			if (active)
			{
				changeDoorsStatus(doorsA, true);
				changeDoorsStatus(doorsB, false);
			}
			else
			{
				changeDoorsStatus(doorsA, false);
				changeDoorsStatus(doorsB, false);
			}
		}
		_doorTask = ThreadPoolManager.getInstance().schedule(() -> changeDoorStatus(active), (getParams().getInteger("doorRotationTime") * 1000));
	}
	
	private void changeDoorsStatus(List<DoorInstance> doors, boolean isOpen)
	{
		for (final DoorInstance door : doors)
		{
			if (door != null)
			{
				if (isOpen)
				{
					door.openMe();
				}
				else
				{
					door.closeMe();
				}
			}
		}
	}
	
	public void addEffects(Player player)
	{
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentMp(player.getMaxMp());
		
		if (player.getSummon() != null)
		{
			player.getSummon().setCurrentHp(player.getSummon().getMaxHp());
			player.getSummon().setCurrentCp(player.getSummon().getMaxCp());
			player.getSummon().setCurrentMp(player.getSummon().getMaxMp());
		}
		
		if (!getBuffList().isEmpty())
		{
			for (final Skill skill : getBuffList())
			{
				if (skill != null)
				{
					skill.getEffects(player, player, false);
					if (player.getSummon() != null)
					{
						skill.getEffects(player.getSummon(), player.getSummon(), false);
					}
				}
			}
		}
		
		player.broadcastUserInfo(true);
		
		if (player.getSummon() != null)
		{
			player.getSummon().broadcastInfo();
		}
	}
	
	private void removeAllEffects(Player player)
	{
		player.getEffectList().stopAllEffects();
		player.stopAllEffects();
		if (player.getSummon() != null)
		{
			player.getSummon().stopAllEffects();
		}
		
		player.broadcastUserInfo(true);
		
		if (player.getSummon() != null)
		{
			player.getSummon().broadcastInfo();
		}
	}
	
	private void respawnEffects(Player player)
	{
		removeAllEffects(player);
		
		if (player.isDead())
		{
			player.doRevive(100.);
		}
		player.broadcastStatusUpdate();
		player.broadcastUserInfo(true);
	}
	
	private void checkRewardPlayers(Map<Player, KrateiCubePlayer> players)
	{
		final List<KrateiCubePlayer> playerList = new ArrayList<>();
		for (final KrateiCubePlayer info : players.values())
		{
			if (info != null)
			{
				playerList.add(info);
			}
		}
		
		final Comparator<KrateiCubePlayer> statsComparator = new SortPointInfo();
		Collections.sort(playerList, statsComparator);
		
		ExPVPMatchCCRecord p = null;
		final Map<String, Integer> participants = getParticipants();
		if (participants != null)
		{
			p = new ExPVPMatchCCRecord(participants, 0);
		}
		
		double dif = 0.05;
		int pos = 0;
		for (final KrateiCubePlayer pl : playerList)
		{
			if (pl != null && pl.getPlayer() != null)
			{
				pos++;
				if (p != null)
				{
					pl.getPlayer().sendPacket(p);
				}
				
				if (pl.getPoints() >= getParams().getInteger("minPoints"))
				{
					final int count = (int) (pl.getPoints() * dif * (1.0 + players.size() / pos * 0.04));
					dif -= 0.0016;
					if (count > 0)
					{
						final int exp = count * getParams().getInteger("expModifier");
						final int sp = count * getParams().getInteger("spModifier");
						pl.getPlayer().addExpAndSp(exp, sp);
						if (!getRewards().isEmpty())
						{
							for (final KrateisReward reward : getRewards())
							{
								if (reward != null)
								{
									long amount = reward.isAllowMidifier() ? count : reward.getAmount();
									if (reward.getId() == -100)
									{
										if ((pl.getPlayer().getPcBangPoints() + amount) > Config.MAX_PC_BANG_POINTS)
										{
											amount = Config.MAX_PC_BANG_POINTS - pl.getPlayer().getPcBangPoints();
										}
										final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
										sm.addNumber((int) amount);
										pl.getPlayer().sendPacket(sm);
										pl.getPlayer().setPcBangPoints((int) (pl.getPlayer().getPcBangPoints() + amount));
										pl.getPlayer().sendPacket(new ExPCCafePointInfo(pl.getPlayer().getPcBangPoints(), (int) amount, true, false, 1));
									}
									else if (reward.getId() == -200)
									{
										if (pl.getPlayer().getClan() != null)
										{
											pl.getPlayer().getClan().addReputationScore((int) amount, true);
										}
									}
									else if (reward.getId() == -300)
									{
										pl.getPlayer().setFame((int) (pl.getPlayer().getFame() + amount));
										final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
										sm.addNumber((int) amount);
										pl.getPlayer().sendPacket(sm);
										pl.getPlayer().sendUserInfo();
									}
									else if (reward.getId() > 0)
									{
										pl.getPlayer().addItem("Krateis Cube Reward", reward.getId(), amount, pl.getPlayer(), true);
									}
								}
							}
						}
					}
				}
			}
		}
		playerList.clear();
	}
	
	private class SortPointInfo implements Comparator<KrateiCubePlayer>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(KrateiCubePlayer o1, KrateiCubePlayer o2)
		{
			return Integer.compare(o2.getPoints(), o1.getPoints());
		}
	}
	
	public void addPoints(Player player, boolean isPlayer)
	{
		final KrateiCubePlayer pl = getPlayers().get(player);
		if (pl != null)
		{
			pl.addPoints(pl.getPoints() + (isPlayer ? getParams().getInteger("pointPerPlayer") : getParams().getInteger("pointPerMob")));
			pl.getPlayer().sendPacket(new ExPVPMatchCCMyRecord(pl.getPoints()));
			
			final Map<String, Integer> participants = getParticipants();
			if (participants != null)
			{
				final ExPVPMatchCCRecord p = new ExPVPMatchCCRecord(participants, 1);
				if (!getPlayers().isEmpty())
				{
					for (final Player $player : getPlayers().keySet())
					{
						if ($player != null)
						{
							$player.sendPacket(p);
						}
					}
				}
			}
		}
	}
	
	public int getPoints(Player player)
	{
		int kills = 0;
		final KrateiCubePlayer pl = getPlayers().get(player);
		if (pl != null)
		{
			kills = pl.getPoints();
		}
		return kills;
	}
	
	private void timeLeftInfo()
	{
		if (_timeTask != null)
		{
			_timeTask.cancel(true);
			_timeTask = null;
		}
		
		if (!getPlayers().isEmpty())
		{
			ThreadPoolManager.getInstance().schedule(new MessageTask(getPlayers(), 10, 1), 0);
		}
	}
	
	public void waitTimeInfo()
	{
		if (!getPlayers().isEmpty())
		{
			ThreadPoolManager.getInstance().schedule(new MessageTask(getPlayers(), 10, 0), 0);
		}
	}
	
	public class MessageTask implements Runnable
	{
		private final Map<Player, KrateiCubePlayer> _players;
		private int _time;
		private final int _type;
		
		public MessageTask(Map<Player, KrateiCubePlayer> players, int time, int type)
		{
			_players = players;
			_time = time;
			_type = type;
		}
		
		@Override
		public void run()
		{
			if ((_players != null && !_players.isEmpty()) && (_time > 0))
			{
				SystemMessage msg = null;
				switch (_type)
				{
					case 0 :
						msg = SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S).addInt(_time);
						break;
					case 1 :
						msg = SystemMessage.getSystemMessage(SystemMessageId.GAME_ENDS_IN_S1_SECONDS).addInt(_time);
						break;
				}
				
				for (final Player player : _players.keySet())
				{
					if (player != null && msg != null)
					{
						player.sendPacket(msg);
					}
				}
				
				_time--;
				if (_time > 0)
				{
					ThreadPoolManager.getInstance().schedule(new MessageTask(_players, _time, _type), 1000);
				}
			}
		}
	}
	
	public void setRespawnTask(Player player)
	{
		if (player != null)
		{
			ThreadPoolManager.getInstance().schedule(new RespawnTask(player, getParams().getInteger("respawnTime")), 0);
		}
	}
	
	private class RespawnTask implements Runnable
	{
		private final Player _player;
		private int _time;
		
		public RespawnTask(Player player, int time)
		{
			_player = player;
			_time = time;
		}
		
		@Override
		public void run()
		{
			if ((_player != null) && (_time > 0))
			{
				_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESURRECTION_WILL_TAKE_PLACE_IN_THE_WAITING_ROOM_AFTER_S1_SECONDS).addInt(_time));
				_time--;
				if (_time > 0)
				{
					ThreadPoolManager.getInstance().schedule(new RespawnTask(_player, _time), 1000);
				}
				else
				{
					respawnEffects(_player);
					_player.teleToLocation(getWaitLoc().get(Rnd.get(getWaitLoc().size())), true, _player.getReflection());
				}
			}
		}
	}
}