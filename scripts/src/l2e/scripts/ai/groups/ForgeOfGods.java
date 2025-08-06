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
package l2e.scripts.ai.groups;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.NpcUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

public class ForgeOfGods extends Fighter
{
	private static final int[] RANDOM_SPAWN_MOBS =
	{
	        18799, 18800, 18801, 18802, 18803
	};
	
	private static final int[] FOG_MOBS =
	{
	        22634, 22635, 22636, 22637, 22638, 22639, 22640, 22641, 22642, 22643, 22644, 22645, 22646, 22647, 22648, 22649
	};
	
	public ForgeOfGods(Attackable actor)
	{
		super(actor);
		
		if (ArrayUtils.contains(RANDOM_SPAWN_MOBS, actor.getId()))
		{
			actor.setIsImmobilized(true);
		}
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final var actor = getActiveChar();
		if (actor == null)
		{
			return;
		}
		
		if (ArrayUtils.contains(FOG_MOBS, getActiveChar().getId()))
		{
			final int chance = actor.getTemplate().getParameter("lavasaurusSpawnChance", 0);
			if (Rnd.chance(chance))
			{
				final var npc = NpcUtils.spawnSingle(RANDOM_SPAWN_MOBS[Rnd.get(RANDOM_SPAWN_MOBS.length)], getActiveChar().getLocation());
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, Rnd.get(1, 100));
			}
		}
		super.onEvtDead(killer);
	}
}