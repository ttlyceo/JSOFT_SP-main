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
package l2e.gameserver.model.entity.underground_coliseum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.ExPVPMatchRecord;
import l2e.gameserver.network.serverpackets.ExPVPMatchUserDie;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class UCArena
{
	private static final long MINUTES_IN_MILISECONDS = 10 * 60000;
	
	private final int _id;
	private final int _minLevel;
	private final int _maxLevel;

	private final UCPoint[] _points = new UCPoint[4];
	private final UCTeam[] _teams = new UCTeam[2];
	private Npc _manager = null;

	private ScheduledFuture<?> _taskFuture = null;
	private final List<UCWaiting> _waitingPartys = new CopyOnWriteArrayList<>();
	private final List<UCReward> _rewards = new ArrayList<>();
	private boolean _isBattleNow = false;

	public UCArena(int id, int curator, int min_level, int max_level)
	{
		_id = id;
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn.getId() == curator)
			{
				_manager = spawn.getLastSpawn();
			}
		}
		_minLevel = min_level;
		_maxLevel = max_level;
		_rewards.clear();
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
	
	public Npc getManager()
	{
		return _manager;
	}

	public void setUCPoint(int index, UCPoint point)
	{
		if (index > 4)
		{
			return;
		}
		_points[index] = point;
	}

	public void setUCTeam(int index, UCTeam team)
	{
		if (index > 2)
		{
			return;
		}
		_teams[index] = team;
	}

	public UCTeam[] getTeams()
	{
		return _teams;
	}

	public UCPoint[] getPoints()
	{
		return _points;
	}

	public List<UCWaiting> getWaitingList()
	{
		return _waitingPartys;
	}

	public void switchStatus(boolean start)
	{
		if ((_taskFuture == null) && start)
		{
			runNewTask(false);
		}
		else
		{
			if (_taskFuture != null)
			{
				_taskFuture.cancel(true);
				_taskFuture = null;
			}
			generateWinner();
			removeTeams();
			for (final UCTeam team : getTeams())
			{
				team.cleanUp();
			}
			
			for (final UCPoint point : getPoints())
			{
				point.actionDoors(false);
				point.getPlayers().clear();
			}
			_isBattleNow = false;
		}
	}

	public void runNewTask(boolean isFullTime)
	{
		final long time = isFullTime ? MINUTES_IN_MILISECONDS : MINUTES_IN_MILISECONDS - 60000L;
		_taskFuture = ThreadPoolManager.getInstance().schedule(new UCRunningTask(this), time);
	}

	public void runTaskNow()
	{
		if (_taskFuture != null)
		{
			_taskFuture.cancel(true);
			_taskFuture = null;
		}
		_taskFuture = ThreadPoolManager.getInstance().schedule(new UCRunningTask(this), 0);
	}

	public void generateWinner()
	{
		final UCTeam blueTeam = _teams[0];
		final UCTeam redTeam = _teams[1];
		UCTeam winnerTeam = null;

		if ((blueTeam.getStatus() == UCTeam.WIN) || (redTeam.getStatus() == UCTeam.WIN))
		{
			winnerTeam = blueTeam.getStatus() == UCTeam.WIN ? blueTeam : redTeam;
		}
		else
		{
			if (blueTeam.getParty() == null && redTeam.getParty() != null)
			{
				redTeam.setStatus(UCTeam.WIN);
				winnerTeam = redTeam;
			}
			else if (redTeam.getParty() == null && blueTeam.getParty() != null)
			{
				blueTeam.setStatus(UCTeam.WIN);
				winnerTeam = blueTeam;
			}
			else if (redTeam.getParty() != null && blueTeam.getParty() != null)
			{
				if (blueTeam.getKillCount() > redTeam.getKillCount())
				{
					blueTeam.setStatus(UCTeam.WIN);
					redTeam.setStatus(UCTeam.FAIL);
					winnerTeam = blueTeam;
				}
				else if (redTeam.getKillCount() > blueTeam.getKillCount())
				{
					blueTeam.setStatus(UCTeam.FAIL);
					redTeam.setStatus(UCTeam.WIN);
					winnerTeam = redTeam;
				}
				else if (blueTeam.getKillCount() == redTeam.getKillCount())
				{
					if (blueTeam.getRegisterTime() > redTeam.getRegisterTime())
					{
						blueTeam.setStatus(UCTeam.FAIL);
						redTeam.setStatus(UCTeam.WIN);
						winnerTeam = redTeam;
					}
					else
					{
						blueTeam.setStatus(UCTeam.WIN);
						redTeam.setStatus(UCTeam.FAIL);
						winnerTeam = blueTeam;
					}
				}
			}
		}

		if (winnerTeam != null)
		{
			broadcastRecord(ExPVPMatchRecord.FINISH, (winnerTeam.getIndex() + 1));
		}
		else
		{
			broadcastRecord(ExPVPMatchRecord.FINISH, 0);
		}
		blueTeam.setLastParty(redTeam.getParty());
		redTeam.setLastParty(blueTeam.getParty());
	}

	public void broadcastToAll(GameServerPacket packet)
	{
		for (final UCTeam team : getTeams())
		{
			final Party party = team.getParty();
			if (party != null)
			{
				for (final Player member : party.getMembers())
				{
					if (member != null)
					{
						member.sendPacket(packet);
					}
				}
			}
		}
	}
	
	public void prepareStart()
	{
		_isBattleNow = true;
		broadcastToAll(new ExShowScreenMessage(NpcStringId.MATCH_BEGINS_IN_S1_MINUTES, 2, 5000, "1"));
		try
		{
			Thread.sleep(30000);
		}
		catch (final InterruptedException e)
		{}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.S1_SECONDS_REMAINING, 2, 5000, "30"));
		try
		{
			Thread.sleep(20000);
		}
		catch (final InterruptedException e)
		{}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.S1_SECONDS_REMAINING, 2, 3000, "10"));
		
		try
		{
			Thread.sleep(5000);
		}
		catch (final InterruptedException e)
		{}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.S1_SECONDS_REMAINING, 2, 1000, "5"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (final InterruptedException e)
		{}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.S1_SECONDS_REMAINING, 2, 1000, "4"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (final InterruptedException e)
		{}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.S1_SECONDS_REMAINING, 2, 1000, "3"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (final InterruptedException e)
		{}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.S1_SECONDS_REMAINING, 2, 1000, "2"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (final InterruptedException e)
		{}
		
		broadcastToAll(new ExShowScreenMessage(NpcStringId.S1_SECONDS_REMAINING, 2, 1000, "1"));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (final InterruptedException e)
		{}
		
		boolean isValid = true;
		for (final UCTeam team : _teams)
		{
			if (team.getParty() == null)
			{
				isValid = false;
				continue;
			}
			
			if (team.getParty().getMemberCount() < Config.UC_PARTY_LIMIT)
			{
				isValid = false;
				continue;
			}
			
			for (final Player pl : team.getParty().getMembers())
			{
				if (pl != null)
				{
					if (pl.getDistance(_manager) > 500 || pl.getClassId().level() < 2)
					{
						isValid = false;
						continue;
					}
				}
			}
		}
		
		if (!isValid)
		{
			broadcastToAll(new ExShowScreenMessage(NpcStringId.THE_MATCH_IS_AUTOMATICALLY_CANCELED_BECAUSE_YOU_ARE_TOO_FAR_FROM_THE_ADMISSION_MANAGER, 2, 5000));
			for (final UCTeam team : _teams)
			{
				team.setParty(null);
				team.setRegisterTime(0);
			}
			_isBattleNow = false;
			runNewTask(false);
			return;
		}
		runNewTask(true);
		splitMembersAndTeleport();
		startFight();
	}
	
	public void splitMembersAndTeleport()
	{
		final UCPoint[] positions = getPoints();
		for (final UCPoint point : positions)
		{
			point.getPlayers().clear();
		}
		
		broadcastRecord(ExPVPMatchRecord.START, 0);
		
		for (final UCTeam team : getTeams())
		{
			final Party party = team.getParty();
			if (party != null)
			{
				int i = 0;
				for (final Player player : party.getMembers())
				{
					if (player != null)
					{
						player.setUCState(Player.UC_STATE_POINT);
						player.setCanRevive(false);
						positions[i].teleportPlayer(player);
						i++;
						if (i >= 3)
						{
							i = 0;
						}
					}
				}
			}
		}
		broadcastRecord(ExPVPMatchRecord.UPDATE, 0);
	}
	
	public void broadcastRecord(int type, int teamType)
	{
		final ExPVPMatchRecord packet = new ExPVPMatchRecord(type, teamType, this);
		final ExPVPMatchUserDie packet2 = type == ExPVPMatchRecord.UPDATE ? new ExPVPMatchUserDie(this) : null;
		for (final UCTeam team : getTeams())
		{
			final Party party = team.getParty();
			if (party != null)
			{
				for (final Player member : party.getMembers())
				{
					if (member != null)
					{
						member.sendPacket(packet);
						if (packet2 != null)
						{
							member.sendPacket(packet2);
						}
					}
				}
			}
		}
	}

	public void startFight()
	{
		for (final UCTeam team : _teams)
		{
			team.spawnTower();
			for (final Player player : team.getParty().getMembers())
			{
				if (player != null)
				{
					player.setTeam(team.getIndex() + 1);
				}
			}
		}
	}
	
	public void removeTeams()
	{
		for (final UCTeam team : _teams)
		{
			if (team.getParty() != null)
			{
				for (final Player player : team.getParty().getMembers())
				{
					if (player == null)
					{
						continue;
					}
					
					player.setTeam(0);
					player.cleanUCStats();
					player.setCanRevive(true);
					player.setUCState(Player.UC_STATE_NONE);
					if (player.isDead())
					{
						UCTeam.resPlayer(player);
					}
					
					if (player.getSaveLoc() != null)
					{
						player.teleToLocation(player.getSaveLoc(), true, ReflectionManager.DEFAULT);
					}
					else
					{
						player.teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
					}
				}
			}
		}
	}
	
	public boolean isBattleNow()
	{
		return _isBattleNow;
	}
	
	public void setIsBattleNow(boolean value)
	{
		_isBattleNow = value;
	}
	
	public void setReward(UCReward ucReward)
	{
		_rewards.add(ucReward);
	}
	
	public List<UCReward> getRewards()
	{
		return _rewards;
	}
}