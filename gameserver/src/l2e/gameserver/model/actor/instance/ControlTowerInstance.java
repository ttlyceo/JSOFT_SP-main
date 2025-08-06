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
package l2e.gameserver.model.actor.instance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Tower;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.spawn.Spawner;

public class ControlTowerInstance extends Tower
{
	private List<Spawner> _guards;
	
	public ControlTowerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.ControlTowerInstance);
	}
	
	@Override
	protected void onDeath(Creature killer)
	{
		if (getCastle().getSiege().getIsInProgress())
		{
			getCastle().getSiege().killedCT(this);
			
			if ((_guards != null) && !_guards.isEmpty())
			{
				for (final Spawner spawn : _guards)
				{
					if (spawn == null)
					{
						continue;
					}
					try
					{
						spawn.stopRespawn();
					}
					catch (final Exception e)
					{
						_log.warn("Error at L2ControlTowerInstance", e);
					}
				}
				_guards.clear();
			}
		}
		super.onDeath(killer);
	}
	
	public void registerGuard(Spawner guard)
	{
		getGuards().add(guard);
	}
	
	private final List<Spawner> getGuards()
	{
		if (_guards == null)
		{
			synchronized (this)
			{
				if (_guards == null)
				{
					_guards = new CopyOnWriteArrayList<>();
				}
			}
		}
		return _guards;
	}
}