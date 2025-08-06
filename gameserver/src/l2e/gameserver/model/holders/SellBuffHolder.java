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
package l2e.gameserver.model.holders;

public final class SellBuffHolder
{
	private final int _id;
	private final int _level;
	private int _itemId;
	private long _price;
	
	public SellBuffHolder(int Id, int lvl, int itemId, long price)
	{
		_id = Id;
		_level = lvl;
		_itemId = itemId;
		_price = price;
	}
	
	public final int getId()
	{
		return _id;
	}

	public final int getLvl()
	{
		return _level;
	}
	
	public final void setPrice(long price)
	{
		_price = price;
	}
	
	public final long getPrice()
	{
		return _price;
	}
	
	public final void setItemId(int itemId)
	{
		_itemId = itemId;
	}
	
	public final int getItemId()
	{
		return _itemId;
	}
}