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
import l2e.gameserver.model.actor.Creature;

public final class MoveToLocation extends GameServerPacket
{
	private final int _charObjId;
	private final Location _location, _destination;

	public MoveToLocation(Creature cha)
	{
		_charObjId = cha.getObjectId();
		_location = cha.getLocation();
		_destination = cha.getDestination();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_charObjId);
		writeD(_destination.getX());
		writeD(_destination.getY());
		writeD(_destination.getZ());
		writeD(_location.getX());
		writeD(_location.getY());
		writeD(_location.getZ());
	}
}