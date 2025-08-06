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
package l2e.gameserver.data.htm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Player;

public class WarehouseCache
{
	protected final Map<Player, Long> _cachedWh = new ConcurrentHashMap<>();
	protected final long _cacheTime = Config.WAREHOUSE_CACHE_TIME * 60000L;
	
	protected WarehouseCache()
	{
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new CacheScheduler(), 120000, 60000);
	}

	public void addCacheTask(Player pc)
	{
		_cachedWh.put(pc, System.currentTimeMillis());
	}

	public void remCacheTask(Player pc)
	{
		_cachedWh.remove(pc);
	}

	public class CacheScheduler implements Runnable
	{
		@Override
		public void run()
		{
			final var cTime = System.currentTimeMillis();
			for (final var pc : _cachedWh.keySet())
			{
				if ((cTime - _cachedWh.get(pc)) > _cacheTime)
				{
					pc.clearWarehouse();
					_cachedWh.remove(pc);
				}
			}
		}
	}

	public static WarehouseCache getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final WarehouseCache _instance = new WarehouseCache();
	}
}