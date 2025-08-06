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

import l2e.gameserver.model.matching.MatchingRoom;

public class PartyRoomInfo extends GameServerPacket
{
	private final int _id;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _lootDist;
	private final int _maxMembers;
	private final int _location;
	private final String _title;
	
	public PartyRoomInfo(MatchingRoom room)
	{
		_id = room.getId();
		_minLevel = room.getMinLevel();
		_maxLevel = room.getMaxLevel();
		_lootDist = room.getLootType();
		_maxMembers = room.getMaxMembersSize();
		_location = room.getLocationId();
		_title = room.getTopic();
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_id);
		writeD(_maxMembers);
		writeD(_minLevel);
		writeD(_maxLevel);
		writeD(_lootDist);
		writeD(_location);
		writeS(_title);
	}
}