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
package l2e.gameserver.model.holders;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;

public class SummonRequestHolder
{
	private final Player _target;
	private final Skill _skill;
	private final boolean _isAdminRecall;

	public SummonRequestHolder(Player destination, Skill skill, boolean isAdminRecall)
	{
		_target = destination;
		_skill = skill;
		_isAdminRecall = isAdminRecall;
	}
	
	public Player getTarget()
	{
		return _target;
	}

	public Skill getSkill()
	{
		return _skill;
	}
	
	public boolean isAdminRecall()
	{
		return _isAdminRecall;
	}
}