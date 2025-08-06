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

public final class TargetSelected extends GameServerPacket
{
	private final int _objectId;
	private final int _targetObjId;
	private final Location _loc;

	public TargetSelected(int objectId, int targetId, Location location)
	{
		_objectId = objectId;
		_targetObjId = targetId;
		_loc = location;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_objectId);
		writeD(_targetObjId);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(0x00);
	}
}