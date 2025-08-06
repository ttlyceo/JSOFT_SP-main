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

import l2e.commons.threading.SteppingRunnableQueueManager;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;

public class EffectTaskManager extends SteppingRunnableQueueManager
{
	private final static long TICK = 250L;

	private static int _randomizer;

	private final static EffectTaskManager[] _instances = new EffectTaskManager[10 * Config.EFFECT_TASK_MANAGER_COUNT];
	static
	{
		for(int i = 0; i < _instances.length; i++)
		{
			_instances[i] = new EffectTaskManager();
		}
	}

	public final static EffectTaskManager getInstance()
	{
		return _instances[_randomizer++ & (_instances.length - 1)];
	}

	private EffectTaskManager()
	{
		super(TICK);
		
		ThreadPoolManager.getInstance().scheduleAtFixedRate(this, Rnd.get(TICK), TICK);
		ThreadPoolManager.getInstance().scheduleAtFixedRate(() ->
		{
			purge();
			if(_randomizer > 10000000)
			{
				_randomizer = 0;
			}
		}, 30000L + 1000L * _randomizer++, 30000L);
	}

	public CharSequence getStats(int num)
	{
		return _instances[num].getStats();
	}
}