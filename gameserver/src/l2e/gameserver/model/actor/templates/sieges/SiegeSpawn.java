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
package l2e.gameserver.model.actor.templates.sieges;

import l2e.gameserver.model.Location;

public class SiegeSpawn
{
	private final Location _location;
	private final int _npcId;
	private final int _heading;
	private final int _castleId;
	private int _hp;
	
	public SiegeSpawn(int castleId, Location loc, int npcId)
	{
		_castleId = castleId;
		_location = loc;
		_heading = loc.getHeading();
		_npcId = npcId;
	}
	
	public SiegeSpawn(int castleId, Location loc, int npcId, int hp)
	{
		_castleId = castleId;
		_location = loc;
		_heading = loc.getHeading();
		_npcId = npcId;
		_hp = hp;
	}
	
	public SiegeSpawn(int castleId, int x, int y, int z, int heading, int npcId, int hp)
	{
		_castleId = castleId;
		_location = new Location(x, y, z, heading);
		_heading = heading;
		_npcId = npcId;
		_hp = hp;
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public int getHeading()
	{
		return _heading;
	}
	
	public int getHp()
	{
		return _hp;
	}
	
	public Location getLocation()
	{
		return _location;
	}
}