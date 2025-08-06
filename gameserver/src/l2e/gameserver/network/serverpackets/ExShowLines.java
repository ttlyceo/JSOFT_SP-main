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

import l2e.gameserver.model.actor.Player;

public class ExShowLines extends GameServerPacket
{
	private final int _x;
	private final int _y;
	private final int _z;
	
	public ExShowLines(Player player)
	{
		_x = player.getX();
		_y = player.getY();
		_z = player.getZ();
	}

	@Override
	protected void writeImpl()
	{
		writeH(0x00);
		writeD(0x02);
		writeC(200);
		writeC(200);
		writeC(256);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(0x00);
	}
}