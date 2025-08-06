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

import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.Seed;
import l2e.gameserver.model.actor.templates.SeedTemplate;

public class ExShowSeedInfo extends GameServerPacket
{
	private final List<SeedTemplate> _seeds;
	private final int _manorId;
	private final boolean _hideButtons;

	public ExShowSeedInfo(int manorId, boolean nextPeriod, boolean hideButtons)
	{
		_manorId = manorId;
		_hideButtons = hideButtons;
		_seeds = (nextPeriod && !CastleManorManager.getInstance().isManorApproved()) ? null : CastleManorManager.getInstance().getSeedProduction(manorId, nextPeriod);
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(_hideButtons ? 0x01 : 0x00);
		writeD(_manorId);
		writeD(0x00);
		if (_seeds == null)
		{
			writeD(0x00);
			return;
		}
		writeD(_seeds.size());
		for (final SeedTemplate seed : _seeds)
		{
			writeD(seed.getId());
			writeQ(seed.getAmount());
			writeQ(seed.getStartAmount());
			writeQ(seed.getPrice());
			final Seed s = CastleManorManager.getInstance().getSeed(seed.getId());
			if (s == null)
			{
				writeD(0x00);
				writeC(0x01);
				writeD(0x00);
				writeC(0x01);
				writeD(0x00);
			}
			else
			{
				writeD(s.getLevel());
				writeC(0x01);
				writeD(s.getReward(1));
				writeC(0x01);
				writeD(s.getReward(2));
			}
		}
	}
}