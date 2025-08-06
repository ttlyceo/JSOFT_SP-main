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
package l2e.scripts.ai.kamaloka;

import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 10.12.2018
 */
public class KnightMontagnarFollower extends Fighter
{
	public KnightMontagnarFollower(Attackable actor)
	{
		super(actor);
		actor.setIsInvul(true);
	}
	
	@Override
	protected void onEvtAggression(Creature attacker, int aggro)
	{
		if (aggro < 1000000)
		{
			return;
		}
		super.onEvtAggression(attacker, aggro);
	}
}