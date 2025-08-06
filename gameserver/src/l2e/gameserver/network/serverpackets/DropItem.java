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

public class DropItem extends GameServerPacket
{
	private final ItemInstance _item;
	private final int _charObjId;

	public DropItem(ItemInstance item, int playerObjId)
	{
		_item = item;
		_charObjId = playerObjId;
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_charObjId);
		writeD(_item.getObjectId());
		writeD(_item.getDisplayId());
		writeD(_item.getX());
		writeD(_item.getY());
		writeD(_item.getZ());
		writeD(_item.isStackable() ? 0x01 : 0x00);
		writeQ(_item.getCount());
		writeD(0x01);
	}
}