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
import l2e.gameserver.model.actor.templates.CropProcureTemplate;

public class ExShowCropSetting extends GameServerPacket
{
	private final int _manorId;
	private final Set<Seed> _seeds;
	private final Map<Integer, CropProcureTemplate> _current = new HashMap<>();
	private final Map<Integer, CropProcureTemplate> _next = new HashMap<>();

	public ExShowCropSetting(int manorId)
	{
		_manorId = manorId;
		_seeds = CastleManorManager.getInstance().getSeedsForCastle(_manorId);
		for (final Seed s : _seeds)
		{
			CropProcureTemplate cp = CastleManorManager.getInstance().getCropProcure(manorId, s.getCropId(), false);
			if (cp != null)
			{
				_current.put(s.getCropId(), cp);
			}
			cp = CastleManorManager.getInstance().getCropProcure(manorId, s.getCropId(), true);
			if (cp != null)
			{
				_next.put(s.getCropId(), cp);
			}
		}
	}
	
	@Override
	public void writeImpl()
	{
		writeD(_manorId);
		writeD(_seeds.size());
		CropProcureTemplate cp;
		for (final Seed s : _seeds)
		{
			writeD(s.getCropId());
			writeD(s.getLevel());
			writeC(0x01);
			writeD(s.getReward(1));
			writeC(0x01);
			writeD(s.getReward(2));
			writeD(s.getCropLimit());
			writeD(0x00);
			writeD(s.getCropMinPrice());
			writeD(s.getCropMaxPrice());
			if (_current.containsKey(s.getCropId()))
			{
				cp = _current.get(s.getCropId());
				writeQ(cp.getStartAmount());
				writeQ(cp.getPrice());
				writeC(cp.getReward());
			}
			else
			{
				writeQ(0x00);
				writeQ(0x00);
				writeC(0x00);
			}
			if (_next.containsKey(s.getCropId()))
			{
				cp = _next.get(s.getCropId());
				writeQ(cp.getStartAmount());
				writeQ(cp.getPrice());
				writeC(cp.getReward());
			}
			else
			{
				writeQ(0x00);
				writeQ(0x00);
				writeC(0x00);
			}
		}
		_next.clear();
		_current.clear();
	}
}