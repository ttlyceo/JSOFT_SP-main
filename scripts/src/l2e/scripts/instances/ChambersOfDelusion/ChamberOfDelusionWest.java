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
public final class ChamberOfDelusionWest extends Chamber
{
	private static final Location[] _enterCoords = new Location[]
	{
		new Location(-108960, -218892, -6720),
		new Location(-108976, -218028, -6720),
		new Location(-108960, -220204, -6720),
		new Location(-108032, -218428, -6720),
		new Location(-108032, -220140, -6720)
	};

	private ChamberOfDelusionWest(String name, String descr)
	{
		super(name, descr, 128, 32659, 32669, 32673, 25691, 18838, "west_chamber_box");
		_coords = _enterCoords;
	}
	
	public static void main(String[] args)
	{
		new ChamberOfDelusionWest(ChamberOfDelusionWest.class.getSimpleName(), "instances");
	}
}