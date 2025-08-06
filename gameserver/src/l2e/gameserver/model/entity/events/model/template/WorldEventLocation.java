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
package l2e.gameserver.model.entity.events.model.template;

import l2e.gameserver.model.Location;

/**
 * Created by LordWinter 13.07.2020
 */
public class WorldEventLocation
{
	private final String _name;
	private final Location _loc;
	
	public WorldEventLocation(String name, Location loc)
	{
		_name = name;
		_loc = loc;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public Location getLocation()
	{
		return _loc;
	}
}