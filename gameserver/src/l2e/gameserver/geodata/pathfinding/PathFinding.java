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
package l2e.gameserver.geodata.pathfinding;

import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.geodata.pathfinding.cellnodes.CellPathFinding;
import l2e.gameserver.geodata.pathfinding.empty.EmptyPathFinding;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.entity.Reflection;

public abstract class PathFinding
{
	private static final class SingletonHolder
	{
		static
		{
			if (Config.PATHFIND_BOOST)
			{
				INSTANCE = new CellPathFinding();
			}
			else
			{
				INSTANCE = new EmptyPathFinding();
			}
		}
		protected static final PathFinding INSTANCE;
	}

	public abstract void load();

	public static PathFinding getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public abstract boolean pathNodesExist(short regionoffset);
	
	public abstract List<AbstractNodeLoc> findPath(GameObject actor, int x, int y, int z, int tx, int ty, int tz, Reflection ref, boolean playable, boolean isDebugPF);
	
	public short getNodePos(int geo_pos)
	{
		return (short) (geo_pos >> 3);
	}
	
	public short getNodeBlock(int node_pos)
	{
		return (short) (node_pos % 256);
	}
	
	public byte getRegionX(int node_pos)
	{
		return (byte) ((node_pos >> 8) + World.TILE_X_MIN);
	}
	
	public byte getRegionY(int node_pos)
	{
		return (byte) ((node_pos >> 8) + World.TILE_Y_MIN);
	}
	
	public short getRegionOffset(byte rx, byte ry)
	{
		return (short) ((rx << 5) + ry);
	}
	
	public int calculateWorldX(short node_x)
	{
		return World.MAP_MIN_X + (node_x * 128) + 48;
	}
	
	public int calculateWorldY(short node_y)
	{
		return World.MAP_MIN_Y + (node_y * 128) + 48;
	}
	
	public String[] getStat()
	{
		return null;
	}
}
