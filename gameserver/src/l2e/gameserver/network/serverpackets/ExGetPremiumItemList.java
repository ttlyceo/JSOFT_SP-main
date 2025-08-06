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

import java.util.Map;
import java.util.Map.Entry;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.PremiumItemTemplate;

public class ExGetPremiumItemList extends GameServerPacket
{
	private final Player _activeChar;
	
	private final Map<Integer, PremiumItemTemplate> _map;

	public ExGetPremiumItemList(Player activeChar)
	{
		_activeChar = activeChar;
		_map = _activeChar.getPremiumItemList();
	}
	
	@Override
	protected void writeImpl()
	{
		if (!_map.isEmpty())
		{
			writeD(_map.size());
			for (final Entry<Integer, PremiumItemTemplate> entry : _map.entrySet())
			{
				final PremiumItemTemplate item = entry.getValue();
				writeD(entry.getKey());
				writeD(_activeChar.getObjectId());
				writeD(item.getId());
				writeQ(item.getCount());
				writeD(0x00);
				writeS(item.getSender());
			}
		}
		else
		{
			writeD(0x00);
		}
	}
}