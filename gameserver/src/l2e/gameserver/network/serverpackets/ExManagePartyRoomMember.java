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

import java.util.List;

import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;

public class ExManagePartyRoomMember extends GameServerPacket
{
	private final int _type;
	private final PartyRoomMemberInfo _memberInfo;

	public ExManagePartyRoomMember(Player player, MatchingRoom room, int type)
	{
		_type = type;
		_memberInfo = (new PartyRoomMemberInfo(player, room.getMemberType(player)));
	}

	static class PartyRoomMemberInfo
	{
		public final int objectId;
		public final int classId;
		public final int level;
		public final int location;
		public final int memberType;
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
		writeD(_memberInfo.objectId);
		writeS(_memberInfo.name);
		writeD(_memberInfo.classId);
		writeD(_memberInfo.level);
		writeD(_memberInfo.location);
		writeD(_memberInfo.memberType);
		writeD(_memberInfo.instanceReuses.size());
		for (final int i : _memberInfo.instanceReuses)
		{
			writeD(i);
		}
	}
}