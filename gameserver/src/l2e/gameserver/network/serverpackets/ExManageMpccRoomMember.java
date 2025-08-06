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

import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;

public class ExManageMpccRoomMember extends GameServerPacket
{
	public static int ADD_MEMBER = 0;
	public static int UPDATE_MEMBER = 1;
	public static int REMOVE_MEMBER = 2;
	
	private final int _type;
	private final MpccRoomMemberInfo _memberInfo;

	public ExManageMpccRoomMember(int type, MatchingRoom room, Player target)
	{
		_type = type;
		_memberInfo = (new MpccRoomMemberInfo(target, room.getMemberType(target)));
	}
	
	static class MpccRoomMemberInfo
	{
		public final int objectId;
		public final int classId;
		public final int level;
		public final int location;
		public final int memberType;
		public final String name;

		public MpccRoomMemberInfo(Player member, int type)
		{
			objectId = member.getObjectId();
			name = member.getName(null);
			classId = member.getClassId().ordinal();
			level = member.getLevel();
			location = MapRegionManager.getInstance().getBBs(member.getLocation());
			memberType = type;
		}
	}

	@Override
	protected void writeImpl()
	{
		writeD(_type);
		writeD(_memberInfo.objectId);
		writeS(_memberInfo.name);
		writeD(_memberInfo.level);
		writeD(_memberInfo.classId);
		writeD(_memberInfo.location);
		writeD(_memberInfo.memberType);
	}
}