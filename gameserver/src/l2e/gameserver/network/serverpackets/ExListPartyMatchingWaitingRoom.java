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

import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.MatchingRoomManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Player;

public class ExListPartyMatchingWaitingRoom extends GameServerPacket
{
	private static final int ITEMS_PER_PAGE = 64;
	private final List<PartyMatchingWaitingInfo> _waitingList = new ArrayList<>(ITEMS_PER_PAGE);
	private final int _fullSize;
	
	public ExListPartyMatchingWaitingRoom(Player searcher, int minLevel, int maxLevel, int page, int[] classes)
	{
		final List<Player> temp = MatchingRoomManager.getInstance().getWaitingList(minLevel, maxLevel, classes);
		_fullSize = temp.size();
		
		final int first = Math.max((page - 1) * ITEMS_PER_PAGE, 0);
		final int firstNot = Math.min(page * ITEMS_PER_PAGE, _fullSize);
		for (int i = first; i < firstNot; i++)
		{
			_waitingList.add(new PartyMatchingWaitingInfo(temp.get(i)));
		}
	}

	static class PartyMatchingWaitingInfo
	{
		public final int classId, level, locationId;
		public final String name;
		public final List<Integer> instanceReuses;
		
		public PartyMatchingWaitingInfo(Player member)
		{
			name = member.getName(null);
			classId = member.getClassId().getId();
			level = member.getLevel();
			locationId = MapRegionManager.getInstance().getBBs(member.getLocation());
			instanceReuses = ReflectionManager.getInstance().getLockedReflectionList(member);
		}
	}

	@Override
	protected void writeImpl()
	{
		writeD(_fullSize);
		writeD(_waitingList.size());
		for (final PartyMatchingWaitingInfo waitingInfo : _waitingList)
		{
			writeS(waitingInfo.name);
			writeD(waitingInfo.classId);
			writeD(waitingInfo.level);
			writeD(waitingInfo.locationId);
			writeD(waitingInfo.instanceReuses.size());
			for (final int i : waitingInfo.instanceReuses)
			{
				writeD(i);
			}
		}
	}
}