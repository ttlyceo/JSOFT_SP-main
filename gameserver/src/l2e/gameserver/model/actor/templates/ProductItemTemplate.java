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
package l2e.gameserver.model.actor.templates;

import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.templates.items.Item;

/**
 * Created by LordWinter 06.10.2011 Fixed by L2J Eternity-World
 */
public class ProductItemTemplate
{
	private final int _itemId;
	private final int _count;
	
	private final int _weight;
	private final boolean _dropable;
	
	public ProductItemTemplate(int item_id, int count)
	{
		_itemId = item_id;
		_count = count;
		
		final Item item = ItemsParser.getInstance().getTemplate(item_id);
		if (item != null)
		{
			_weight = item.getWeight();
			_dropable = item.isDropable();
		}
		else
		{
			_weight = 0;
			_dropable = true;
		}
	}

	public int getId()
	{
		return _itemId;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getWeight()
	{
		return _weight;
	}
	
	public boolean isDropable()
	{
		return _dropable;
	}
}