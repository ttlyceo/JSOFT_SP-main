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

import java.util.Date;

import l2e.commons.log.LoggerObject;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;

public class AutoRestart extends LoggerObject
{
	private long _nextRestart;
	
	protected AutoRestart()
	{
		startCalculationOfNextRestartTime();
		if (getRestartNextTime() <= 0)
		{
			info("System is disabled.");
		}
	}
	
	public long getRestartNextTime()
	{
		if (_nextRestart > System.currentTimeMillis())
		{
			return (_nextRestart - System.currentTimeMillis()) / 1000;
		}
		return 0L;
	}
	
	private void startCalculationOfNextRestartTime()
	{
		try
		{
			SchedulingPattern cronTime;
			try
			{
				cronTime = new SchedulingPattern(Config.AUTO_RESTART_PATTERN);
			}
			catch (final InvalidPatternException e)
			{
				return;
			}
			
			final var nextRestart = cronTime.next(System.currentTimeMillis());
			if (nextRestart > System.currentTimeMillis())
			{
				_nextRestart = nextRestart + (Config.AUTO_RESTART_TIME * 1000L);
				info("System activated.");
				info("Next restart - " + new Date(_nextRestart));
				ThreadPoolManager.getInstance().schedule(new RestartTask(), (nextRestart - System.currentTimeMillis()));
			}
		}
		catch (final Exception e)
		{
			warn("Has problem with the config file, please, check and correct it.!");
		}
	}
	
	private class RestartTask implements Runnable
	{
		@Override
		public void run()
		{
			info("Auto restart started.");
			Shutdown.getInstance().autoRestart(Config.AUTO_RESTART_TIME);
		}
	}
	
	public static AutoRestart getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoRestart _instance = new AutoRestart();
	}
}