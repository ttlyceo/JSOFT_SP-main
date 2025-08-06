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

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.stats.Env;

public class ConditionPlayerRace extends Condition
{
	private final Race[] _races;
	
	public ConditionPlayerRace(Race[] races)
	{
		_races = races;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		if ((env.getCharacter() == null) || !env.getCharacter().isPlayer())
		{
			return false;
		}
		return ArrayUtils.contains(_races, env.getPlayer().getRace());
	}
}