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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.HerbsDropParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;

public class ItemsAutoDestroy extends LoggerObject
{
	private final Map<ItemInstance, Long> _items = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _timeTask = null;
	private boolean _isRecalc = false;
	private boolean _isLocked = false;
	private long _nextRecal = 0L;
	private long _herbRecal = 0L;
	private static final int[] HERBS =
	{
	        14824, 14825, 14826, 14827
	};
	
	public ItemsAutoDestroy()
	{
		_items.clear();
		final var herbs = HerbsDropParser.getInstance();
		if (herbs.getHerbSpawns().size() > 0)
		{
			final long destroyTime = System.currentTimeMillis() + (herbs.getRespawnDelay() * 60000L);
			_herbRecal = destroyTime;
			herbs.getHerbSpawns().stream().filter(r -> (r != null)).forEach(i -> spawnHerbs(Rnd.get(1, herbs.getRndAmount()), i, destroyTime));
			recalcTime();
		}
	}
	
	private void spawnHerbs(int count, Location loc, long destroyTime)
	{
		if (Rnd.getChance(HerbsDropParser.getInstance().getChance()))
		{
			for (int i = 0; i < count; i++)
			{
				final var item = new ItemInstance(IdFactory.getInstance().getNextId(), HERBS[Rnd.get(0, HERBS.length - 1)]);
				item.setCount(1);
				final var pos = Location.findPointToStay(loc, loc.getHeading(), false);
				item.dropMe(null, pos, false);
				addItem(item, destroyTime);
			}
		}
	}
	
	public synchronized void tryRecalcTime()
	{
		if (_timeTask == null)
		{
			recalcTime();
			return;
		}
		
		if (!_isRecalc || _isLocked)
		{
			return;
		}
		recalcTime();
	}
	
	private void recalcTime()
	{
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		
		if (_items.isEmpty())
		{
			_isRecalc = false;
			return;
		}
		
		final var sorted = _items.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			
			if (_herbRecal <= nextTime)
			{
				_nextRecal = 0;
			}
			_nextRecal = System.currentTimeMillis() + nextTime;
			_timeTask = ThreadPoolManager.getInstance().schedule(new CheckItemsForDestroy(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next task run at " + new Date(_nextRecal));
			}
		}
		_isRecalc = false;
	}
	
	public synchronized boolean addItem(ItemInstance item, long destroyDelay)
	{
		long dropDelay = 0L;
		final var it = item.getItem();
		final var now = System.currentTimeMillis();
		if (it.getAutoDestroyTime() > 0)
		{
			dropDelay = now + it.getAutoDestroyTime();
		}
		else if (it.isHerb())
		{
			dropDelay = now + (Config.HERB_AUTO_DESTROY_TIME * 1000L);
		}
		else
		{
			dropDelay = now + (Config.AUTODESTROY_ITEM_AFTER * 1000L);
		}
		
		if (destroyDelay > 0)
		{
			dropDelay = destroyDelay;
		}
		
		if (dropDelay > now)
		{
			item.setDropTime(dropDelay);
			_items.put(item, dropDelay);
			if (dropDelay < _nextRecal && !_isRecalc && !_isLocked)
			{
				_isRecalc = true;
				return true;
			}
		}
		return false;
	}
	
	private class CheckItemsForDestroy implements Runnable
	{
		@Override
		public void run()
		{
			_isLocked = true;
			final var curtime = System.currentTimeMillis();
			for (final var entry : _items.entrySet())
			{
				final var item = entry.getKey();
				if (curtime < entry.getValue())
				{
					continue;
				}
				
				if (item == null || (item.getItemLocation() != ItemLocation.VOID))
				{
					_items.remove(item);
				}
				item.decayMe();
				_items.remove(item);
			}
			
			if (_herbRecal <= curtime)
			{
				final var herbs = HerbsDropParser.getInstance();
				if (herbs.getHerbSpawns().size() > 0)
				{
					final long destroyTime = System.currentTimeMillis() + (HerbsDropParser.getInstance().getRespawnDelay() * 60000L);
					_herbRecal = destroyTime;
					herbs.getHerbSpawns().stream().filter(r -> (r != null)).forEach(i -> spawnHerbs(Rnd.get(1, HerbsDropParser.getInstance().getRndAmount()), i, destroyTime));
				}
			}
			_isLocked = false;
			recalcTime();
		}
	}
	
	public static ItemsAutoDestroy getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemsAutoDestroy _instance = new ItemsAutoDestroy();
	}
}