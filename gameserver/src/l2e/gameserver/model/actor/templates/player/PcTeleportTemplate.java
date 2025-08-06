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
package l2e.gameserver.model.actor.templates.player;

public class PcTeleportTemplate
{
	private final int _id;
	private int _locX;
	private int _locY;
	private int _locZ;
	private final String _name;
	
	public PcTeleportTemplate(int id, String name, int locX, int locY, int locZ)
	{
		_id = id;
		_name = name;
		_locX = locX;
		_locY = locY;
		_locZ = locZ;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getX()
	{
		return _locX;
	}
	
	public int getY()
	{
		return _locY;
	}
	
	public int getZ()
	{
		return _locZ;
	}
	
	public void setX(int x)
	{
		_locX = x;
	}
	
	public void setY(int y)
	{
		_locY = y;
	}
	
	public void setZ(int z)
	{
		_locZ = z;
	}
}