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
package l2e.scripts.ai.isle_of_prayer;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 21.09.2018
 */
public class WaterDragonDetractor extends Fighter
{
	public WaterDragonDetractor(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		if (killer != null)
		{
			final var player = killer.getActingPlayer();
			if (player != null)
			{
				final var actor = getActiveChar();
				actor.dropSingleItem(player, 9689, 1);
				if (Rnd.chance(10))
				{
					actor.dropSingleItem(player, 9595, 1);
				}
			}
		}
		super.onEvtDead(killer);
	}
}
