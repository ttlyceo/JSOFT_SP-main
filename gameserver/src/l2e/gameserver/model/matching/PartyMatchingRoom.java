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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExClosePartyRoom;
import l2e.gameserver.network.serverpackets.ExPartyRoomMember;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.PartyRoomInfo;

public class PartyMatchingRoom extends MatchingRoom
{
	public PartyMatchingRoom(Player leader, int minLevel, int maxLevel, int maxMemberSize, int lootType, String topic)
	{
		super(leader, minLevel, maxLevel, maxMemberSize, lootType, topic);
		leader.broadcastUserInfo(true);
	}
	
	@Override
	public SystemMessageId notValidMessage()
	{
		return SystemMessageId.CANT_ENTER_PARTY_ROOM;
	}
	
	@Override
	public SystemMessageId enterMessage()
	{
		return SystemMessageId.C1_ENTERED_PARTY_ROOM;
	}
	
	@Override
	public SystemMessageId exitMessage(boolean toOthers, boolean kick)
	{
		if (toOthers)
		{
			return kick ? SystemMessageId.C1_KICKED_FROM_PARTY_ROOM : SystemMessageId.C1_LEFT_PARTY_ROOM;
		}
		else
		{
			return kick ? SystemMessageId.OUSTED_FROM_PARTY_ROOM : SystemMessageId.PARTY_ROOM_EXITED;
		}
	}
	
	@Override
	public SystemMessageId closeRoomMessage()
	{
		return SystemMessageId.PARTY_ROOM_DISBANDED;
	}
	
	@Override
	public SystemMessageId changeLeaderMessage()
	{
		return SystemMessageId.PARTY_ROOM_LEADER_CHANGED;
	}
	
	@Override
	public GameServerPacket closeRoomPacket()
	{
		return ExClosePartyRoom.STATIC;
	}
	
	@Override
	public GameServerPacket infoRoomPacket()
	{
		return new PartyRoomInfo(this);
	}
	
	@Override
	public GameServerPacket addMemberPacket(Player member, Player active)
	{
		return membersPacket(member);
	}
	
	@Override
	public GameServerPacket removeMemberPacket(Player member, Player active)
	{
		return membersPacket(member);
	}
	
	@Override
	public GameServerPacket updateMemberPacket(Player member, Player active)
	{
		return membersPacket(member);
	}
	
	@Override
	public GameServerPacket membersPacket(Player active)
	{
		return new ExPartyRoomMember(this, active);
	}
	
	@Override
	public int getType()
	{
		return PARTY_MATCHING;
	}
	
	@Override
	public int getMemberType(Player member)
	{
		return member.equals(_leader) ? ROOM_MASTER : member.getParty() != null && _leader.getParty() == member.getParty() ? PARTY_MEMBER : WAIT_PLAYER;
	}
}
