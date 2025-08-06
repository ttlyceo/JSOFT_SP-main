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

import l2e.gameserver.model.actor.instance.DoorInstance;

public final class DoorStatusUpdate extends GameServerPacket
{
	private final DoorInstance _door;
	
	public DoorStatusUpdate(DoorInstance door)
	{
		_door = door;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_door.getObjectId());
		writeD(_door.getOpen() ? 0x00 : 0x01);
		writeD(_door.getDamage());
		writeD(_door.isEnemy() ? 0x01 : 0x00);
		writeD(_door.getDoorId());
		writeD((int) _door.getCurrentHp());
		writeD((int) _door.getMaxHp());
	}
}