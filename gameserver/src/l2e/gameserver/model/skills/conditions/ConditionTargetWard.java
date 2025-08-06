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

import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.TerritoryWard;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.stats.Env;

public class ConditionTargetWard extends Condition
{
	private final boolean _val;

	public ConditionTargetWard(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		final Creature target = env.getTarget();
		boolean canCast = true;
		
		if (TerritoryWarManager.getInstance().getHQForClan(env.getPlayer().getClan()) != target)
		{
			canCast = false;
		}
		
		final TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(env.getPlayer());
		if (ward == null)
		{
			canCast = false;
		}
		return (_val == canCast);
	}
}