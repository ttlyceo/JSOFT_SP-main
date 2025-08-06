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

import l2e.gameserver.model.actor.templates.VehicleTemplate;

public class ExAirShipTeleportList extends GameServerPacket
{
	private final int _dockId;
	private final VehicleTemplate[][] _teleports;
	private final int[] _fuelConsumption;
	
	public ExAirShipTeleportList(int dockId, VehicleTemplate[][] teleports, int[] fuelConsumption)
	{
		_dockId = dockId;
		_teleports = teleports;
		_fuelConsumption = fuelConsumption;
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_dockId);
		if (_teleports != null)
		{
			writeD(_teleports.length);
			VehicleTemplate[] path;
			VehicleTemplate dst;
			for (int i = 0; i < _teleports.length; i++)
			{
				writeD(i - 1);
				writeD(_fuelConsumption[i]);
				path = _teleports[i];
				dst = path[path.length - 1];
				writeD(dst.getX());
				writeD(dst.getY());
				writeD(dst.getZ());
			}
		}
		else
		{
			writeD(0x00);
		}
	}
}