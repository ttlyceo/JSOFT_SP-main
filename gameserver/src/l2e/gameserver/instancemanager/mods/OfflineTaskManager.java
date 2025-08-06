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
package l2e.gameserver.instancemanager.mods;

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
import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter
 */
public class OfflineTaskManager extends LoggerObject
{
	private final Map<Player, Long> _offlinePlayers = new ConcurrentHashMap<>();
	private final int _offlineTime = Config.OFFLINE_MODE_TIME * 3600000;
	private ScheduledFuture<?> _timeTask = null;
	
	public OfflineTaskManager()
	{
		_offlinePlayers.clear();
	}
	
	public void addOfflinePlayer(Player player, long newTime, boolean recalc)
	{
		if (!_offlinePlayers.containsKey(player))
		{
			final long time = newTime <= 0 ? System.currentTimeMillis() + _offlineTime : newTime;
			_offlinePlayers.put(player, time);
			player.setVar("offlineTime", time);
			if (recalc)
			{
				recalcTime();
			}
		}
		else
		{
			final long lastTime = getRemainOfflineTime(player);
			if (lastTime <= 0)
			{
				final long time = System.currentTimeMillis() + _offlineTime;
				_offlinePlayers.put(player, time);
				player.setVar("offlineTime", time);
				if (recalc)
				{
					recalcTime();
				}
			}
		}
	}
	
	public long getRemainOfflineTime(Player player)
	{
		final Long time = _offlinePlayers.get(player);
		return time != null ? time - System.currentTimeMillis() : 0;
	}
	
	public boolean isActivePlayer(Player player)
	{
		return getRemainOfflineTime(player) > 0;
	}
	
	public void removeOfflinePlayer(Player player)
	{
		if (_offlinePlayers.containsKey(player))
		{
			_offlinePlayers.remove(player);
		}
	}
	
	public void recalcTime()
	{
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		
		if (_offlinePlayers.isEmpty())
		{
			return;
		}
		
		final Map<Player, Long> sorted = _offlinePlayers.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_timeTask = ThreadPoolManager.getInstance().schedule(new CheckOfflinePlayers(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
	}
	
	private class CheckOfflinePlayers implements Runnable
	{
		@Override
		public void run()
		{
			final long time = System.currentTimeMillis();
			for (final Entry<Player, Long> entry : _offlinePlayers.entrySet())
			{
				final Player pl = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				if (pl != null && pl.isInOfflineMode())
				{
					if (pl.getInventory().getItemByItemId(Config.OFFLINE_MODE_PRICE[0]) == null || pl.getInventory().getItemByItemId(Config.OFFLINE_MODE_PRICE[0]).getCount() < Config.OFFLINE_MODE_PRICE[1])
					{
						_offlinePlayers.remove(pl);
						pl.deleteMe();
						return;
					}
					pl.destroyItemByItemId("OfflineMode", Config.OFFLINE_MODE_PRICE[0], Config.OFFLINE_MODE_PRICE[1], pl, false);
					_offlinePlayers.put(pl, (System.currentTimeMillis() + _offlineTime));
					pl.setVar("offlineTime", time);
				}
				else
				{
					_offlinePlayers.remove(pl);
				}
			}
			recalcTime();
		}
	}
	
	public static final OfflineTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final OfflineTaskManager _instance = new OfflineTaskManager();
	}
}