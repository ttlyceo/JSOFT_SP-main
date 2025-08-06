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

public class ExBrAgathionEnergyInfo extends GameServerPacket
{
	private final int _size;
	private ItemInstance[] _itemList = null;
	
	public ExBrAgathionEnergyInfo(int size, ItemInstance... item)
	{
		_itemList = item;
		_size = size;
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_size);
		for (final ItemInstance item : _itemList)
		{
			if (item == null || item.getItem().getAgathionMaxEnergy() < 0)
			{
				continue;
			}
			writeD(item.getObjectId());
			writeD(item.getDisplayId());
			writeD(0x200000);
			writeD(item.getAgathionEnergy());
			writeD(item.getItem().getAgathionMaxEnergy());
		}
	}
}