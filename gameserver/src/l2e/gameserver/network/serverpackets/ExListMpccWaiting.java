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
import java.util.List;

import l2e.commons.apache.StringUtils;
import l2e.gameserver.instancemanager.MatchingRoomManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;

public class ExListMpccWaiting extends GameServerPacket
{
	private static final int ITEMS_PER_PAGE = 10;
	private final int _page;
	private final List<MatchingRoom> _list;
	
	public ExListMpccWaiting(Player player, int page, int location, boolean allLevels)
	{
		final int first = (page - 1) * ITEMS_PER_PAGE;
		final int firstNot = page * ITEMS_PER_PAGE;
		
		final List<MatchingRoom> temp = MatchingRoomManager.getInstance().getMatchingRooms(MatchingRoom.CC_MATCHING, location, allLevels, player);
		_page = page;
		_list = new ArrayList<>(ITEMS_PER_PAGE);
		
		for (int i = 0; i < temp.size(); i++)
		{
			if (i < first || i >= firstNot)
			{
				continue;
			}
			_list.add(temp.get(i));
		}
	}
	
	@Override
	public void writeImpl()
	{
		writeD(_page);
		writeD(_list.size());
		for (final MatchingRoom room : _list)
		{
			writeD(room.getId());
			writeS(room.getTopic());
			writeD(room.getMemberCount());
			writeD(room.getMinLevel());
			writeD(room.getMaxLevel());
			writeD(room.getLeader().getParty().getCommandChannel().getPartys().size());
			writeD(room.getMaxMembersSize());
			writeS(room.getLeader() == null ? StringUtils.EMPTY : room.getLeader().getName(null));
		}
	}
}
