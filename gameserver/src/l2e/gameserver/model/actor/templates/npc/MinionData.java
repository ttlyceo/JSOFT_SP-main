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
package l2e.gameserver.model.actor.templates.npc;

import java.util.ArrayList;
import java.util.List;

public class MinionData
{
	private List<MinionTemplate> _minions = new ArrayList<>();

	public MinionData(MinionTemplate template)
	{
		_minions.add(template);
	}
	
	public MinionData(List<MinionTemplate> minions)
	{
		_minions = minions;
	}
	
	public List<MinionTemplate> getMinions()
	{
		return _minions;
	}
}