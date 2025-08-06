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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.items.ItemInfo;
import l2e.gameserver.model.items.instance.ItemInstance;

public class PetInventoryUpdate extends GameServerPacket
{
	private final List<ItemInfo> _items;

	public PetInventoryUpdate(List<ItemInfo> items)
	{
		_items = items;
	}

	public PetInventoryUpdate()
	{
		this(new ArrayList<ItemInfo>());
	}

	public void addItem(ItemInstance item)
	{
		_items.add(new ItemInfo(item));
	}

	public void addNewItem(ItemInstance item)
	{
		_items.add(new ItemInfo(item, 1));
	}

	public void addModifiedItem(ItemInstance item)
	{
		_items.add(new ItemInfo(item, 2));
	}

	public void addRemovedItem(ItemInstance item)
	{
		_items.add(new ItemInfo(item, 3));
	}

	public void addItems(List<ItemInstance> items)
	{
		for (final ItemInstance item : items)
		{
			_items.add(new ItemInfo(item));
		}
	}

	@Override
	protected final void writeImpl()
	{
		final int count = _items.size();
		writeH(count);
		for (final ItemInfo item : _items)
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
	}
}