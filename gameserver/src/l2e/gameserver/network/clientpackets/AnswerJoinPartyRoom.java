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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.network.SystemMessageId;

public final class AnswerJoinPartyRoom extends GameClientPacket
{
	private int _response;

	@Override
	protected void readImpl()
	{
		if (_buf.hasRemaining())
		{
			_response = readD();
		}
		else
		{
			_response = 0;
		}
	}

	@Override
	protected void runImpl()
	{
		final Player player = getActiveChar();
		if (player == null)
		{
			return;
		}

		final Player partner = player.getActiveRequester();
		if (partner == null)
		{
			player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.setActiveRequester(null);
			return;
		}
		
		if (_response == 0)
		{
			player.setActiveRequester(null);
			partner.sendPacket(SystemMessageId.PARTY_MATCHING_REQUEST_NO_RESPONSE);
			return;
		}

		if (player.getMatchingRoom() != null)
		{
			player.setActiveRequester(null);
			return;
		}

		if ((_response == 1) && !partner.isRequestExpired())
		{
			final MatchingRoom room = partner.getMatchingRoom();
			if (room == null || room.getType() != MatchingRoom.PARTY_MATCHING)
			{
				return;
			}
			room.addMember(player);
		}
		else
		{
			partner.sendPacket(SystemMessageId.PARTY_MATCHING_REQUEST_NO_RESPONSE);
		}
		player.setActiveRequester(null);
		partner.onTransactionResponse();
	}
}