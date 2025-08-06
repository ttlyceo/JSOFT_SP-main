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
package l2e.gameserver.model.items.itemauction;

import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.stats.StatsSet;

public final class AuctionItem
{
	private final int _auctionItemId;
	private final int _auctionLength;
	private final long _auctionInitBid;
	
	private final int _itemId;
	private final long _itemCount;
	private final StatsSet _itemExtra;

	public AuctionItem(final int auctionItemId, final int auctionLength, final long auctionInitBid, final int itemId, final long itemCount, final StatsSet itemExtra)
	{
		_auctionItemId = auctionItemId;
		_auctionLength = auctionLength;
		_auctionInitBid = auctionInitBid;
		
		_itemId = itemId;
		_itemCount = itemCount;
		_itemExtra = itemExtra;
	}
	
	public final boolean checkItemExists()
	{
		final Item item = ItemsParser.getInstance().getTemplate(_itemId);
		if (item == null)
		{
			return false;
		}
		return true;
	}
	
	public final int getAuctionItemId()
	{
		return _auctionItemId;
	}
	
	public final int getAuctionLength()
	{
		return _auctionLength;
	}
	
	public final long getAuctionInitBid()
	{
		return _auctionInitBid;
	}
	
	public final int getId()
	{
		return _itemId;
	}
	
	public final long getCount()
	{
		return _itemCount;
	}
	
	public final ItemInstance createNewItemInstance()
	{
		final ItemInstance item = ItemsParser.getInstance().createItem("ItemAuction", _itemId, _itemCount, null, null);
		
		item.setEnchantLevel(item.getDefaultEnchantLevel());
		
		final int augmentationId = _itemExtra.getInteger("augmentation_id", 0);
		if (augmentationId > 0)
		{
			item.setAugmentation(new Augmentation(augmentationId));
		}
		return item;
	}
}