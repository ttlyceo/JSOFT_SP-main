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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.collections.LazyArrayList;
import l2e.commons.util.Util;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.spawn.Spawner;

public class SpawnTaskManager
{
	private static final Logger _log = LoggerFactory.getLogger(SpawnTaskManager.class);
	
	private SpawnTask[] _spawnTasks = new SpawnTask[500];
	private int _spawnTasksSize = 0;
	private final Object spawnTasks_lock = new Object();

	public SpawnTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new SpawnScheduler(), 2000, 2000);
	}

	public void addSpawnTask(Npc actor, long interval)
	{
		removeObject(actor);
		addObject(new SpawnTask(actor, System.currentTimeMillis() + interval));
	}

	public class SpawnScheduler implements Runnable
	{
		@Override
		public void run()
		{
			if (_spawnTasksSize > 0)
			{
				try
				{
					final List<Npc> works = new LazyArrayList<>();
					synchronized (spawnTasks_lock)
					{
						final long current = System.currentTimeMillis();
						final int size = _spawnTasksSize;
						
						for (int i = size - 1; i >= 0; i--)
						{
							try
							{
								final SpawnTask container = _spawnTasks[i];
								
								if (container != null && container.endtime > 0 && current > container.endtime)
								{
									final Npc actor = container.getActor();
									if (actor != null && actor.getSpawn() != null)
									{
										works.add(actor);
									}
									
									container.endtime = -1;
								}
								
								if (container == null || container.getActor() == null || container.endtime < 0)
								{
									if (i == _spawnTasksSize - 1)
									{
										_spawnTasks[i] = null;
									}
									else
									{
										_spawnTasks[i] = _spawnTasks[_spawnTasksSize - 1];
										_spawnTasks[_spawnTasksSize - 1] = null;
									}
									
									if (_spawnTasksSize > 0)
									{
										_spawnTasksSize--;
									}
								}
							}
							catch (final Exception e)
							{
								_log.error("", e);
							}
						}
					}
					
					for (final Npc work : works)
					{
						final Spawner spawn = work.getSpawn();
						if (spawn == null)
						{
							continue;
						}
						spawn.decreaseScheduledCount();
						spawn.respawnNpc(work);
					}
				}
				catch (final Exception e)
				{
					_log.error("", e);
				}
			}
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("============= SpawnTask Manager Report ============\n\r");
		sb.append("Tasks count: ").append(_spawnTasksSize).append("\n\r");
		sb.append("Tasks dump:\n\r");

		final long current = System.currentTimeMillis();
		for(final SpawnTask container : _spawnTasks)
		{
			sb.append("Class/Name: ").append(container.getClass().getSimpleName()).append('/').append(container.getActor());
			sb.append(" spawn timer: ").append(Util.formatTime((int)(container.endtime - current))).append("\n\r");
		}

		return sb.toString();
	}

	private class SpawnTask
	{
		private final Npc _npc;
		public long endtime;

		SpawnTask(Npc cha, long delay)
		{
			_npc = cha;
			endtime = delay;
		}

		public Npc getActor()
		{
			return _npc;
		}
	}

	private void addObject(SpawnTask decay)
	{
		synchronized (spawnTasks_lock)
		{
			if(_spawnTasksSize >= _spawnTasks.length)
			{
				final SpawnTask[] temp = new SpawnTask[_spawnTasks.length * 2];
				for(int i = 0; i < _spawnTasksSize; i++)
				{
					temp[i] = _spawnTasks[i];
				}
				_spawnTasks = temp;
			}

			_spawnTasks[_spawnTasksSize] = decay;
			_spawnTasksSize++;
		}
	}

	public void removeObject(Npc actor)
	{
		synchronized (spawnTasks_lock)
		{
			if(_spawnTasksSize > 1)
			{
				int k = -1;
				for(int i = 0; i < _spawnTasksSize; i++)
				{
					if(_spawnTasks[i].getActor() == actor)
					{
						k = i;
					}
				}
				if(k > -1)
				{
					_spawnTasks[k] = _spawnTasks[_spawnTasksSize - 1];
					_spawnTasks[_spawnTasksSize - 1] = null;
					_spawnTasksSize--;
				}
			}
			else if(_spawnTasksSize == 1 && _spawnTasks[0].getActor() == actor)
			{
				_spawnTasks[0] = null;
				_spawnTasksSize = 0;
			}
		}
	}
	
	public static final SpawnTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnTaskManager _instance = new SpawnTaskManager();
	}
}