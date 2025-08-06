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

import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.model.matching.PartyMatchingRoom;

public class RequestManagePartyRoom extends GameClientPacket
{
	private int _lootDist;
	private int _maxMembers;
	private int _minLevel;
	private int _maxLevel;
	private int _roomId;
	private String _roomTitle;
	
	@Override
	protected void readImpl()
	{
		_roomId = readD();
		_maxMembers = readD();
		_minLevel = readD();
		_maxLevel = readD();
		_lootDist = readD();
		_roomTitle = readS();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}

		final Party party = player.getParty();
		if (party != null && party.getLeader() != player)
		{
			return;
		}
		
		MatchingRoom room = player.getMatchingRoom();
		if (room == null)
		{
			room = new PartyMatchingRoom(player, _minLevel, _maxLevel, _maxMembers, _lootDist, _roomTitle);
			if (party != null)
			{
				for (final Player member : party.getMembers())
				{
					if (member != null && member != player)
					{
						room.addMemberForce(member);
					}
				}
			}
		}
		else if (room.getId() == _roomId && room.getType() == MatchingRoom.PARTY_MATCHING && room.getLeader() == player)
		{
			room.setMinLevel(_minLevel);
			room.setMaxLevel(_maxLevel);
			room.setMaxMemberSize(_maxMembers);
			room.setTopic(_roomTitle);
			room.setLootType(_lootDist);
			room.broadCast(room.infoRoomPacket());
		}
	}
}