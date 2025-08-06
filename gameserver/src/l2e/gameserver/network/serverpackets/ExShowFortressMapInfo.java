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

import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.spawn.SpawnFortSiege;
import l2e.gameserver.model.spawn.Spawner;

public class ExShowFortressMapInfo extends GameServerPacket
{
	private final Fort _fortress;

	public ExShowFortressMapInfo(Fort fortress)
	{
		_fortress = fortress;
	}

	@Override
	protected void writeImpl()
	{
		writeD(_fortress.getId());
		writeD(_fortress.getSiege().getIsInProgress() ? 0x01 : 0x00);
		writeD(_fortress.getFortSize());
		final List<SpawnFortSiege> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(_fortress.getId());
		if ((commanders != null) && (commanders.size() != 0) && _fortress.getSiege().getIsInProgress())
		{
			switch (commanders.size())
			{
				case 3 :
				{
					for (final SpawnFortSiege spawn : commanders)
					{
						if (isSpawned(spawn.getId()))
						{
							writeD(0x00);
						}
						else
						{
							writeD(0x01);
						}
					}
					break;
				}
				case 4 :
				{
					int count = 0;
					for (final SpawnFortSiege spawn : commanders)
					{
						count++;
						if (count == 4)
						{
							writeD(0x01);
						}
						if (isSpawned(spawn.getId()))
						{
							writeD(0x00);
						}
						else
						{
							writeD(0x01);
						}
					}
					break;
				}
			}
		}
		else
		{
			for (int i = 0; i < _fortress.getFortSize(); i++)
			{
				writeD(0x00);
			}
		}
	}

	private boolean isSpawned(int npcId)
	{
		boolean ret = false;
		for (final Spawner spawn : _fortress.getSiege().getCommanders())
		{
			if (spawn.getId() == npcId)
			{
				ret = true;
				break;
			}
		}
		return ret;
	}
}