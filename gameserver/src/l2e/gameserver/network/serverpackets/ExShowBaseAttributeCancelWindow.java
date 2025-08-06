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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;

public class ExShowBaseAttributeCancelWindow extends GameServerPacket
{
	private final ItemInstance[] _items;
	private long _price;
	
	public ExShowBaseAttributeCancelWindow(Player player)
	{
		_items = player.getInventory().getElementItems();
	}

	@Override
	protected void writeImpl()
	{
		writeD(_items.length);
		for (final ItemInstance item : _items)
		{
			writeD(item.getObjectId());
			writeQ(getPrice(item));
		}
	}
	
	private long getPrice(ItemInstance item)
	{
		switch (item.getItem().getCrystalType())
		{
			case Item.CRYSTAL_S :
				if (item.getItem() instanceof Weapon)
				{
					_price = 50000;
				}
				else
				{
					_price = 40000;
				}
				break;
			case Item.CRYSTAL_S80 :
				if (item.getItem() instanceof Weapon)
				{
					_price = 100000;
				}
				else
				{
					_price = 80000;
				}
				break;
			case Item.CRYSTAL_S84 :
				if (item.getItem() instanceof Weapon)
				{
					_price = 200000;
				}
				else
				{
					_price = 160000;
				}
				break;
		}
		return _price;
	}
}