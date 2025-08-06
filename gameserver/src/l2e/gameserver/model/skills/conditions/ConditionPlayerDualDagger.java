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

import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Stats;

public class ConditionPlayerDualDagger extends Condition
{
	private final boolean _val;
	
	public ConditionPlayerDualDagger(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		var canUse = false;
		final var player = env.getPlayer();
		final var item = env.getItem();
		if (player == null)
		{
			canUse = false;
		}
		
		if (player != null && (player.calcStat(Stats.DUAL_DAGGER, 0, null, null) > 0 || (player.isInFightEvent() && item != null && item.isEventItem())))
		{
			canUse = true;
		}
		return (_val == canUse);
	}
}