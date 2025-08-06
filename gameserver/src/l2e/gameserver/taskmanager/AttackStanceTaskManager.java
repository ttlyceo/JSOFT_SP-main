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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.CubicInstance;
import l2e.gameserver.network.serverpackets.AutoAttackStop;

public class AttackStanceTaskManager
{
	protected static final Logger _log = LoggerFactory.getLogger(AttackStanceTaskManager.class);
	
	protected static final Map<Creature, Long> _attackStanceTasks = new ConcurrentHashMap<>();
	
	protected AttackStanceTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new FightModeScheduler(), 0, 1000);
	}

	public void addAttackStanceTask(Creature actor)
	{
		if (actor != null)
		{
			if (actor.isPlayable())
			{
				final Player player = actor.getActingPlayer();
				for (final CubicInstance cubic : player.getCubics().values())
				{
					if (cubic.getId() != CubicInstance.LIFE_CUBIC)
					{
						cubic.doAction();
					}
				}
			}
			_attackStanceTasks.put(actor, System.currentTimeMillis());
		}
	}
	
	public void removeAttackStanceTask(Creature actor)
	{
		if (actor != null)
		{
			if (actor.isSummon())
			{
				actor = actor.getActingPlayer();
			}
			_attackStanceTasks.remove(actor);
		}
	}
	
	public boolean hasAttackStanceTask(Creature actor)
	{
		if (actor != null)
		{
			if (actor.isSummon())
			{
				actor = actor.getActingPlayer();
			}
			return _attackStanceTasks.containsKey(actor);
		}
		return false;
	}
	
	protected class FightModeScheduler implements Runnable
	{
		@Override
		public void run()
		{
			final long current = System.currentTimeMillis();
			try
			{
				final Iterator<Entry<Creature, Long>> iter = _attackStanceTasks.entrySet().iterator();
				Entry<Creature, Long> e;
				Creature actor;
				while (iter.hasNext())
				{
					e = iter.next();
					if ((current - e.getValue()) > 15000)
					{
						actor = e.getKey();
						if (actor != null)
						{
							actor.broadcastPacket(new AutoAttackStop(actor.getObjectId()));
							actor.getAI().setAutoAttacking(false);
							if (actor.isPlayer() && actor.hasSummon())
							{
								actor.getSummon().broadcastPacket(new AutoAttackStop(actor.getSummon().getObjectId()));
							}
						}
						iter.remove();
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("Error in FightModeScheduler: " + e.getMessage(), e);
			}
		}
	}
	
	public static AttackStanceTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AttackStanceTaskManager _instance = new AttackStanceTaskManager();
	}
}