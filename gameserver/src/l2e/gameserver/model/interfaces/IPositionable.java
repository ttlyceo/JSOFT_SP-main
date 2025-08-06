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
package l2e.gameserver.model.interfaces;

import l2e.gameserver.model.Location;

public interface IPositionable extends ILocational
{
	public void setX(int x);
	
	public void setY(int y);
	
	public void setZ(int z);
	
	public boolean setXYZ(int x, int y, int z);
	
	public boolean setXYZ(ILocational loc);
	
	public void setHeading(int heading);
	
	public boolean setLocation(Location loc);
}