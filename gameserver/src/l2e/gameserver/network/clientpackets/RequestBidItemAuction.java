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

import l2e.gameserver.instancemanager.ItemAuctionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.itemauction.ItemAuction;
import l2e.gameserver.model.items.itemauction.ItemAuctionInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;

public final class RequestBidItemAuction extends GameClientPacket
{
	private int _instanceId;
	private long _bid;
	
	@Override
	protected final void readImpl()
	{
		_instanceId = super.readD();
		_bid = super.readQ();
	}

	@Override
	protected final void runImpl()
	{
		final Player activeChar = super.getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if ((_bid < 0) || (_bid > PcInventory.MAX_ADENA))
		{
			return;
		}
		
		final ItemAuctionInstance instance = ItemAuctionManager.getInstance().getManagerInstance(_instanceId);
		if (instance != null)
		{
			final ItemAuction auction = instance.getCurrentAuction();
			if (auction != null)
			{
				auction.registerBid(activeChar, _bid);
			}
		}
	}
}