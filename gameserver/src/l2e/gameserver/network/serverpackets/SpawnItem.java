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

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.items.instance.ItemInstance;

public final class SpawnItem extends GameServerPacket
{
	private final int _objectId;
	private int _itemId;
	private final int _x, _y, _z;
	private int _stackable;
	private long _count;

	public SpawnItem(GameObject obj)
	{
		_objectId = obj.getObjectId();
		_x = obj.getX();
		_y = obj.getY();
		_z = obj.getZ();

		if (obj instanceof ItemInstance)
		{
			final ItemInstance item = (ItemInstance) obj;
			_itemId = item.getDisplayId();
			_stackable = item.isStackable() ? 0x01 : 0x00;
			_count = item.getCount();
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_objectId);
		writeD(_itemId);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_stackable);
		writeQ(_count);
		writeD(0x00);
		writeD(0x00);
	}
}