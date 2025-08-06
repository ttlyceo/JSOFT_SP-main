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
import java.util.Collections;
import java.util.List;

import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;

public class ExPartyRoomMember extends GameServerPacket
{
	private final int _type;
	private List<PartyRoomMemberInfo> _members = Collections.emptyList();

	public ExPartyRoomMember(MatchingRoom room, Player activeChar)
	{
		_type = room.getMemberType(activeChar);
		_members = new ArrayList<>(room.getPlayers().size());
		for (final Player $member : room.getPlayers())
		{
			_members.add(new PartyRoomMemberInfo($member, room.getMemberType($member)));
		}
	}
	
	static class PartyRoomMemberInfo
	{
		public final int objectId, classId, level, location, memberType;
		public final String name;
		public final List<Integer> instanceReuses;

		public PartyRoomMemberInfo(Player member, int type)
		{
			objectId = member.getObjectId();
			name = member.getName(null);
			classId = member.getClassId().ordinal();
			level = member.getLevel();
			location = MapRegionManager.getInstance().getBBs(member.getLocation());
			memberType = type;
			instanceReuses = ReflectionManager.getInstance().getLockedReflectionList(member);
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_type);
		writeD(_members.size());
		for (final PartyRoomMemberInfo member_info : _members)
		{
			writeD(member_info.objectId);
			writeS(member_info.name);
			writeD(member_info.classId);
			writeD(member_info.level);
			writeD(member_info.location);
			writeD(member_info.memberType);
			writeD(member_info.instanceReuses.size());
			for (final int i : member_info.instanceReuses)
			{
				writeD(i);
			}
		}
	}
}