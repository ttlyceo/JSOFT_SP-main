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
package l2e.scripts.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.TimeUtils;
import l2e.gameserver.Announcements;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.WorldEventParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.model.entity.events.model.template.WorldEventSpawn;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Updated by LordWinter 13.07.2020
 */
public class Achievements extends AbstractWorldEvent
{
	private boolean _isActive = false;
	private WorldEventTemplate _template = null;
	private ScheduledFuture<?> _eventTask = null;
	
	private final List<Npc> _npcList = new ArrayList<>();
	
	public Achievements(String name, String descr)
	{
		super(name, descr);

		_template = WorldEventParser.getInstance().getEvent(1);
		if (_template != null && !_isActive)
		{
			if (_template.isNonStop())
			{
				eventStart(-1, false);
			}
			else
			{
				final long startTime = calcEventStartTime(_template, false);
				final long expireTime = calcEventStopTime(_template, false);
				if (startTime <= System.currentTimeMillis() && expireTime > System.currentTimeMillis() || (expireTime < startTime && expireTime > System.currentTimeMillis()))
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
				else
				{
					checkTimerTask(startTime);
				}
			}
		}
	}

	@Override
	public boolean isEventActive()
	{
		return _isActive;
	}

	@Override
	public WorldEventTemplate getEventTemplate()
	{
		return _template;
	}
	
	@Override
	public boolean eventStart(long totalTime, boolean force)
	{
		if (_isActive || totalTime == 0)
		{
			return false;
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_isActive = true;
		
		final List<WorldEventSpawn> spawnList = _template.getSpawnList();
		if (spawnList != null && !spawnList.isEmpty())
		{
			for (final WorldEventSpawn spawn : spawnList)
			{
				_npcList.add(addSpawn(spawn.getNpcId(), spawn.getLocation().getX(), spawn.getLocation().getY(), spawn.getLocation().getZ(), spawn.getLocation().getHeading(), false, 0));
			}
		}
		
		final ServerMessage msg = new ServerMessage("EventAchievements.START", true);
		Announcements.getInstance().announceToAll(msg);
		
		AchievementManager.getInstance().setIsActive(true);
		if (totalTime > 0)
		{
			_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					eventStop();
				}
			}, totalTime);
			_log.info("Event " + _template.getName(null) + " will end in: " + TimeUtils.toSimpleFormat(System.currentTimeMillis() + totalTime));
		}
		return true;
	}

	@Override
	public boolean eventStop()
	{
		if (!_isActive)
		{
			return false;
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		_isActive = false;

		if (!_npcList.isEmpty())
		{
			for (final Npc _npc : _npcList)
			{
				if (_npc != null)
				{
					_npc.deleteMe();
				}
			}
		}
		_npcList.clear();

		final ServerMessage msg = new ServerMessage("EventAchievements.STOP", true);
		Announcements.getInstance().announceToAll(msg);
		AchievementManager.getInstance().setIsActive(false);
		checkTimerTask(calcEventStartTime(_template, false));

		return true;
	}
	
	@Override
	public void startTimerTask(long time)
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				final long expireTime = calcEventStopTime(_template, false);
				if (expireTime > System.currentTimeMillis())
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
			}
		}, (time - System.currentTimeMillis()));
		_log.info("Event " + _template.getName(null) + " will start in: " + TimeUtils.toSimpleFormat(time));
	}
	
	@Override
	public boolean isReloaded()
	{
		if (isEventActive())
		{
			return false;
		}
		
		_template = WorldEventParser.getInstance().getEvent(1);
		if (_template != null)
		{
			if (_template.isNonStop())
			{
				eventStart(-1, false);
			}
			else
			{
				final long startTime = calcEventStartTime(_template, false);
				final long expireTime = calcEventStopTime(_template, false);
				if (startTime <= System.currentTimeMillis() && expireTime > System.currentTimeMillis() || (expireTime < startTime && expireTime > System.currentTimeMillis()))
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
				else
				{
					checkTimerTask(startTime);
				}
			}
			return true;
		}
		return false;
	}

	public static void main(String[] args)
	{
		new Achievements(Achievements.class.getSimpleName(), "events");
	}
}
