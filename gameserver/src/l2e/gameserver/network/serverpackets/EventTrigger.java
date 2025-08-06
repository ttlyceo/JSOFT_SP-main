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

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.model.actor.instance.DoorInstance;

public class EventTrigger extends GameServerPacket
{
	private final int _emitterId;
	private final boolean _opened;
	
	private static final int[] REVERSE_DOORS =
	{
	        16200023, 16200024, 16200025
	};
	
	public EventTrigger(DoorInstance door, boolean opened)
	{
		_emitterId = door.getEmitter();
		if (ArrayUtils.contains(REVERSE_DOORS, door.getDoorId()))
		{
			_opened = (!opened);
		}
		else
		{
			_opened = opened;
		}
	}
	
	public EventTrigger(int id, boolean opened)
	{
		_emitterId = id;
		_opened = opened;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_emitterId);
		writeD(_opened ? 0x00 : 0x01);
	}
}