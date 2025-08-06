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
package l2e.gameserver.model.petition;

import java.util.Collection;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.HashIntObjectMap;

public class PetitionMainGroup extends PetitionGroup
{
	private final IntObjectMap<PetitionSection> _subGroups = new HashIntObjectMap<>();
	
	public PetitionMainGroup(int id)
	{
		super(id);
	}
	
	public void addSubGroup(PetitionSection subGroup)
	{
		_subGroups.put(subGroup.getId(), subGroup);
	}
	
	public PetitionSection getSubGroup(int val)
	{
		return _subGroups.get(val);
	}
	
	public Collection<PetitionSection> getSubGroups()
	{
		return _subGroups.valueCollection();
	}
}