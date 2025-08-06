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
package l2e.gameserver.network.communication.gameserverpackets;

import l2e.gameserver.network.communication.SendablePacket;

public class ChangeAccessLevel extends SendablePacket
{
	private final String _account;
	private final int _level;
	private final int _banExpire;
	
	public ChangeAccessLevel(String account, int level, int banExpire)
	{
		_account = account;
		_level = level;
		_banExpire = banExpire;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0x11);
		writeS(_account);
		writeD(_level);
		writeD(_banExpire);
	}
}
