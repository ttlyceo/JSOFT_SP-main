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

import l2e.gameserver.model.items.ItemInfo;
import l2e.gameserver.model.items.itemauction.ItemAuction;
import l2e.gameserver.model.items.itemauction.ItemAuctionBid;
import l2e.gameserver.model.items.itemauction.ItemAuctionState;

public final class ExItemAuctionInfo extends GameServerPacket
{
	private final boolean _refresh;
	private final int _timeRemaining;
	private final ItemAuction _currentAuction;
	private final ItemAuction _nextAuction;
	
	public ExItemAuctionInfo(final boolean refresh, final ItemAuction currentAuction, final ItemAuction nextAuction)
	{
		if (currentAuction == null)
		{
			throw new NullPointerException();
		}
		
		if (currentAuction.getAuctionState() != ItemAuctionState.STARTED)
		{
			_timeRemaining = 0;
		}
		else
		{
			_timeRemaining = (int) (currentAuction.getFinishingTimeRemaining() / 1000);
		}
		
		_refresh = refresh;
		_currentAuction = currentAuction;
		_nextAuction = nextAuction;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(_refresh ? 0x00 : 0x01);
		writeD(_currentAuction.getReflectionId());

		final ItemAuctionBid highestBid = _currentAuction.getHighestBid();
		writeQ(highestBid != null ? highestBid.getLastBid() : _currentAuction.getAuctionInitBid());
		writeD(_timeRemaining);
		writeItemInfo(_currentAuction.getItemInfo());
		if (_nextAuction != null)
		{
			writeQ(_nextAuction.getAuctionInitBid());
			writeD((int) (_nextAuction.getStartingTime() / 1000));
			writeItemInfo(_nextAuction.getItemInfo());
		}
	}
	
	private final void writeItemInfo(final ItemInfo item)
	{
		writeD(item.getItem().getId());
		writeD(item.getItem().getId());
		writeD(item.getLocation());
		writeQ(item.getCount());
		writeH(item.getItem().getType2());
		writeH(item.getCustomType1());
		writeH(0x00);
		writeD(item.getItem().getBodyPart());
		writeH(item.getEnchant());
		writeH(item.getCustomType2());
		writeD(item.getAugmentationBonus());
		writeD(item.getMana());
		writeD(item.getTime());
		writeH(item.getAttackElementType());
		writeH(item.getAttackElementPower());
		for (byte i = 0; i < 6; i++)
		{
			writeH(item.getElementDefAttr(i));
		}
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
	}
}