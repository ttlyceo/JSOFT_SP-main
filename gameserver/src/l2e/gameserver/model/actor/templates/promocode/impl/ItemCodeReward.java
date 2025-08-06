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
package l2e.gameserver.model.actor.templates.promocode.impl;

import org.w3c.dom.NamedNodeMap;

import l2e.commons.util.Util;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;

public class ItemCodeReward extends AbstractCodeReward
{
	private final int _itemId;
	private final long _itemCount;
	private final int _enchant;
	private final int _augmentId;
	private final String _elementals;
	private final int _durability;
	
	public ItemCodeReward(NamedNodeMap attr)
	{
		_itemId = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
		_itemCount = attr.getNamedItem("count") != null ? Long.parseLong(attr.getNamedItem("count").getNodeValue()) : 1;
		_enchant = attr.getNamedItem("enchant") != null ? Integer.parseInt(attr.getNamedItem("enchant").getNodeValue()) : 0;
		_augmentId = attr.getNamedItem("augmentId") != null ? Integer.parseInt(attr.getNamedItem("augmentId").getNodeValue()) : -1;
		_elementals = attr.getNamedItem("elementals") != null ? attr.getNamedItem("elementals").getNodeValue() : null;
		_durability = attr.getNamedItem("durability") != null ? Integer.parseInt(attr.getNamedItem("durability").getNodeValue()) : 0;
	}
	
	@Override
	public void giveReward(Player player)
	{
		final ItemInstance item = player.getInventory().addItem("Code", _itemId, _itemCount, player, null);
		if (item != null && !item.isStackable())
		{
			if (_enchant != 0)
			{
				item.setEnchantLevel(_enchant);
			}
			
			if (_augmentId != -1)
			{
				item.setAugmentation(new Augmentation(_augmentId));
			}
			
			if (_elementals != null && !_elementals.isEmpty())
			{
				final String[] elements = _elementals.split(";");
				for (final String el : elements)
				{
					final String[] element = el.split(":");
					if (element != null)
					{
						item.setElementAttr(Byte.parseByte(element[0]), Integer.parseInt(element[1]));
					}
				}
			}
			item.setCount(_itemCount);
			if (_durability > 0)
			{
				item.setTime(_durability);
			}
			item.updateDatabase();
		}
	}
	
	@Override
	public String getIcon()
	{
		return Util.getItemIcon(_itemId);
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public long getCount()
	{
		return _itemCount;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
	
	public int getAugmentId()
	{
		return _augmentId;
	}
	
	public String getElementals()
	{
		return _elementals;
	}
	
	public int getDurability()
	{
		return _durability;
	}
}