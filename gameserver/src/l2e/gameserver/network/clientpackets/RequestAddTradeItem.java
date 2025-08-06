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
import l2e.gameserver.model.TradeItem;
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.TradeOtherAdd;
import l2e.gameserver.network.serverpackets.TradeOwnAdd;
import l2e.gameserver.network.serverpackets.TradeUpdate;

public final class RequestAddTradeItem extends GameClientPacket
{
	private int _tradeId;
	private int _objectId;
	private long _count;

	@Override
	protected void readImpl()
	{
		_tradeId = readD();
		_objectId = readD();
		_count = readQ();
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
		
		final TradeList trade = player.getActiveTradeList();
		if (trade == null)
		{
			_log.warn("Character: " + player.getName(null) + " requested item:" + _objectId + " add without active tradelist:" + _tradeId);
			return;
		}
		
		final Player partner = trade.getPartner();
		if ((partner == null) || (GameObjectsStorage.getPlayer(partner.getObjectId()) == null) || (partner.getActiveTradeList() == null))
		{
			if (partner != null)
			{
				_log.warn("Character:" + player.getName(null) + " requested invalid trade object: " + _objectId);
			}
			player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.cancelActiveTrade();
			return;
		}
		
		if (trade.isConfirmed() || partner.getActiveTradeList().isConfirmed())
		{
			player.sendPacket(SystemMessageId.CANNOT_ADJUST_ITEMS_AFTER_TRADE_CONFIRMED);
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			player.cancelActiveTrade();
			return;
		}
		
		if (!player.validateItemManipulation(_objectId, "trade"))
		{
			player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
			return;
		}
		
		final TradeItem item = trade.addItem(_objectId, _count);
		if (item != null)
		{
			player.sendPacket(new TradeOwnAdd(item), new TradeUpdate(player, item));
			trade.getPartner().sendPacket(new TradeOtherAdd(item));
		}
	}
}