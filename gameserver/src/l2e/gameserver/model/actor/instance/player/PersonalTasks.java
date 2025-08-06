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
package l2e.gameserver.model.actor.instance.player;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.player.impl.AbstractPlayerTask;
import l2e.gameserver.model.actor.instance.player.impl.PunishmentTask;
import l2e.gameserver.model.punishment.PunishmentTemplate;

/**
 * Created by LordWinter
 */
public class PersonalTasks extends LoggerObject
{
	private final Player _owner;
	private final Map<AbstractPlayerTask, Long> _playerTasks = new ConcurrentHashMap<>();
	private Future<?> _timeTask = null;
	private boolean _isRecalc = false;
	private long _nextTimeCalc = 0L;
	
	public PersonalTasks(Player player)
	{
		_owner = player;
		_playerTasks.clear();
	}
	
	public boolean isActiveTask(int id)
	{
		final var tasks = _playerTasks;
		if (tasks.isEmpty())
		{
			return false;
		}
		
		for (final var task : _playerTasks.keySet())
		{
			if (task != null && task.isOneUse() && task.getId() == id)
			{
				return true;
			}
		}
		return false;
	}
	
	public long isTaskDelay(int id)
	{
		final var tasks = _playerTasks;
		if (tasks.isEmpty())
		{
			return 0L;
		}
		
		for (final var task : _playerTasks.keySet())
		{
			if (task != null && task.getId() == id)
			{
				return _playerTasks.get(task);
			}
		}
		return 0L;
	}
	
	public AbstractPlayerTask getActiveTask(int id)
	{
		final var tasks = _playerTasks;
		if (tasks.isEmpty())
		{
			return null;
		}
		
		for (final var task : _playerTasks.keySet())
		{
			if (task != null && task.getId() == id)
			{
				return task;
			}
		}
		return null;
	}
	
	public boolean addTask(AbstractPlayerTask task)
	{
		if (isActiveTask(task.getId()))
		{
			return false;
		}
		
		final var timeCalc = task.getInterval();
		_playerTasks.put(task, timeCalc);
		if (_nextTimeCalc <= 0)
		{
			recalcTime();
		}
		else
		{
			if (_nextTimeCalc > timeCalc)
			{
				recalcTime();
			}
		}
		return true;
	}
	
	public boolean removeTask(int id, boolean isRecalc)
	{
		boolean found = false;
		for (final var entry : _playerTasks.entrySet())
		{
			final var task = entry.getKey();
			if (task != null && task.getId() == id)
			{
				_playerTasks.remove(task);
				found = true;
				break;
			}
		}
		
		if (found && isRecalc)
		{
			recalcTime();
		}
		return found;
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
		
		if (_playerTasks.isEmpty())
		{
			_nextTimeCalc = 0;
			_isRecalc = false;
			return;
		}
		
		final var sorted = _playerTasks.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		if (!sorted.isEmpty())
		{
			long nextTime = sorted.entrySet().iterator().next().getValue() - System.currentTimeMillis();
			if (nextTime < 0)
			{
				nextTime = 0;
			}
			_timeTask = ThreadPoolManager.getInstance().schedule(new CheckTasks(), nextTime);
			_nextTimeCalc = System.currentTimeMillis() + nextTime;
			if (Config.DEBUG && nextTime > 0)
			{
				info("Next task run at " + new Date(System.currentTimeMillis() + nextTime));
			}
		}
		_isRecalc = false;
	}
	
	private class CheckTasks implements Runnable
	{
		@Override
		public void run()
		{
			_isRecalc = true;
			final long time = System.currentTimeMillis();
			for (final var entry : _playerTasks.entrySet())
			{
				final var task = entry.getKey();
				if (time < entry.getValue())
				{
					continue;
				}
				
				if (task != null)
				{
					task.getTask(_owner);
					if (task.isSingleUse())
					{
						_playerTasks.remove(task);
					}
					else
					{
						_playerTasks.put(task, task.getInterval());
					}
				}
			}
			_isRecalc = false;
			recalcTime();
		}
	}
	
	public void cleanUp()
	{
		final var task = _timeTask;
		if (task != null)
		{
			task.cancel(false);
		}
		_timeTask = null;
		_nextTimeCalc = 0L;
		_playerTasks.clear();
	}
	
	public void removePunishment(PunishmentTemplate template)
	{
		boolean found = false;
		for (final var entry : _playerTasks.entrySet())
		{
			final var task = entry.getKey();
			if (task != null && task.getId() == 10)
			{
				final var p = (PunishmentTask) task;
				if (p.getTemplate().getId() == template.getId())
				{
					_playerTasks.remove(task);
					found = true;
					break;
				}
			}
		}
		
		if (found)
		{
			recalcTime();
		}
	}
}