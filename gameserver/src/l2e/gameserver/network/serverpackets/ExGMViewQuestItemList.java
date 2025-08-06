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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;

public class ExGMViewQuestItemList extends GameServerPacket
{
	private final int _size;
	private final ItemInstance[] _items;

	private final int _limit;
	private final String _name;

	public ExGMViewQuestItemList(Player player, ItemInstance[] items, int size)
	{
		_items = items;
		_size = size;
		_name = player.getName(null);
		_limit = Config.INVENTORY_MAXIMUM_QUEST_ITEMS;
	}

	@Override
	protected void writeImpl()
	{
		writeS(_name);
		writeD(_limit);
		writeH(_size);
		for (final ItemInstance temp : _items)
		{
			if (temp.isQuestItem())
			{
				writeItemInfo(temp);
			}
		}
	}
}