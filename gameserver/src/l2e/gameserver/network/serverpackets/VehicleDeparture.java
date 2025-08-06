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
package l2e.gameserver.network.serverpackets;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.instance.BoatInstance;

public class VehicleDeparture extends GameServerPacket
{
	private final int _objId, _moveSpeed, _rotationSpeed;
	private final Location _destination;

	public VehicleDeparture(BoatInstance boat)
	{
		_objId = boat.getObjectId();
		_destination = boat.getDestination();
		_moveSpeed = (int) boat.getStat().getMoveSpeed();
		_rotationSpeed = boat.getStat().getRotationSpeed();
	}

	@Override
	protected void writeImpl()
	{
		writeD(_objId);
		writeD(_moveSpeed);
		writeD(_rotationSpeed);
		writeD(_destination.getX());
		writeD(_destination.getY());
		writeD(_destination.getZ());

	}
}