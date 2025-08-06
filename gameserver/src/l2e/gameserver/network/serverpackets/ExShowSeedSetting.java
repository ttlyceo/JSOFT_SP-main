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
import java.util.Set;

import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.Seed;
import l2e.gameserver.model.actor.templates.SeedTemplate;

public class ExShowSeedSetting extends GameServerPacket
{
	private final int _manorId;
	private final Set<Seed> _seeds;
	private final Map<Integer, SeedTemplate> _current = new HashMap<>();
	private final Map<Integer, SeedTemplate> _next = new HashMap<>();
	
	public ExShowSeedSetting(int manorId)
	{
		final CastleManorManager manor = CastleManorManager.getInstance();
		_manorId = manorId;
		_seeds = manor.getSeedsForCastle(_manorId);
		for (final Seed s : _seeds)
		{
			SeedTemplate sp = manor.getSeedProduct(manorId, s.getSeedId(), false);
			if (sp != null)
			{
				_current.put(s.getSeedId(), sp);
			}
			sp = manor.getSeedProduct(manorId, s.getSeedId(), true);
			if (sp != null)
			{
				_next.put(s.getSeedId(), sp);
			}
		}
	}

	@Override
	public void writeImpl()
	{
		writeD(_manorId);
		writeD(_seeds.size());
		SeedTemplate sp;
		for (final Seed s : _seeds)
		{
			writeD(s.getSeedId());
			writeD(s.getLevel());
			writeC(0x01);
			writeD(s.getReward(1));
			writeC(0x01);
			writeD(s.getReward(2));
			writeD(s.getSeedLimit());
			writeD(s.getSeedReferencePrice());
			writeD(s.getSeedMinPrice());
			writeD(s.getSeedMaxPrice());
			if (_current.containsKey(s.getSeedId()))
			{
				sp = _current.get(s.getSeedId());
				writeQ(sp.getStartAmount());
				writeQ(sp.getPrice());
			}
			else
			{
				writeQ(0x00);
				writeQ(0x00);
			}
			if (_next.containsKey(s.getSeedId()))
			{
				sp = _next.get(s.getSeedId());
				writeQ(sp.getStartAmount());
				writeQ(sp.getPrice());
			}
			else
			{
				writeQ(0x00);
				writeQ(0x00);
			}
		}
		_current.clear();
		_next.clear();
	}
}