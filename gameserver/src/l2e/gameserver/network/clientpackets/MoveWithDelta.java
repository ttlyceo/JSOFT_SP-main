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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.Config;

public class MoveWithDelta extends GameClientPacket
{
	protected int _dx;
	protected int _dy;
	protected int _dz;
	
	@Override
	protected void readImpl()
	{
		_dx = readD();
		_dy = readD();
		_dz = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.CLIENT_PACKET_HANDLER_DEBUG)
		{
			_log.warn("MoveWithDelta: Not support for this packet!!!");
		}
	}
}