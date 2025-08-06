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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.instancemanager.BoatManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.BoatInstance;
import l2e.gameserver.network.serverpackets.GetOnVehicle;

public final class RequestGetOnVehicle extends GameClientPacket
{
	private int _boatId;
	private Location _pos;
	
	@Override
	protected void readImpl()
	{
		int x, y, z;
		_boatId = readD();
		x = readD();
		y = readD();
		z = readD();
		_pos = new Location(x, y, z);
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		BoatInstance boat;
		if (activeChar.isInBoat())
		{
			boat = activeChar.getBoat();
			if (boat.getObjectId() != _boatId)
			{
				sendActionFailed();
				return;
			}
		}
		else
		{
			boat = BoatManager.getInstance().getBoat(_boatId);
			if ((boat == null) || boat.isMoving())
			{
				sendActionFailed();
				return;
			}
		}
		
		activeChar.setInVehiclePosition(_pos);
		activeChar.setVehicle(boat);
		activeChar.broadcastPacket(new GetOnVehicle(activeChar.getObjectId(), boat.getObjectId(), _pos));
		
		activeChar.setXYZ(boat.getX(), boat.getY(), boat.getZ());
		activeChar.revalidateZone(true);
	}
}