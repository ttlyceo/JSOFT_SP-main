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
package l2e.fake.model;

import l2e.gameserver.model.Location;

public class WalkNode
{
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _stayIterations;
	
	public WalkNode(int x, int y, int z, int stayIterations)
	{
		_x = x;
		_y = y;
		_z = z;
		_stayIterations = stayIterations;
	}
	
	public int getX()
	{
		return _x;
	}
	
	public int getY()
	{
		return _y;
	}
	
	public int getZ()
	{
		return _z;
	}
	
	public int getStayIterations()
	{
		return _stayIterations;
	}
	
	public Location getLocation()
	{
		return new Location(_x, _y, _z);
	}
}