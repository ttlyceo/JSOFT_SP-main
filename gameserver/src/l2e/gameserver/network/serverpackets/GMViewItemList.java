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
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.items.instance.ItemInstance;

public class GMViewItemList extends GameServerPacket
{
	private final int _size;
	private final ItemInstance[] _items;
	private final int _limit;
	private final String _name;
	
	public GMViewItemList(Player cha, ItemInstance[] items, int size)
	{
		_size = size;
		_items = items;
		_name = cha.getName(null);
		_limit = cha.getInventoryLimit();
	}

	public GMViewItemList(PetInstance cha, ItemInstance[] items, int size)
	{
		_size = size;
		_items = items;
		_name = cha.getName(null);
		_limit = cha.getInventoryLimit();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeS(_name);
		writeD(_limit);
		writeH(0x01);
		writeH(_size);
		for (final ItemInstance temp : _items)
		{
			if (!temp.isQuestItem())
			{
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
				writeD(temp.isAugmented() ? temp.getAugmentation().getAugmentationId() : 0x00);
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
		}
	}
}