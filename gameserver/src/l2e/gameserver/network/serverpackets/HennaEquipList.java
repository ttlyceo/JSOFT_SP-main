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

import java.util.List;

import l2e.gameserver.data.parser.HennaParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Henna;

public class HennaEquipList extends GameServerPacket
{
	private final Player _player;
	private final List<Henna> _hennaEquipList;

	public HennaEquipList(Player player)
	{
		_player = player;
		_hennaEquipList = HennaParser.getInstance().getHennaList(player.getClassId());
	}
	
	public HennaEquipList(Player player, List<Henna> list)
	{
		_player = player;
		_hennaEquipList = list;
	}

	@Override
	protected final void writeImpl()
	{
		writeQ(_player.getAdena());
		writeD(0x03);
		writeD(_hennaEquipList.size());
		for (final Henna henna : _hennaEquipList)
		{
			if ((_player.getInventory().getItemByItemId(henna.getDyeItemId())) != null)
			{
				writeD(henna.getDyeId());
				writeD(henna.getDyeItemId());
				writeQ(henna.getWearCount());
				writeQ(henna.getWearFee());
				writeD(henna.isAllowedClass(_player.getClassId()) ? 0x01 : 0x00);
			}
		}
	}
}