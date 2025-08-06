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
package l2e.gameserver.model.spawn;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.ControllableMobInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

public class SpawnGroup extends Spawner
{
	private final NpcTemplate _template;
	
	public SpawnGroup(NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		super(mobTemplate);
		
		_template = mobTemplate;
		setAmount(1);
	}

	public Npc doGroupSpawn()
	{
		try
		{
			if (_template.isType("Pet") || _template.isType("Minion"))
			{
				return null;
			}

			int newlocx = 0;
			int newlocy = 0;
			int newlocz = 0;

			if ((getX() == 0) && (getY() == 0))
			{
				if (getLocationId() == 0)
				{
					return null;
				}
				return null;
			}
			
			newlocx = getX();
			newlocy = getY();
			newlocz = getZ();

			final Npc mob = new ControllableMobInstance(IdFactory.getInstance().getNextId(), _template);
			
			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			
			if (getHeading() == -1)
			{
				mob.setHeading(Rnd.nextInt(61794));
			}
			else
			{
				mob.setHeading(getHeading());
			}
			
			mob.setSpawn(this);
			mob.spawnMe(newlocx, newlocy, newlocz);
			mob.onSpawn();
			
			if (Config.DEBUG)
			{
				_log.info("Spawned Mob Id: " + _template.getId() + " ,at: X: " + mob.getX() + " Y: " + mob.getY() + " Z: " + mob.getZ());
			}
			return mob;
			
		}
		catch (final Exception e)
		{
			_log.warn("NPC class not found: " + e.getMessage(), e);
			return null;
		}
	}
}