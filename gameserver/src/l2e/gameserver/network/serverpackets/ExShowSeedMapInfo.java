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

import l2e.gameserver.instancemanager.SoDManager;
import l2e.gameserver.instancemanager.SoIManager;
import l2e.gameserver.model.Location;

public class ExShowSeedMapInfo extends GameServerPacket
{
	public static final ExShowSeedMapInfo STATIC_PACKET = new ExShowSeedMapInfo();

	private static final Location[] ENTRANCES =
	{
	        new Location(-246857, 251960, 4331, 1), new Location(-213770, 210760, 4400, 2),
	};
	
	private ExShowSeedMapInfo()
	{
	}

	@Override
	protected void writeImpl()
	{
		writeD(ENTRANCES.length);
		for (final Location loc : ENTRANCES)
		{
			writeD(loc.getX());
			writeD(loc.getY());
			writeD(loc.getZ());
			switch (loc.getHeading())
			{
				case 1 :
					if (SoDManager.getInstance().isAttackStage())
					{
						writeD(2771);
					}
					else
					{
						writeD(2772);
					}
					break;
				case 2 :
					writeD(SoIManager.getInstance().getCurrentStage() + 2765);
					break;
			}
		}
	}
}