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
package l2e.scripts.ai;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 21.09.2018
 */
public class Elpy extends Fighter
{
	public Elpy(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();

		if (attacker != null)
		{
			if ((actor.getId() >= 18150) && (actor.getId() <= 18157))
			{
				final Location pos = Location.findPointToStay(actor, 40, 60, false);
				if (pos != null)
				{
					actor.setRunning();
					moveTo(new Location(pos.getX(), pos.getY(), pos.getZ()));
				}
			}
			else
			{
				if (Rnd.chance(50))
				{
					final Location pos = Location.findPointToStay(actor, 150, 200, false);
					if (pos != null)
					{
						actor.setRunning();
						moveTo(new Location(pos.getX(), pos.getY(), pos.getZ()));
					}
				}
			}
		}
	}

	@Override
	public boolean checkAggression(Creature target)
	{
		return false;
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}
}
