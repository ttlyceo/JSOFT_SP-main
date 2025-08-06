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
package l2e.scripts.vehicles;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.templates.VehicleTemplate;

public class SoAController extends AirShipController
{
	private static final int DOCK_ZONE = 50604;
	private static final int LOCATION = 103;
	private static final int CONTROLLER_ID = 32779;

	private static final VehicleTemplate[] ARRIVAL =
	{
	        new VehicleTemplate(-174946, 155306, 4605, 280, 2000), new VehicleTemplate(-174946, 155306, 3105, 280, 2000),
	};

	private static final VehicleTemplate[] DEPART =
	{
	        new VehicleTemplate(-175063, 155726, 4105, 280, 2000), new VehicleTemplate(-179546, 161425, 5105, 280, 2000)
	};

	private static final VehicleTemplate[][] TELEPORTS =
	{
	        {
	                new VehicleTemplate(-179438, 162776, 5129, 280, 2000)
			},
			{
	                new VehicleTemplate(-195357, 233430, 2500, 280, 2000)
			}
	};

	private static final int[] FUEL =
	{
	        0, 150
	};

	public SoAController(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(CONTROLLER_ID);
		addFirstTalkId(CONTROLLER_ID);
		addTalkId(CONTROLLER_ID);

		_dockZone = DOCK_ZONE;
		addEnterZoneId(DOCK_ZONE);
		addExitZoneId(DOCK_ZONE);

		_shipSpawnX = -178118;
		_shipSpawnY = 156415;
		_shipSpawnZ = 5809;
		_shipHeading = 62027;

		_oustLoc = new Location(-175689, 154160, 2712);

		_locationId = LOCATION;
		_arrivalPath = ARRIVAL;
		_departPath = DEPART;
		_teleportsTable = TELEPORTS;
		_fuelTable = FUEL;

		_movieId = 1004;

		validityCheck();
	}

	public static void main(String[] args)
	{
		new SoAController(-1, SoAController.class.getSimpleName(), "vehicles");
	}
}
