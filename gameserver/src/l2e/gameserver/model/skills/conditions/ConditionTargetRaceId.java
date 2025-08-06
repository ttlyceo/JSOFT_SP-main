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

import java.util.ArrayList;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.stats.Env;

public class ConditionTargetRaceId extends Condition
{
	private final ArrayList<Integer> _raceIds;

	public ConditionTargetRaceId(ArrayList<Integer> raceId)
	{
		_raceIds = raceId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		final var target = env.getTarget();
		if (target == null)
		{
			return false;
		}
		if (target.isNpc())
		{
			return (_raceIds.contains(((Npc) target).getTemplate().getRace().ordinal() + 1));
		}
		else if (target.isSummon())
		{
			return (_raceIds.contains(((Summon) target).getTemplate().getRace().ordinal() + 1));
		}
		return false;
	}
}