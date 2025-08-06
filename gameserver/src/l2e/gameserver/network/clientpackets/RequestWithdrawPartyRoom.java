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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;

public final class RequestWithdrawPartyRoom extends GameClientPacket
{
	private int _roomId;

	@Override
	protected void readImpl()
	{
		_roomId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final MatchingRoom room = player.getMatchingRoom();
		if (room == null || room.getId() != _roomId || room.getType() != MatchingRoom.PARTY_MATCHING)
		{
			return;
		}
		
		final int type = room.getMemberType(player);
		if (type == MatchingRoom.ROOM_MASTER || type == MatchingRoom.PARTY_MEMBER)
		{
			player.setMatchingRoomWindowOpened(false);
			return;
		}
		room.removeMember(player, false);
	}
}