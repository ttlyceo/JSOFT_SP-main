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

import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.items.instance.ItemInstance;

public class ExChooseInventoryAttributeItem extends GameServerPacket
{
	private final int _itemId;
	private final byte _atribute;
	private final int _level;

	public ExChooseInventoryAttributeItem(ItemInstance item)
	{
		_itemId = item.getDisplayId();
		_atribute = Elementals.getItemElement(_itemId);
		if (_atribute == Elementals.NONE)
		{
			throw new IllegalArgumentException("Undefined Atribute item: " + item);
		}
		_level = Elementals.getMaxElementLevel(_itemId);
	}

	@Override
	protected void writeImpl()
	{
		writeD(_itemId);
		writeD(_atribute == Elementals.FIRE ? 1 : 0);
		writeD(_atribute == Elementals.WATER ? 1 : 0);
		writeD(_atribute == Elementals.WIND ? 1 : 0);
		writeD(_atribute == Elementals.EARTH ? 1 : 0);
		writeD(_atribute == Elementals.HOLY ? 1 : 0);
		writeD(_atribute == Elementals.DARK ? 1 : 0);
		writeD(_level);
	}
}