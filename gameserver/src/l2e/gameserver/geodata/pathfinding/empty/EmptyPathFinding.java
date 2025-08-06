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
package l2e.gameserver.geodata.pathfinding.empty;

import java.util.Collections;
import java.util.List;

import l2e.gameserver.geodata.pathfinding.AbstractNodeLoc;
import l2e.gameserver.geodata.pathfinding.PathFinding;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.entity.Reflection;

public final class EmptyPathFinding extends PathFinding
{
	public EmptyPathFinding()
	{
	}

	@Override
	public void load()
	{
	}

	@Override
	public boolean pathNodesExist(short regionoffset)
	{
		return false;
	}

	@Override
	public List<AbstractNodeLoc> findPath(GameObject actor, int x, int y, int z, int tx, int ty, int tz, Reflection ref, boolean playable, boolean isDebugPF)
	{
		return Collections.emptyList();
	}
}