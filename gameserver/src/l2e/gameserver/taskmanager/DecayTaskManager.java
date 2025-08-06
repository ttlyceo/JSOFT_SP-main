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

import java.util.List;

import l2e.commons.collections.LazyArrayList;
import l2e.commons.util.Util;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Creature;

public class DecayTaskManager
{
	private DecayTask[] _decayTasks = new DecayTask[1000];
	private int _decayTasksSize = 0;
	private final Object decayTasks_lock = new Object();
	
	private DecayTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new DecayScheduler(), 2000, 2000);
	}

	public void addDecayTask(Creature actor, long interval, boolean checkTime)
	{
		removeObject(actor);
		if (checkTime)
		{
			interval += System.currentTimeMillis();
		}
		addObject(new DecayTask(actor, interval));
	}
	
	public void cancelDecayTask(Creature actor)
	{
		removeObject(actor);
	}
	
	public class DecayScheduler implements Runnable
	{
		@Override
		public void run()
		{
			if (_decayTasksSize > 0)
			{
				try
				{
					final List<Creature> works = new LazyArrayList<>();
					synchronized (decayTasks_lock)
					{
						final long current = System.currentTimeMillis();
						final int size = _decayTasksSize;
						
						for (int i = size - 1; i >= 0; i--)
						{
							try
							{
								final DecayTask container = _decayTasks[i];
								if (container != null && container.endtime > 0 && current > container.endtime)
								{
									final Creature actor = container.getActor();
									if (actor != null)
									{
										works.add(actor);
									}
									
									container.endtime = -1;
								}
								
								if (container == null || container.getActor() == null || container.endtime < 0)
								{
									if (i == _decayTasksSize - 1)
									{
										_decayTasks[i] = null;
									}
									else
									{
										_decayTasks[i] = _decayTasks[_decayTasksSize - 1];
										_decayTasks[_decayTasksSize - 1] = null;
									}
									
									if (_decayTasksSize > 0)
									{
										_decayTasksSize--;
									}
								}
							}
							catch (final Exception e)
							{
								e.printStackTrace();
							}
						}
					}
					
					for (final Creature work : works)
					{
						work.onDecay();
					}
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public String toString()
	{
		final StringBuffer sb = new StringBuffer("============= DecayTask Manager Report ============\n\r");
		sb.append("Tasks count: ").append(_decayTasksSize).append("\n\r");
		sb.append("Tasks dump:\n\r");
		
		final long current = System.currentTimeMillis();
		for (final DecayTask container : _decayTasks)
		{
			sb.append("Class/Name: ").append(container.getClass().getSimpleName()).append('/').append(container.getActor());
			sb.append(" decay timer: ").append(Util.formatTime((int) (container.endtime - current))).append("\n\r");
		}
		return sb.toString();
	}
	
	private class DecayTask
	{
		private final Creature _actor;
		public long endtime;
		
		public DecayTask(Creature cha, long delay)
		{
			_actor = cha;
			endtime = delay;
		}
		
		public Creature getActor()
		{
			return _actor;
		}
	}
	
	private void addObject(DecayTask decay)
	{
		synchronized (decayTasks_lock)
		{
			if (_decayTasksSize >= _decayTasks.length)
			{
				final DecayTask[] temp = new DecayTask[_decayTasks.length * 2];
				for (int i = 0; i < _decayTasksSize; i++)
				{
					temp[i] = _decayTasks[i];
				}
				_decayTasks = temp;
			}
			
			_decayTasks[_decayTasksSize] = decay;
			_decayTasksSize++;
		}
	}

	private void removeObject(Creature actor)
	{
		synchronized (decayTasks_lock)
		{
			if (_decayTasksSize > 1)
			{
				int k = -1;
				for (int i = 0; i < _decayTasksSize; i++)
				{
					if (_decayTasks[i].getActor() == actor)
					{
						k = i;
					}
				}
				if (k > -1)
				{
					_decayTasks[k] = _decayTasks[_decayTasksSize - 1];
					_decayTasks[_decayTasksSize - 1] = null;
					_decayTasksSize--;
				}
			}
			else if (_decayTasksSize == 1 && _decayTasks[0].getActor() == actor)
			{
				_decayTasks[0] = null;
				_decayTasksSize = 0;
			}
		}
	}
	
	public static final DecayTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DecayTaskManager _instance = new DecayTaskManager();
	}
}