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
import l2e.gameserver.model.actor.templates.items.Item;

public class RewardItem
{
	public final int _itemId;
	public long _count;
	private final Item _template;
	
	public RewardItem(int itemId)
	{
		_itemId = itemId;
		_count = 1;
		_template = ItemsParser.getInstance().getTemplate(_itemId);
	}

	public boolean isHerb()
	{
		final Item item = ItemsParser.getInstance().getTemplate(_itemId);
		if (item == null)
		{
			return false;
		}
		return item.is_ex_immediate_effect();
	}
	
	public boolean isAdena()
	{
		final Item item = ItemsParser.getInstance().getTemplate(_itemId);
		if (item == null)
		{
			return false;
		}
		return item.isAdena();
	}
	
	public Item getTemplate()
	{
		return _template;
	}
}
