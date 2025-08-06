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
public final class ChamberOfDelusionSquare extends Chamber
{
	private static final Location[] _enterCoords = new Location[]
	{
		new Location(-122368, -153388, -6688),
		new Location(-122368, -152524, -6688),
		new Location(-120480, -155116, -6688),
		new Location(-120480, -154236, -6688),
		new Location(-121440, -151212, -6688),
		new Location(-120464, -152908, -6688),
		new Location(-122368, -154700, -6688),
		new Location(-121440, -152908, -6688),
		new Location(-121440, -154572, -6688)
	};

	private ChamberOfDelusionSquare(int questId, String name, String descr)
	{
		super(name, descr, 131, 32662, 32684, 32692, 25694, 18820, "square_chamber_box");
		_coords = _enterCoords;
	}
	
	public static void main(String[] args)
	{
		new ChamberOfDelusionSquare(-1, ChamberOfDelusionSquare.class.getSimpleName(), "instances");
	}
}