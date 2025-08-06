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

import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.CropProcureTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;

public class SellListProcure extends GameServerPacket
{
	private final long _money;
	private final Map<ItemInstance, Long> _sellList = new HashMap<>();
	
	public SellListProcure(Player player, int castleId)
	{
		_money = player.getAdena();
		for (final CropProcureTemplate c : CastleManorManager.getInstance().getCropProcure(castleId, false))
		{
			final ItemInstance item = player.getInventory().getItemByItemId(c.getId());
			if ((item != null) && (c.getAmount() > 0))
			{
				_sellList.put(item, c.getAmount());
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeQ(_money);
		writeD(0x00);
		writeH(_sellList.size());
		for (final ItemInstance item : _sellList.keySet())
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getDisplayId());
			writeQ(_sellList.get(item));
			writeH(item.getItem().getType2());
			writeH(0x00);
			writeQ(0x00);
		}
	}
}