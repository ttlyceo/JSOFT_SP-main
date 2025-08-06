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
package l2e.gameserver.model.reward;

import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.items.instance.ItemInstance;

public class RewardItemResult
{
	private final int _itemId;
	private long _count;
	private boolean _isAdena;
	
	public RewardItemResult(int itemId)
	{
		_itemId = itemId;
		_count = 1;
	}

	public RewardItemResult(int itemId, long count)
	{
		_itemId = itemId;
		_count = count;
	}

	public RewardItemResult setCount(long count)
	{
		_count = count;
		return this;
	}

	public int getId()
	{
		return _itemId;
	}

	public long getCount()
	{
		return _count;
	}

	public boolean isAdena()
	{
		return _isAdena;
	}

	public void setIsAdena(boolean val)
	{
		_isAdena = val;
	}

	public ItemInstance createItem()
	{
		if (_count < 1)
		{
			return null;
		}

		final ItemInstance item = ItemsParser.getInstance().createItem(_itemId);
		if (item != null)
		{
			item.setCount(_count);
			return item;
		}
		return null;
	}
}
