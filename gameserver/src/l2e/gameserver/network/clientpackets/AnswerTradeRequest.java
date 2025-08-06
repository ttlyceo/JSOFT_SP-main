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

import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.TradeDone;

public final class AnswerTradeRequest extends GameClientPacket
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

		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			sendActionFailed();
			return;
		}

		final Player partner = player.getActiveRequester();
		if (partner == null)
		{
			player.sendPacket(new TradeDone(0));
			SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.sendPacket(msg);
			player.setActiveRequester(null);
			msg = null;
			return;
		}
		else if (GameObjectsStorage.getPlayer(partner.getObjectId()) == null)
		{
			player.sendPacket(new TradeDone(0));
			SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.sendPacket(msg);
			player.setActiveRequester(null);
			msg = null;
			return;
		}
		
		if (partner.isActionsDisabled())
		{
			player.sendPacket(new TradeDone(0));
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
			msg.addString(partner.getName(null));
			player.sendPacket(msg);
			player.setActiveRequester(null);
			player.sendActionFailed();
			return;
		}

		if ((_response == 1) && !partner.isRequestExpired())
		{
			player.startTrade(partner);
		}
		else
		{
			SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_DENIED_TRADE_REQUEST);
			msg.addString(player.getName(null));
			partner.sendPacket(msg);
			msg = null;
		}
		player.setActiveRequester(null);
		partner.onTransactionResponse();
	}
}