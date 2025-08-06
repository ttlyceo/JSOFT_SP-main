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
package l2e.gameserver;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.instancemanager.DayNightSpawnManager;
import l2e.gameserver.model.actor.Creature;

public final class GameTimeController extends Thread
{
	private static final Logger _log = LoggerFactory.getLogger(GameTimeController.class);
	
	public static final int TICKS_PER_SECOND = 10;
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;
	public static final int IG_DAYS_PER_DAY = 6;
	public static final int MILLIS_PER_IG_DAY = (3600000 * 24) / IG_DAYS_PER_DAY;
	public static final int SECONDS_PER_IG_DAY = MILLIS_PER_IG_DAY / 1000;
	public static final int MINUTES_PER_IG_DAY = SECONDS_PER_IG_DAY / 60;
	public static final int TICKS_PER_IG_DAY = SECONDS_PER_IG_DAY * TICKS_PER_SECOND;
	public static final int TICKS_SUN_STATE_CHANGE = TICKS_PER_IG_DAY / 4;

	private final Set<Creature> _movingObjects = ConcurrentHashMap.newKeySet();
	private final long _referenceTime;
	
	private GameTimeController()
	{
		super("GameTimeController");
		super.setDaemon(true);
		super.setPriority(MAX_PRIORITY);
		
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		_referenceTime = c.getTimeInMillis();
		super.start();
	}
	
	public boolean isNowNight()
	{
		return isNight();
	}
	
	public final int getGameTime()
	{
		return (getGameTicks() % TICKS_PER_IG_DAY) / MILLIS_IN_TICK;
	}
	
	public final int getGameHour()
	{
		return getGameTime() / 60;
	}
	
	public final int getGameMinute()
	{
		return getGameTime() % 60;
	}
	
	public final boolean isNight()
	{
		return getGameHour() < 6;
	}
	
	public final int getGameTicks()
	{
		return (int) ((System.currentTimeMillis() - _referenceTime) / MILLIS_IN_TICK);
	}
	
	public final void registerMovingObject(final Creature cha)
	{
		if (cha == null)
		{
			return;
		}
		_movingObjects.add(cha);
	}
	
	private final void moveObjects()
	{
		_movingObjects.removeIf(Creature::updatePosition);
	}
	
	public final void stopTimer()
	{
		super.interrupt();
		_log.info("Stopping " + getClass().getSimpleName());
	}
	
	@Override
	public final void run()
	{
		_log.info(getClass().getSimpleName() + ": Started.");
		
		long nextTickTime, sleepTime;
		var isNight = isNight();
		
		if (isNight)
		{
			ThreadPoolManager.getInstance().execute(new Runnable()
			{
				@Override
				public final void run()
				{
					DayNightSpawnManager.getInstance().notifyChangeMode();
				}
			});
		}
		
		while (true)
		{
			nextTickTime = ((System.currentTimeMillis() / MILLIS_IN_TICK) * MILLIS_IN_TICK) + 100;
			try
			{
				moveObjects();
			}
			catch (final Throwable e)
			{
				_log.warn(getClass().getSimpleName(), e);
			}
			sleepTime = nextTickTime - System.currentTimeMillis();
			if (sleepTime > 0)
			{
				try
				{
					Thread.sleep(sleepTime);
				}
				catch (final InterruptedException e)
				{
					
				}
			}
			
			if (isNight() != isNight)
			{
				isNight = !isNight;
				
				ThreadPoolManager.getInstance().execute(new Runnable()
				{
					@Override
					public final void run()
					{
						DayNightSpawnManager.getInstance().notifyChangeMode();
					}
				});
			}
		}
	}
	
	public static GameTimeController getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final GameTimeController _instance = new GameTimeController();
	}
}