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

import l2e.gameserver.Config;
import l2e.gameserver.model.Party.messageType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExManagePartyRoomMember;
import l2e.gameserver.network.serverpackets.JoinParty;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestAnswerJoinParty extends GameClientPacket
{
	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}

		player.isntAfk();

		final Player requestor = player.getActiveRequester();
		if (requestor == null)
		{
			return;
		}

		requestor.sendPacket(new JoinParty(_response));

		if (_response == 1)
		{
			if (requestor.isInParty())
			{
				if (requestor.getParty().getMemberCount() >= Config.PARTY_LIMIT)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PARTY_FULL);
					player.sendPacket(sm);
					requestor.sendPacket(sm);
					return;
				}
			}
			player.joinParty(requestor.getParty());
			
			final MatchingRoom requestorRoom = requestor.getMatchingRoom();
			
			if (requestorRoom != null)
			{
				requestorRoom.addMember(player);
				final ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, requestorRoom, 1);
				for (final Player member : requestorRoom.getPlayers())
				{
					if (member != null)
					{
						member.sendPacket(packet);
					}
				}
				player.broadcastCharInfo();
			}
		}
		else if (_response == -1)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_SET_TO_REFUSE_PARTY_REQUEST);
			sm.addPcName(player);
			requestor.sendPacket(sm);

			if (requestor.isInParty() && (requestor.getParty().getMemberCount() == 1))
			{
				requestor.getParty().removePartyMember(requestor, messageType.None);
			}
		}
		else
		{
			if (requestor.isInParty() && (requestor.getParty().getMemberCount() == 1))
			{
				requestor.getParty().removePartyMember(requestor, messageType.None);
			}
		}

		if (requestor.isInParty())
		{
			requestor.getParty().setPendingInvitation(false);
		}

		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}
}