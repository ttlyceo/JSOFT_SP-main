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
package l2e.scripts.instances.ChambersOfDelusion;

import l2e.gameserver.model.Location;

/**
 * Rework by LordWinter 14.11.2020
 */
public final class ChamberOfDelusionSouth extends Chamber
{
	private static final Location[] _enterCoords = new Location[]
	{
		new Location(-122368, -207820, -6720),
		new Location(-122368, -206940, -6720),
		new Location(-122368, -209116, -6720),
		new Location(-121456, -207356, -6720),
		new Location(-121440, -209004, -6720)
	};

	private ChamberOfDelusionSouth(String name, String descr)
	{
		super(name, descr, 129, 32660, 32674, 32678, 25692, 18838, "south_chamber_box");
		_coords = _enterCoords;
	}
	
	public static void main(String[] args)
	{
		new ChamberOfDelusionSouth(ChamberOfDelusionSouth.class.getSimpleName(), "instances");
	}
}