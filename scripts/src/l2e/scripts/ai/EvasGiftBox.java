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
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 22.11.2018
 */
public class EvasGiftBox extends Fighter
{
	public EvasGiftBox(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		final var actor = getActiveChar();
		if (killer != null)
		{
			final var player = killer.getActingPlayer();
			if ((player != null) && ((player.getFirstEffect(1073) != null) || (player.getFirstEffect(3141) != null) || (player.getFirstEffect(3252) != null)))
			{
				actor.dropSingleItem(player, Rnd.chance(50) ? 9692 : 9693, 1);
			}
		}
		super.onEvtDead(killer);
	}
}
