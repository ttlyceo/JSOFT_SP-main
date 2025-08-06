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
import l2e.commons.util.TimeUtils;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.FightEventManager;

/**
 * Created by LordWinter
 */
public class EventTaskManager extends LoggerObject
{
	private final Map<AbstractFightEvent, Long> _eventTasks = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _timeTask = null;
	private boolean _isRecalc = false;
	
	public EventTaskManager()
	{
		_eventTasks.clear();
	}
	
	public void addEventTask(AbstractFightEvent event, long newTime, boolean isRecalc)
	{
		removeEventTask(event, false);
		
		_eventTasks.put(event, newTime);
		info("Next event " + event.getName(null) + " will start in: " + TimeUtils.toSimpleFormat(newTime));
		if (isRecalc)
		{
			recalcTime();
		}
	}
	
	public void removeEventTask(AbstractFightEvent event, boolean isRecalc)
	{
		AbstractFightEvent tpl = null;
		for (final var task : _eventTasks.keySet())
		{
			if (task != null && task.getId() == event.getId())
			{
				tpl = task;
				break;
			}
		}
		
		if (tpl != null)
		{
			_eventTasks.remove(tpl);
			if (isRecalc)
			{
				recalcTime();
			}
		}
	}
	
	public void recalcTime()
	{
		if (_isRecalc)
		{
			return;
		}
		_isRecalc = true;
		
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		_timeTask = null;
		
		if (_eventTasks.isEmpty())
		{
			_isRecalc = false;
			return;
		}
		
		final Map<AbstractFightEvent, Long> sorted = _eventTasks.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_timeTask = ThreadPoolManager.getInstance().schedule(new CheckEventTasks(), nextTime);
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
		_isRecalc = false;
	}
	
	private class CheckEventTasks implements Runnable
	{
		@Override
		public void run()
		{
			_isRecalc = true;
			final long time = System.currentTimeMillis();
			for (final var entry : _eventTasks.entrySet())
			{
				final var event = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				if (event != null)
				{
					FightEventManager.getInstance().startEventCountdown(event);
				}
				_eventTasks.remove(event);
			}
			_isRecalc = false;
			recalcTime();
		}
	}
	
	public static final EventTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventTaskManager _instance = new EventTaskManager();
	}
}