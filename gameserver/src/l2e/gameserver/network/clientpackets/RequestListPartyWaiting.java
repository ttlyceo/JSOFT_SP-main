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

import l2e.gameserver.instancemanager.MatchingRoomManager;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.CCMatchingRoom;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ListPartyWaiting;

public final class RequestListPartyWaiting extends GameClientPacket
{
	private int _page;
	private int _region;
	private int _allLevels;
	
	@Override
	protected void readImpl()
	{
		_page = readD();
		_region = readD();
		_allLevels = readD();
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
		final CommandChannel channel = party != null ? party.getCommandChannel() : null;

		if (channel != null && channel.getLeader() == player)
		{
			if (channel.getMatchingRoom() == null)
			{
				final CCMatchingRoom room = new CCMatchingRoom(player, 1, player.getLevel(), 50, party.getLootDistribution(), player.getName(null));
				channel.setMatchingRoom(room);
				for (final Party ccParty : player.getParty().getCommandChannel().getPartys())
				{
					for (final Player ccMember : ccParty.getMembers())
					{
						if (ccParty.isLeader(ccMember) && ccParty.getLeader() != player)
						{
							room.addMember(ccMember);
							ccMember.setMatchingRoomWindowOpened(true);
							ccMember.sendPacket(room.infoRoomPacket(), room.membersPacket(ccMember));
						}
					}
				}
			}
		}
		else if (channel != null && !channel.getPartys().contains(party))
		{
			player.sendPacket(SystemMessageId.THE_COMMAND_CHANNEL_AFFILIATED_PARTY_S_PARTY_MEMBER_CANNOT_USE_THE_MATCHING_SCREEN);
		}
		else if (party != null && !party.isLeader(player))
		{
			final MatchingRoom room = player.getMatchingRoom();
			if (room != null && room.getType() == MatchingRoom.PARTY_MATCHING)
			{
				player.setMatchingRoomWindowOpened(true);
				player.sendPacket(room.infoRoomPacket(), room.membersPacket(player));
			}
			else
			{
				player.sendPacket(SystemMessageId.CANT_VIEW_PARTY_ROOMS);
			}
		}
		else
		{
			if (party == null)
			{
				MatchingRoomManager.getInstance().addToWaitingList(player);
			}
			player.sendPacket(new ListPartyWaiting(_region, _allLevels == 1, _page, player));
		}
	}
}