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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.actor.templates.SeedTemplate;

public final class BuyListSeed extends GameServerPacket
{
	private final int _manorId;
	private final long _money;
	private final List<SeedTemplate> _list = new ArrayList<>();
	
	public BuyListSeed(long currentMoney, int castleId)
	{
		_money = currentMoney;
		_manorId = castleId;
		for (final SeedTemplate s : CastleManorManager.getInstance().getSeedProduction(castleId, false))
		{
			if ((s.getAmount() > 0) && (s.getPrice() > 0))
			{
				_list.add(s);
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeQ(_money);
		writeD(_manorId);
		if (_list != null && _list.size() > 0)
		{
			writeH(_list.size());
			for (final SeedTemplate s : _list)
			{
				writeD(s.getId());
				writeD(s.getId());
				writeD(0x00);
				writeQ(s.getAmount());
				writeH(0x05);
				writeH(0x00);
				writeH(0x00);
				writeD(0x00);
				writeH(0x00);
				writeH(0x00);
				writeD(0x00);
				writeD(-1);
				writeD(-9999);
				writeH(0x00);
				writeH(0x00);
				for (byte i = 0; i < 6; i++)
				{
					writeH(0x00);
				}
				writeH(0x00);
				writeH(0x00);
				writeH(0x00);
				writeQ(s.getPrice());
			}
			_list.clear();
		}
		else
		{
			writeH(0x00);
		}
	}
}