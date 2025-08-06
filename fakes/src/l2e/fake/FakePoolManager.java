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
package l2e.fake;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import l2e.commons.threading.PriorityThreadFactory;
import l2e.gameserver.Config;

public class FakePoolManager
{
	private static final long MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;

	private final ScheduledThreadPoolExecutor _scheduledExecutor;
	private final ThreadPoolExecutor _executor;
	
	private boolean _shutdown;

	private FakePoolManager()
	{
		_scheduledExecutor = new ScheduledThreadPoolExecutor(Config.SCHEDULED_THREAD_POOL_SIZE, new PriorityThreadFactory("ScheduledThreadPool", Thread.NORM_PRIORITY), new ThreadPoolExecutor.CallerRunsPolicy());
		_executor = new ThreadPoolExecutor(Config.EXECUTOR_THREAD_POOL_SIZE, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("ThreadPoolExecutor", Thread.NORM_PRIORITY), new ThreadPoolExecutor.CallerRunsPolicy());
		
		scheduleAtFixedRate(() ->
		{
			_scheduledExecutor.purge();
			_executor.purge();
		}, 300000L, 300000L);
	}

	private long validate(long delay)
	{
		return Math.max(0, Math.min(MAX_DELAY, delay));
	}

	public boolean isShutdown()
	{
		return _shutdown;
	}
	
	public Runnable wrap(Runnable r)
	{
		return r;
	}
	
	public ScheduledFuture<?> schedule(Runnable r, long delay)
	{
		return _scheduledExecutor.schedule(wrap(r), validate(delay), TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay)
	{
		return _scheduledExecutor.scheduleAtFixedRate(wrap(r), validate(initial), validate(delay), TimeUnit.MILLISECONDS);
	}
	
	public ScheduledFuture<?> scheduleAtFixedDelay(Runnable r, long initial, long delay)
	{
		return _scheduledExecutor.scheduleWithFixedDelay(wrap(r), validate(initial), validate(delay), TimeUnit.MILLISECONDS);
	}
	
	public void execute(Runnable r)
	{
		_executor.execute(wrap(r));
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
	
	public static FakePoolManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePoolManager _instance = new FakePoolManager();
	}
}