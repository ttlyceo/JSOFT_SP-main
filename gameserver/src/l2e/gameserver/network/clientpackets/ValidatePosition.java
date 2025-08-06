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

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.GetOnVehicle;

public class ValidatePosition extends GameClientPacket
{
	private final Location _loc = new Location();
	private int _vehicle;

	@Override
	protected void readImpl()
	{
		_loc.setX(readD());
		_loc.setY(readD());
		_loc.setZ(readD());
		_loc.setHeading(readD());
		_vehicle = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null || activeChar.isTeleporting() || activeChar.inObserverMode())
		{
			return;
		}
		
		final int realX = activeChar.getX();
		final int realY = activeChar.getY();
		final int realZ = activeChar.getZ();
		
		if ((_loc.getX() == 0) && (_loc.getY() == 0))
		{
			if (realX != 0)
			{
				return;
			}
		}

		int dx, dy, dz;
		double diffSq;
		
		if (activeChar.isInBoat())
		{
			if (Config.COORD_SYNCHRONIZE)
			{
				dx = _loc.getX() - activeChar.getInVehiclePosition().getX();
				dy = _loc.getY() - activeChar.getInVehiclePosition().getY();
				diffSq = ((dx * dx) + (dy * dy));
				if (diffSq > 250000)
				{
					sendPacket(new GetOnVehicle(activeChar.getObjectId(), _vehicle, activeChar.getInVehiclePosition()));
				}
			}
			return;
		}
		if (activeChar.isInAirShip())
		{
			return;
		}
		
		if (activeChar.isFalling(_loc.getZ()))
		{
			activeChar.correctFallingPosition(_loc);
			activeChar.setClientLoc(_loc.setH(activeChar.getHeading()));
			return;
		}
		
		if (!Config.GEODATA && !GeoEngine.getInstance().hasGeo(activeChar.getX(), activeChar.getY()))
		{
			activeChar.setXYZ(activeChar.getX(), activeChar.getY(), _loc.getZ());
		}
		
		dx = _loc.getX() - realX;
		dy = _loc.getY() - realY;
		dz = _loc.getZ() - realZ;
		diffSq = ((dx * dx) + (dy * dy));
		
		if (activeChar.isFlyingMounted() && (_loc.getX() > World.GRACIA_MAX_X))
		{
			activeChar.untransform();
		}
		
		if (activeChar.isFlying() || activeChar.isInWater(activeChar))
		{
			activeChar.setXYZ(realX, realY, _loc.getZ());
			if (diffSq > 90000)
			{
				activeChar.validateLocation(0);
			}
		}
		else if (diffSq < 360000)
		{
			if (!Config.GEODATA)
			{
				activeChar.setXYZ(realX, realY, _loc.getZ());
				return;
			}
			
			if (!Config.COORD_SYNCHRONIZE)
			{
				if (!activeChar.isMoving() || !activeChar.validateMovementHeading(_loc.getHeading()))
				{
					if (diffSq < 2500)
					{
						activeChar.setXYZ(realX, realY, _loc.getZ());
					}
					else
					{
						activeChar.setXYZ(_loc.getX(), _loc.getY(), _loc.getZ());
					}
				}
				else
				{
					activeChar.setXYZ(realX, realY, _loc.getZ());
				}
				return;
			}
			
			if ((Util.calculateDistance(_loc.getX(), _loc.getY(), realX, realY) > 512) || (Math.abs(dz) > 200))
			{
				activeChar.validateLocation(0);
			}
		}
		activeChar.setClientLoc(_loc);
	}
}