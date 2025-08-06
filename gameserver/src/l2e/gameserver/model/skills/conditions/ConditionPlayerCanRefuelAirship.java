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
package l2e.gameserver.model.skills.conditions;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ControllableAirShipInstance;
import l2e.gameserver.model.stats.Env;

public class ConditionPlayerCanRefuelAirship extends Condition
{
	private final int _val;

	public ConditionPlayerCanRefuelAirship(int val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		boolean canRefuelAirship = true;
		final Player player = env.getPlayer();
		if ((player == null) || (player.getAirShip() == null) || !(player.getAirShip() instanceof ControllableAirShipInstance) || ((player.getAirShip().getFuel() + _val) > player.getAirShip().getMaxFuel()))
		{
			canRefuelAirship = false;
		}
		return canRefuelAirship;
	}
}