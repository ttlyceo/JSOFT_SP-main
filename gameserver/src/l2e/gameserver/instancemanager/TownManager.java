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
package l2e.gameserver.instancemanager;

import java.util.List;

import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.model.zone.type.TownZone;

public class TownManager
{
	public static final int getTownCastle(int townId)
	{
		switch (townId)
		{
			case 912 :
				return 1;
			case 916 :
				return 2;
			case 918 :
				return 3;
			case 922 :
				return 4;
			case 924 :
				return 5;
			case 926 :
				return 6;
			case 1538 :
				return 7;
			case 1537 :
				return 8;
			case 1714 :
				return 9;
			default :
				return 0;
		}
	}
	
	public static final boolean townHasCastleInSiege(int townId)
	{
		final int castleIndex = getTownCastle(townId);
		if (castleIndex > 0)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
			if (castle != null)
			{
				return castle.getSiege().getIsInProgress();
			}
		}
		return false;
	}
	
	public static final boolean townHasCastleInSiege(int x, int y)
	{
		return townHasCastleInSiege(MapRegionManager.getInstance().getMapRegionLocId(x, y));
	}
	
	public static final TownZone getTown(int townId)
	{
		for (final TownZone temp : ZoneManager.getInstance().getAllZones(TownZone.class))
		{
			if (temp != null && temp.getTownId() == townId)
			{
				return temp;
			}
		}
		return null;
	}
	
	public static final TownZone getTown(int x, int y, int z)
	{
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(x, y, z);
		if (zones != null && !zones.isEmpty())
		{
			for (final ZoneType zone : zones)
			{
				if (zone != null && zone instanceof TownZone)
				{
					return (TownZone) zone;
				}
			}
		}
		return null;
	}
	
	public static final TownZone getTownZone(int x, int y, int z)
	{
		final TownZone zone = ZoneManager.getInstance().getZone(x, y, z, TownZone.class);
		if (zone != null && zone.getTaxById() > 0)
		{
			return zone;
		}
		return null;
	}
}