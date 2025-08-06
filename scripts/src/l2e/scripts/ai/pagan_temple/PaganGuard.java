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
package l2e.scripts.ai.pagan_temple;

import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 05.12.2021
 */
public class PaganGuard extends Mystic
{
	public PaganGuard(Attackable actor)
	{
		super(actor);
		
		actor.setIsNoRndWalk(true);
		actor.setIsImmobilized(true);
	}

	@Override
	protected void movementDisable()
	{
		final Attackable actor = getActiveChar();
		final Creature target = getAttackTarget();
		if (target != null && actor.getAggroList().get(target) != null && Math.sqrt(actor.getDistanceSq(target)) >= 600)
		{
			actor.getAggroList().remove(target);
			return;
		}
		super.movementDisable();
	}
}