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
package l2e.fake;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.model.actor.templates.player.FakePassiveLocTemplate;

/**
 * Created by LordWinter
 */
public class PassiveFakeTaskManager
{
	protected static final Logger _log = LoggerFactory.getLogger(PassiveFakeTaskManager.class);
	
	private final Map<FakePlayer, Long> _spawnPlayers = new ConcurrentHashMap<>();
	private final Map<FakePlayer, Long> _despawnPlayers = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _spawnTask = null;
	private ScheduledFuture<?> _despawnTask = null;
	private boolean _isSpawnActive = false;
	private boolean _isEnableDespawn = false;
	
	public PassiveFakeTaskManager()
	{
		_spawnPlayers.clear();
		_despawnPlayers.clear();
	}
	
	public void addSpawnPlayer(FakePlayer player, long newTime)
	{
		final long time = System.currentTimeMillis() + newTime;
		_spawnPlayers.put(player, time);
		if (!_isEnableDespawn)
		{
			_isEnableDespawn = true;
			recalcSpawnTime();
		}
	}
	
	public void addDespawnPlayer(FakePlayer player, long newTime)
	{
		final long time = System.currentTimeMillis() + (newTime * 1000L);
		_despawnPlayers.put(player, time);
	}
	
	public void recalcSpawnTime()
	{
		if (_spawnTask != null)
		{
			_spawnTask.cancel(false);
		}
		_spawnTask = null;
		
		if (_spawnPlayers.isEmpty())
		{
			return;
		}
		
		final Map<FakePlayer, Long> sorted = _spawnPlayers.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_spawnTask = FakePoolManager.getInstance().schedule(new CheckSpawnPlayers(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				_log.info("PassiveFakeTaskManager: Next spawned task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
	}
	
	public void recalcDespawnTime()
	{
		if (_despawnTask != null)
		{
			_despawnTask.cancel(false);
		}
		_despawnTask = null;
		
		if (_despawnPlayers.isEmpty())
		{
			return;
		}
		
		final Map<FakePlayer, Long> sorted = _despawnPlayers.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_despawnTask = FakePoolManager.getInstance().schedule(new CheckDespawnPlayers(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				_log.info("PassiveFakeTaskManager: Next despawned task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
	}
	
	private class CheckSpawnPlayers implements Runnable
	{
		@Override
		public void run()
		{
			final long time = System.currentTimeMillis();
			for (final Entry<FakePlayer, Long> entry : _spawnPlayers.entrySet())
			{
				final FakePlayer pl = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				final FakePassiveLocTemplate loc = pl.getFakeTerritory();
				if (loc != null)
				{
					pl.deleteMe();
					loc.setCurrentAmount(loc.getCurrentAmount() - 1);
					addDespawnPlayer(pl, Rnd.get(loc.getMinRespawn(), loc.getMaxRespawn()));
					_spawnPlayers.remove(pl);
				}
			}
			checkTime();
		}
	}
	
	private void checkTime()
	{
		if (_isSpawnActive)
		{
			return;
		}
		_isSpawnActive = true;
		recalcSpawnTime();
		recalcDespawnTime();
		_isSpawnActive = false;
	}
	
	private class CheckDespawnPlayers implements Runnable
	{
		@Override
		public void run()
		{
			final long time = System.currentTimeMillis();
			for (final Entry<FakePlayer, Long> entry : _despawnPlayers.entrySet())
			{
				final FakePlayer pl = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				FakePlayerManager.getInstance().respawnPassivePlayer();
				_despawnPlayers.remove(pl);
			}
			checkTime();
		}
	}
	
	public static PassiveFakeTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PassiveFakeTaskManager _instance = new PassiveFakeTaskManager();
	}
}