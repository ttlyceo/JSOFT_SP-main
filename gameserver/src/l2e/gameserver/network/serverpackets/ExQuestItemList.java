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

import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;

public class ExQuestItemList extends GameServerPacket
{
	private final int _size;
	private final ItemInstance[] _items;
	private final PcInventory _inventory;

	public ExQuestItemList(PcInventory inv, int size, ItemInstance[] items)
	{
		_size = size;
		_items = items;
		_inventory = inv;
	}

	@Override
	protected void writeImpl()
	{
		writeH(_size);
		for (final ItemInstance item : _items)
		{
			if (item == null || item.getItem() == null || !item.isQuestItem())
			{
				continue;
			}
			
			writeD(item.getObjectId());
			writeD(item.getDisplayId());
			writeD(item.getLocationSlot());
			writeQ(item.getCount());
			writeD(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(item.getCustomType2());
			if (item.isAugmented())
			{
				writeD(item.getAugmentation().getAugmentationId());
			}
			else
			{
				writeD(0x00);
			}
			writeD(item.getMana());
			writeD(item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -9999);
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
		if (_inventory.hasInventoryBlock())
		{
			writeH(_inventory.getBlockItems().size());
			writeC(_inventory.getBlockMode());
			for (final int i : _inventory.getBlockItems())
			{
				writeD(i);
			}
		}
		else
		{
			writeH(0x00);
		}
	}
}