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
package l2e.gameserver.taskmanager;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.DayNightSpawnManager;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.instancemanager.RaidBossSpawnManager.StatusEnum;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.actor.templates.npc.AnnounceTemplate;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;

/**
 * Created by LordWinter
 */
public class RaidBossTaskManager extends LoggerObject
{
	private final Map<Integer, Long> _raidList = new ConcurrentHashMap<>();
	private final Map<AnnounceTemplate, Long> _announceList = new ConcurrentHashMap<>();
	private final Map<MonsterInstance, Long> _minionList = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _timeTask = null;
	private ScheduledFuture<?> _announceTask = null;
	private ScheduledFuture<?> _minionTask = null;
	
	public RaidBossTaskManager()
	{
		_raidList.clear();
		_announceList.clear();
		_minionList.clear();
	}
	
	public void recalcAll()
	{
		recalcTime();
		recalcAnnounceTime();
	}
	
	public void addToMinionList(MonsterInstance minion, long time)
	{
		if (!_minionList.containsKey(minion))
		{
			_minionList.put(minion, time);
			recalcMinionTime();
		}
	}
	
	public void removeMinions(MonsterInstance leader)
	{
		if (_minionTask != null)
		{
			_minionTask.cancel(false);
		}
		_minionTask = null;
		
		for (final Entry<MonsterInstance, Long> entry : _minionList.entrySet())
		{
			final var minion = entry.getKey();
			final var master = minion.getLeader();
			if (master != null && master == leader)
			{
				_minionList.remove(minion);
			}
		}
		recalcMinionTime();
	}
	

	public void addToAnnounceList(AnnounceTemplate tpl, long time, boolean isRecalc)
	{
		if (!_announceList.containsKey(tpl))
		{
			_announceList.put(tpl, time);
			if (isRecalc)
			{
				recalcAnnounceTime();
			}
		}
	}
	
	public void removeAnnounce(int bossId)
	{
		AnnounceTemplate template = null;
		for (final var tpl : _announceList.keySet())
		{
			if (tpl != null && tpl.getTemplate().getId() == bossId)
			{
				template = tpl;
				break;
			}
		}
		
		if (template != null)
		{
			_announceList.remove(template);
		}
	}
	
	public void addToRaidList(int bossId, long time, boolean isRecalc)
	{
		if (!_raidList.containsKey(bossId))
		{
			_raidList.put(bossId, time);
			if (isRecalc)
			{
				recalcTime();
			}
		}
	}
	
	public boolean isInRaidList(int bossId)
	{
		return _raidList.containsKey(bossId);
	}
	
	public void removeFromList(int bossId)
	{
		if (_raidList.containsKey(bossId))
		{
			_raidList.remove(bossId);
		}
		
		AnnounceTemplate template = null;
		for (final var tpl : _announceList.keySet())
		{
			if (tpl != null && tpl.getTemplate().getId() == bossId)
			{
				template = tpl;
				break;
			}
		}
		
		if (template != null)
		{
			_announceList.remove(template);
		}
	}
	
	public void cleanUp()
	{
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		
		if (_announceTask != null)
		{
			_announceTask.cancel(false);
		}
		_announceTask = null;
		
		if (_minionTask != null)
		{
			_minionTask.cancel(false);
		}
		_minionTask = null;
		_raidList.clear();
		_announceList.clear();
		_minionList.clear();
	}
	
	public void recalcAnnounceTime()
	{
		if (_announceTask != null)
		{
			_announceTask.cancel(false);
		}
		_announceTask = null;
		
		if (_announceList.isEmpty())
		{
			return;
		}
		
		final Map<AnnounceTemplate, Long> sorted = _announceList.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_announceTask = ThreadPoolManager.getInstance().schedule(new CheckAnnounceList(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next announce task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
	}
	
	public void recalcMinionTime()
	{
		if (_minionTask != null)
		{
			_minionTask.cancel(false);
		}
		_minionTask = null;
		
		if (_minionList.isEmpty())
		{
			return;
		}
		
		final Map<MonsterInstance, Long> sorted = _minionList.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_minionTask = ThreadPoolManager.getInstance().schedule(new CheckMinionList(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next minion task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
	}
	
	public void recalcTime()
	{
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		
		if (_raidList.isEmpty())
		{
			return;
		}
		
		final Map<Integer, Long> sorted = _raidList.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_timeTask = ThreadPoolManager.getInstance().schedule(new CheckRaidList(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
	}
	
	private class CheckAnnounceList implements Runnable
	{
		@Override
		public void run()
		{
			final long time = System.currentTimeMillis();
			for (final Entry<AnnounceTemplate, Long> entry : _announceList.entrySet())
			{
				final var tpl = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					if (player != null && player.isOnline())
					{
						final ServerMessage msg = new ServerMessage("Announce.RAID_PRE_ANNOUNCE", player.getLang());
						msg.add(tpl.getTemplate().getName(player.getLang()));
						msg.add((int) (tpl.getDelay() / 60000));
						player.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, "", msg.toString()));
					}
				}
				_announceList.remove(tpl);
			}
			recalcAnnounceTime();
		}
	}
	
	private class CheckMinionList implements Runnable
	{
		@Override
		public void run()
		{
			final long time = System.currentTimeMillis();
			for (final Entry<MonsterInstance, Long> entry : _minionList.entrySet())
			{
				final var minion = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				final var master = minion.getLeader();
				if (master != null && !master.isDead())
				{
					minion.refreshID();
					master.spawnMinion(minion);
				}
				_minionList.remove(minion);
			}
			recalcMinionTime();
		}
	}
	
	private class CheckRaidList implements Runnable
	{
		@Override
		public void run()
		{
			final long time = System.currentTimeMillis();
			final var instance = RaidBossSpawnManager.getInstance();
			for (final Entry<Integer, Long> entry : _raidList.entrySet())
			{
				final int bossId = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				RaidBossInstance raidboss = null;
				
				if (bossId == 25328)
				{
					raidboss = DayNightSpawnManager.getInstance().handleBoss(instance.getSpawns().get(bossId));
				}
				else
				{
					if (instance.isDefined(bossId))
					{
						raidboss = (RaidBossInstance) instance.getSpawns().get(bossId).doSpawn();
					}
				}

				instance.askOnSummon(raidboss, instance);

				_raidList.remove(bossId);
			}
			recalcTime();
		}
	}

	public static final RaidBossTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidBossTaskManager _instance = new RaidBossTaskManager();
	}
}