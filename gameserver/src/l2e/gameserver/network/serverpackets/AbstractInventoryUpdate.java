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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.model.items.ItemInfo;
import l2e.gameserver.model.items.instance.ItemInstance;

public abstract class AbstractInventoryUpdate extends GameServerPacket
{
	private final Map<Integer, ItemInfo> _items = new ConcurrentHashMap<>();
	
	public AbstractInventoryUpdate()
	{
	}
	
	public AbstractInventoryUpdate(ItemInstance item)
	{
		addItem(item);
	}
	
	public AbstractInventoryUpdate(List<ItemInfo> items)
	{
		for (final ItemInfo item : items)
		{
			_items.put(item.getObjectId(), item);
		}
	}
	
	public void addItem(ItemInstance item)
	{
		_items.put(item.getObjectId(), new ItemInfo(item));
	}
	
	public void addNewItem(ItemInstance item)
	{
		_items.put(item.getObjectId(), new ItemInfo(item, 1));
	}
	
	public void addModifiedItem(ItemInstance item)
	{
		_items.put(item.getObjectId(), new ItemInfo(item, 2));
	}
	
	public void addRemovedItem(ItemInstance item)
	{
		_items.put(item.getObjectId(), new ItemInfo(item, 3));
	}
	
	public void addItems(Collection<ItemInstance> items)
	{
		for (final ItemInstance item : items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item));
		}
	}
	
	public Collection<ItemInfo> getItems()
	{
		return _items.values();
	}
	
	protected final void writeItems()
	{
		writeH(_items.size());
		for (final var item : _items.values())
		{
			writeH(item.getChange());
			writeD(item.getObjectId());
			writeD(item.getItem().getDisplayId());
			writeD(item.getLocation());
			writeQ(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeH(item.getEquipped());
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
			for (final int op : item.getEnchantOptions())
			{
				writeH(op);
			}
		}
		checkAgathionItems();
	}
	
	private void checkAgathionItems()
	{
		final var player = getClient().getActiveChar();
		if (player != null)
		{
			int agathionItems = 0;
			final var allItem = player.getInventory().getItems();
			for (final var item : allItem)
			{
				if (item != null)
				{
					if (item.isEnergyItem())
					{
						agathionItems++;
					}
				}
			}
			
			if (agathionItems > 0)
			{
				player.sendPacket(new ExBrAgathionEnergyInfo(agathionItems, allItem));
			}
		}
	}
}