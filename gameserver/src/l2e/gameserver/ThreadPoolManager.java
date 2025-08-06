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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import l2e.commons.log.LoggerObject;
import l2e.commons.threading.RunnableWrapper;

public class ThreadPoolManager extends LoggerObject
{
	private static final long MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;
	
	private static int _threadPoolRandomizer;
	
	protected ScheduledThreadPoolExecutor[] _scheduledExecutor;
	protected ThreadPoolExecutor[] _executor;
	
	private boolean _shutdown;
	
	private ThreadPoolManager()
	{
		int poolCount = Config.SCHEDULED_THREAD_POOL_SIZE;
		_scheduledExecutor = new ScheduledThreadPoolExecutor[poolCount];
		for (int i = 0; i < poolCount; i++)
		{
			_scheduledExecutor[i] = new ScheduledThreadPoolExecutor(4);
		}
		
		poolCount = Config.EXECUTOR_THREAD_POOL_SIZE;
		_executor = new ThreadPoolExecutor[poolCount];
		for (int i = 0; i < poolCount; i++)
		{
			_executor[i] = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000));
		}
		
		for (final var threadPool : _scheduledExecutor)
		{
			threadPool.setRemoveOnCancelPolicy(true);
			threadPool.prestartAllCoreThreads();
		}
		
		for (final var threadPool : _executor)
		{
			threadPool.prestartAllCoreThreads();
		}
		

		scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				for (final var threadPool : _scheduledExecutor)
				{
					threadPool.purge();
				}
				
				for (final var threadPool : _executor)
				{
					threadPool.purge();
				}
			}
		}, 60000L, 60000L);
	}

	public boolean isShutdown()
	{
		return _shutdown;
	}
	
	private <T> T getPool(T[] threadPools)
	{
		return threadPools[_threadPoolRandomizer++ % threadPools.length];
	}
	
	private long validate(long delay)
	{
		return Math.max(0, Math.min(MAX_DELAY, delay));
	}
	
	public ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit timeUnit)
	{
		try
		{
			return getPool(_scheduledExecutor).schedule(new RunnableWrapper(r), validate(delay), timeUnit);
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
			return getPool(_scheduledExecutor).scheduleAtFixedRate(new RunnableWrapper(r), validate(initial), validate(delay), timeUnit);
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
	
	public ScheduledFuture<?> scheduleAtFixedDelay(Runnable r, long initial, long delay, TimeUnit timeUnit)
	{
		try
		{
			return getPool(_scheduledExecutor).scheduleWithFixedDelay(new RunnableWrapper(r), validate(initial), validate(delay), timeUnit);
		}
		catch (final RejectedExecutionException e)
		{
			return null;
		}
	}

	public ScheduledFuture<?> scheduleAtFixedDelay(Runnable r, long initial, long delay)
	{
		return scheduleAtFixedDelay(r, initial, delay, TimeUnit.MILLISECONDS);
	}
	
	public void execute(Runnable r)
	{
		try
		{
			getPool(_executor).execute(new RunnableWrapper(r));
		}
		catch (final RejectedExecutionException e)
		{
		}
	}

	public void shutdown()
	{
		_shutdown = true;
		try
		{
			for (final var threadPool : _scheduledExecutor)
			{
				threadPool.shutdownNow();
			}
			
			for (final var threadPool : _executor)
			{
				threadPool.shutdownNow();
			}
		}
		catch (final Throwable t)
		{
		}
	}
	
	public void getInfo()
	{
		for (int i = 0; i < _scheduledExecutor.length; i++)
		{
			final var threadPool = _scheduledExecutor[i];
			info("=================================================");
			info("ScheduledPool #" + i + ":");
			info("getActiveCount: ...... " + threadPool.getActiveCount());
			info("getCorePoolSize: ..... " + threadPool.getCorePoolSize());
			info("getPoolSize: ......... " + threadPool.getPoolSize());
			info("getLargestPoolSize: .. " + threadPool.getLargestPoolSize());
			info("getMaximumPoolSize: .. " + threadPool.getMaximumPoolSize());
			info("getCompletedTaskCount: " + threadPool.getCompletedTaskCount());
			info("getQueuedTaskCount: .. " + threadPool.getQueue().size());
			info("getTaskCount: ........ " + threadPool.getTaskCount());
		}
		
		for (int i = 0; i < _executor.length; i++)
		{
			final var threadPool = _executor[i];
			info("=================================================");
			info("ExecutorPool #" + i + ":");
			info("getActiveCount: ...... " + threadPool.getActiveCount());
			info("getCorePoolSize: ..... " + threadPool.getCorePoolSize());
			info("getPoolSize: ......... " + threadPool.getPoolSize());
			info("getLargestPoolSize: .. " + threadPool.getLargestPoolSize());
			info("getMaximumPoolSize: .. " + threadPool.getMaximumPoolSize());
			info("getCompletedTaskCount: " + threadPool.getCompletedTaskCount());
			info("getQueuedTaskCount: .. " + threadPool.getQueue().size());
			info("getTaskCount: ........ " + threadPool.getTaskCount());
		}
	}
	
	public static ThreadPoolManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ThreadPoolManager _instance = new ThreadPoolManager();
	}
}
