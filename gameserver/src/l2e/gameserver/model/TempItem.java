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
package l2e.gameserver.model;

import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;

public final class TempItem
{
	private final int _itemId;
	private int _quantity;
	private final int _referencePrice;
	private final Item _item;
	
	public TempItem(ItemInstance item, int quantity)
	{
		super();
		_itemId = item.getId();
		_quantity = quantity;
		_item = item.getItem();
		_referencePrice = item.getReferencePrice();
	}
	
	public int getQuantity()
	{
		return _quantity;
	}
	
	public void setQuantity(int quantity)
	{
		_quantity = quantity;
	}
	
	public int getReferencePrice()
	{
		return _referencePrice;
	}
	
	public int getId()
	{
		return _itemId;
	}
	
	public Item getItem()
	{
		return _item;
	}
}