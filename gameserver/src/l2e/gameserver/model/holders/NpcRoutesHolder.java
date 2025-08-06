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

import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.interfaces.ILocational;

public class NpcRoutesHolder
{
	private final Map<String, String> _correspondences;

	public NpcRoutesHolder()
	{
		_correspondences = new HashMap<>();
	}

	public void addRoute(String routeName, Location loc)
	{
		_correspondences.put(getUniqueKey(loc), routeName);
	}

	public String getRouteName(Npc npc)
	{
		if (npc.getSpawn() != null)
		{
			final String key = getUniqueKey(npc.getSpawn().getLocation());
			return _correspondences.containsKey(key) ? _correspondences.get(key) : "";
		}
		return "";
	}

	private String getUniqueKey(ILocational loc)
	{
		return (loc.getX() + "-" + loc.getY() + "-" + loc.getZ());
	}
}