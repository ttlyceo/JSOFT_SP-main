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

public class Faction
{
	public final static String none = "none";
	public final static Faction NONE = new Faction(none);
	
	public final String _factionId;
	public int _factionRange;
	public List<Integer> ignoreId = new ArrayList<>();
	
	public Faction(String factionId)
	{
		_factionId = factionId;
	}
	
	public String getName()
	{
		return _factionId;
	}
	
	public void setRange(int factionRange)
	{
		_factionRange = factionRange;
	}
	
	public int getRange()
	{
		return _factionRange;
	}
	
	public void addIgnoreNpcId(int npcId)
	{
		ignoreId.add(npcId);
	}
	
	public boolean isIgnoreNpcId(int npcId)
	{
		return ignoreId.contains(npcId);
	}
	
	public boolean isNone()
	{
		return _factionId.isEmpty() || _factionId.equals(none);
	}

	public boolean equals(Faction faction)
	{
		if (isNone() || !faction.getName().equalsIgnoreCase(_factionId))
		{
			return false;
		}
		return true;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		if (o == null)
		{
			return false;
		}
		if (o.getClass() != this.getClass())
		{
			return false;
		}
		return equals((Faction) o);
	}
	
	@Override
	public String toString()
	{
		return isNone() ? none : _factionId;
	}
}