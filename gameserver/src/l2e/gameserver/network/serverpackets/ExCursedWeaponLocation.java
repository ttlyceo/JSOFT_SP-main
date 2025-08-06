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

import l2e.gameserver.model.Location;

public class ExCursedWeaponLocation extends GameServerPacket
{
	private final List<CursedWeaponInfo> _cursedWeaponInfo;
	
	public ExCursedWeaponLocation(List<CursedWeaponInfo> cursedWeaponInfo)
	{
		_cursedWeaponInfo = cursedWeaponInfo;
	}
	
	@Override
	protected void writeImpl()
	{
		if (!_cursedWeaponInfo.isEmpty())
		{
			writeD(_cursedWeaponInfo.size());
			for (final CursedWeaponInfo w : _cursedWeaponInfo)
			{
				writeD(w._id);
				writeD(w._activated);
				writeD(w._loc.getX());
				writeD(w._loc.getY());
				writeD(w._loc.getZ());
			}
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
		}
	}
	
	public static class CursedWeaponInfo
	{
		public Location _loc;
		public int _id;
		public int _activated;
		
		public CursedWeaponInfo(Location loc, int id, int status)
		{
			_loc = loc;
			_id = id;
			_activated = status;
		}
	}
}