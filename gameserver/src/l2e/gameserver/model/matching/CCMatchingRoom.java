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
package l2e.gameserver.model.matching;

import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExDissmissMpccRoom;
import l2e.gameserver.network.serverpackets.ExManageMpccRoomMember;
import l2e.gameserver.network.serverpackets.ExMpccRoomInfo;
import l2e.gameserver.network.serverpackets.ExMpccRoomMember;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class CCMatchingRoom extends MatchingRoom
{
	public CCMatchingRoom(Player leader, int minLevel, int maxLevel, int maxMemberSize, int lootType, String topic)
	{
		super(leader, minLevel, maxLevel, maxMemberSize, lootType, topic);
		
		leader.sendPacket(SystemMessageId.THE_COMMAND_CHANNEL_MATCHING_ROOM_WAS_CREATED);
	}
	
	@Override
	public SystemMessageId notValidMessage()
	{
		return SystemMessageId.YOU_CANNOT_ENTER_THE_COMMAND_CHANNEL_MATCHING_ROOM_BECAUSE_YOU_DO_NOT_MEET_THE_REQUIREMENTS;
	}
	
	@Override
	public SystemMessageId enterMessage()
	{
		return SystemMessageId.C1_ENTERED_THE_COMMAND_CHANNEL_MATCHING_ROOM;
	}
	
	@Override
	public SystemMessageId exitMessage(boolean toOthers, boolean kick)
	{
		if (!toOthers)
		{
			return kick ? SystemMessageId.YOU_WERE_EXPELLED_FROM_THE_COMMAND_CHANNEL_MATCHING_ROOM : SystemMessageId.YOU_EXITED_FROM_THE_COMMAND_CHANNEL_MATCHING_ROOM;
		}
		else
		{
			return null;
		}
	}
	
	@Override
	public SystemMessageId closeRoomMessage()
	{
		return SystemMessageId.THE_COMMAND_CHANNEL_MATCHING_ROOM_WAS_CANCELLED;
	}
	
	@Override
	public SystemMessageId changeLeaderMessage()
	{
		return null;
	}
	
	@Override
	public GameServerPacket closeRoomPacket()
	{
		return ExDissmissMpccRoom.STATIC;
	}
	
	@Override
	public GameServerPacket infoRoomPacket()
	{
		return new ExMpccRoomInfo(this);
	}
	
	@Override
	public GameServerPacket addMemberPacket(Player member, Player active)
	{
		return new ExManageMpccRoomMember(ExManageMpccRoomMember.ADD_MEMBER, this, active);
	}
	
	@Override
	public GameServerPacket removeMemberPacket(Player member, Player active)
	{
		return new ExManageMpccRoomMember(ExManageMpccRoomMember.REMOVE_MEMBER, this, active);
	}
	
	@Override
	public GameServerPacket updateMemberPacket(Player member, Player active)
	{
		return new ExManageMpccRoomMember(ExManageMpccRoomMember.UPDATE_MEMBER, this, active);
	}
	
	@Override
	public GameServerPacket membersPacket(Player active)
	{
		return new ExMpccRoomMember(this, active);
	}
	
	@Override
	public int getType()
	{
		return CC_MATCHING;
	}
	
	@Override
	public void disband()
	{
		final Party party = _leader.getParty();
		if (party != null)
		{
			final CommandChannel commandChannel = party.getCommandChannel();
			if (commandChannel != null)
			{
				commandChannel.setMatchingRoom(null);
			}
		}
		
		super.disband();
	}
	
	@Override
	public int getMemberType(Player member)
	{
		final Party party = _leader.getParty();
		final CommandChannel commandChannel = party != null ? party.getCommandChannel() : null;
		if (member == _leader)
		{
			return MatchingRoom.UNION_LEADER;
		}
		else if (member.getParty() == null)
		{
			return MatchingRoom.WAIT_NORMAL;
		}
		else if (member.getParty() == party || (commandChannel != null && commandChannel.getPartys().contains(member.getParty())))
		{
			return MatchingRoom.UNION_PARTY;
		}
		else if (member.getParty() != null)
		{
			return MatchingRoom.WAIT_PARTY;
		}
		else
		{
			return MatchingRoom.WAIT_NORMAL;
		}
	}
}
