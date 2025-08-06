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
package l2e.gameserver.model.actor.templates;

public final class AirShipTeleportTemplate
{
	private final int _location;
	private final int[] _fuel;
	private final VehicleTemplate[][] _routes;
	
	public AirShipTeleportTemplate(int loc, int[] f, VehicleTemplate[][] r)
	{
		_location = loc;
		_fuel = f;
		_routes = r;
	}

	public int getLocation()
	{
		return _location;
	}

	public int[] getFuel()
	{
		return _fuel;
	}

	public VehicleTemplate[][] getRoute()
	{
		return _routes;
	}
}