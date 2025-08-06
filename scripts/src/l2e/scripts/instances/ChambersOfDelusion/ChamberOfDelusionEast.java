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
public final class ChamberOfDelusionEast extends Chamber
{
	private static final Location[] _enterCoords = new Location[]
	{
		new Location(-122368, -218972, -6720),
		new Location(-122352, -218044, -6720),
		new Location(-122368, -220220, -6720),
		new Location(-121440, -218444, -6720),
		new Location(-121424, -220124, -6720)
	};

	private ChamberOfDelusionEast(String name, String descr)
	{
		super(name, descr, 127, 32658, 32664, 32668, 25690, 18838, "east_chamber_box");
		_coords = _enterCoords;
	}
	
	public static void main(String[] args)
	{
		new ChamberOfDelusionEast(ChamberOfDelusionEast.class.getSimpleName(), "instances");
	}
}