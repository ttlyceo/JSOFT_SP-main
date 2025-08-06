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

public final class ItemList extends GameServerPacket
{
	private final PcInventory _inventory;
	private final ItemInstance[] _items;
	private final boolean _showWindow;
	private final int _size;

	public ItemList(PcInventory inventory, int size, ItemInstance[] items, boolean showWindow)
	{
		_inventory = inventory;
		_size = size;
		_items = items;
		_showWindow = showWindow;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeH(_showWindow ? 0x01 : 0x00);
		writeH(_size);
		for (final ItemInstance temp : _items)
		{
			if (temp == null || temp.getItem() == null || temp.isQuestItem())
			{
				continue;
			}
			writeD(temp.getObjectId());
			writeD(temp.getDisplayId());
			writeD(temp.getLocationSlot());
			writeQ(temp.getCount());
			writeH(temp.getItem().getType2());
			writeH(temp.getCustomType1());
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(temp.getItem().getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			if (temp.isAugmented())
			{
				writeD(temp.getAugmentation().getAugmentationId());
			}
			else
			{
				writeD(0x00);
			}
			writeD(temp.getMana());
			writeD(temp.isTimeLimitedItem() ? (int) (temp.getRemainingTime() / 1000) : -9999);
			writeH(temp.getAttackElementType());
			writeH(temp.getAttackElementPower());
			for (byte i = 0; i < 6; i++)
			{
				writeH(temp.getElementDefAttr(i));
			}
			for (final int op : temp.getEnchantOptions())
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