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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import l2e.gameserver.instancemanager.MatchingRoomManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;

public class ListPartyWaiting extends GameServerPacket
{
	private static final int ITEMS_PER_PAGE = 16;
	private final Collection<MatchingRoom> _rooms = new ArrayList<>(ITEMS_PER_PAGE);
	private final int _page;
	
	public ListPartyWaiting(int region, boolean allLevels, int page, Player activeChar)
	{
		_page = page;
		final List<MatchingRoom> temp = MatchingRoomManager.getInstance().getMatchingRooms(MatchingRoom.PARTY_MATCHING, region, allLevels, activeChar);
		
		final int first = Math.max((page - 1) * ITEMS_PER_PAGE, 0);
		final int firstNot = Math.min(page * ITEMS_PER_PAGE, temp.size());
		for (int i = first; i < firstNot; i++)
		{
			_rooms.add(temp.get(i));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_page);
		writeD(_rooms.size());
		for (final MatchingRoom room : _rooms)
		{
			writeD(room.getId());
			writeS(room.getTopic());
			writeD(room.getLocationId());
			writeD(room.getMinLevel());
			writeD(room.getMaxLevel());
			writeD(room.getMaxMembersSize());
			writeS(room.getLeader() == null ? "None" : room.getLeader().getName(null));
			final Collection<Player> players = room.getPlayers();
			writeD(players.size());
			for (final Player player : players)
			{
				writeD(player.getClassId().getId());
				writeS(player.getName(null));
			}
		}
	}
}