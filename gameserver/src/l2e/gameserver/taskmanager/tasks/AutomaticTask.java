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
package l2e.gameserver.taskmanager.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import l2e.gameserver.ThreadPoolManager;

public abstract class AutomaticTask implements Runnable
{
	protected static final Logger _log = LoggerFactory.getLogger(AutomaticTask.class);

	public AutomaticTask()
	{
		init(true);
	}

	public abstract void doTask() throws Exception;

	public abstract long reCalcTime(boolean start);

	public void init(boolean start)
	{
		ThreadPoolManager.getInstance().schedule(this, reCalcTime(start) - System.currentTimeMillis());
	}

	@Override
	public void run()
	{
		try
		{
			doTask();
		}
		catch(final Exception e)
		{
			_log.error("Exception: " + e, e);
		}
		finally
		{
			init(false);
		}
	}
}
