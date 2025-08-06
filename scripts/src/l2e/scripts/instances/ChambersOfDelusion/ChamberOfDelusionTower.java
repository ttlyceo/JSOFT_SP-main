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
public final class ChamberOfDelusionTower extends Chamber
{
	private static final Location[] _enterCoords = new Location[]
	{
		new Location(-108976, -153372, -6688),
		new Location(-108960, -152524, -6688),
		new Location(-107088, -155052, -6688),
		new Location(-107104, -154236, -6688),
		new Location(-108048, -151244, -6688),
		new Location(-107088, -152956, -6688),
		new Location(-108992, -154604, -6688),
		new Location(-108032, -152892, -6688),
		new Location(-108048, -154572, -6688)
	};

	private ChamberOfDelusionTower(String name, String descr)
	{
		super(name, descr, 132, 32663, 32693, 32701, 25695, 18823, "tower_chamber_box");
		_coords = _enterCoords;
	}
	
	public static void main(String[] args)
	{
		new ChamberOfDelusionTower(ChamberOfDelusionTower.class.getSimpleName(), "instances");
	}
}