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

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.stats.Env;

public final class ConditionPeaceZone extends Condition
{
	protected final boolean _self;
	
	public ConditionPeaceZone(boolean self)
	{
		_self = self;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		final Creature target = _self ? env.getCharacter() : env.getTarget();
		return target != null && target.isPlayer() && !target.isInZonePeace();
	}
}