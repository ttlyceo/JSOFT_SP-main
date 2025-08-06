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
package l2e.gameserver.model.entity.auction;

import l2e.gameserver.model.items.instance.ItemInstance;

public class Auction
{
	private final int _auctionId;
	private final int _sellerObjectId;
	private final String _sellerName;
	private final ItemInstance _item;
	private long _countToSell;
	private final int _priceItemId;
	private final long _pricePerItem;
	private final AuctionItemTypes _itemType;
	private final boolean _privateStore;

	public Auction(int id, int sellerObjectId, String sellerName, ItemInstance item, int priceItemId, long pricePerItem, long countToSell, AuctionItemTypes itemType, boolean privateStore)
	{
		_auctionId = id;
		_sellerObjectId = sellerObjectId;
		_sellerName = sellerName;
		_item = item;
		_priceItemId = priceItemId;
		_pricePerItem = pricePerItem;
		_countToSell = countToSell;
		_itemType = itemType;
		_privateStore = privateStore;
	}

	public int getAuctionId()
	{
		return _auctionId;
	}

	public int getSellerObjectId()
	{
		return _sellerObjectId;
	}

	public String getSellerName()
	{
		return _sellerName;
	}

	public ItemInstance getItem()
	{
		return _item;
	}

	public void setCount(long count)
	{
		_countToSell = count;
	}

	public long getCountToSell()
	{
		return _countToSell;
	}
	
	public int getPriceItemId()
	{
		return _priceItemId;
	}

	public long getPricePerItem()
	{
		return _pricePerItem;
	}

	public AuctionItemTypes getItemType()
	{
		return _itemType;
	}

	public boolean isPrivateStore()
	{
		return _privateStore;
	}
}