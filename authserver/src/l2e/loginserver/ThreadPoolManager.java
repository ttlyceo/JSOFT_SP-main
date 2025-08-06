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
package l2e.loginserver;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import l2e.commons.threading.RunnableWrapper;

public class ThreadPoolManager
{
	private static final long MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;
	
	private final ScheduledThreadPoolExecutor _scheduledExecutor;
	private final ThreadPoolExecutor _executor;
	
	private boolean _shutdown;

	private ThreadPoolManager()
	{
		_scheduledExecutor = new ScheduledThreadPoolExecutor(1);
		_scheduledExecutor.setRemoveOnCancelPolicy(true);
		_executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100000));
		
		scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
			_scheduledExecutor.purge();
			_executor.purge();
			}
		}, 600000L, 600000L);
	}
	
	private long validate(long delay)
	{
		return Math.max(0, Math.min(MAX_DELAY, delay));
	}

	public boolean isShutdown()
	{
		return _shutdown;
	}
	
	public ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit timeUnit)
	{
		try
		{
			return _scheduledExecutor.schedule(new RunnableWrapper(r), validate(delay), timeUnit);
		}
		catch (final RejectedExecutionException e)
		{
			return null;
		}
	}
	
	public ScheduledFuture<?> schedule(Runnable r, long delay)
	{
		return schedule(r, delay, TimeUnit.MILLISECONDS);
	}
	
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay, TimeUnit timeUnit)
	{
		try
		{
			return _scheduledExecutor.scheduleAtFixedRate(new RunnableWrapper(r), validate(initial), validate(delay), timeUnit);
		}
		catch (final RejectedExecutionException e)
		{
			return null;
		}
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay)
	{
		return scheduleAtFixedRate(r, initial, delay, TimeUnit.MILLISECONDS);
	}
	
	public void execute(Runnable r)
	{
		try
		{
			_executor.execute(new RunnableWrapper(r));
		}
		catch (final RejectedExecutionException e)
		{
		}
	}
	
	public void shutdown() throws InterruptedException
	{
		_shutdown = true;
		try
		{
			_scheduledExecutor.shutdown();
			_scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
		}
		finally
		{
			_executor.shutdown();
			_executor.awaitTermination(1, TimeUnit.MINUTES);
		}
	}
	
	public static final ThreadPoolManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ThreadPoolManager _instance = new ThreadPoolManager();
	}
}