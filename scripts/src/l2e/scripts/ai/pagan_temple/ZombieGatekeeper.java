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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Rework by LordWinter 13.11.2020
 */
public class ZombieGatekeeper extends Fighter
{
	public ZombieGatekeeper(Attackable actor)
	{
		super(actor);
		
		actor.setIsImmobilized(true);
	}
	
	@Override
	public boolean checkAggression(Creature target)
	{
		final Attackable actor = getActiveChar();
		if (target == null || actor.isDead() || actor.getAI().getIntention() != CtrlIntention.ACTIVE)
		{
			return false;
		}
		
		if (target.isAlikeDead() || !target.isPlayer() || !target.isInRangeZ(actor.getSpawnedLoc(), actor.getAggroRange()) || !GeoEngine.getInstance().canSeeTarget(actor, target))
		{
			return false;
		}
		
		if (target.getInventory().getItemByItemId(8067) != null || target.getInventory().getItemByItemId(8064) != null)
		{
			return false;
		}
		
		if (getIntention() != CtrlIntention.ATTACK)
		{
			actor.addDamageHate(target, 0, 1);
			actor.getAI().setIntention(CtrlIntention.ATTACK, target);
		}
		return true;
	}
}