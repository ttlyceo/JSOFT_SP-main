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
package l2e.gameserver.ai.guard;

import java.util.ArrayList;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.DefenderInstance;

public final class SpecialGuardAI extends GuardAI
{
	private final ArrayList<Integer> _allied;
	
	public SpecialGuardAI(DefenderInstance character)
	{
		super(character);

		_allied = new ArrayList<>();
	}
	
	public ArrayList<Integer> getAlly()
	{
		return _allied;
	}
	
	@Override
	protected boolean checkAggression(Creature target)
	{
		if (_allied.contains(target.getObjectId()))
		{
			return false;
		}
		return super.checkAggression(target);
	}
}