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

import java.util.List;

import l2e.gameserver.network.communication.SendablePacket;

public class SetAccountInfo extends SendablePacket
{
	private final String _account;
	private final int _size;
	private final List<Long> _deleteChars;
  
	public SetAccountInfo(String account, int size, List<Long> deleteChars)
	{
		_account = account;
		_size = size;
		_deleteChars = deleteChars;
	}
  
	@Override
	protected void writeImpl()
	{
		writeC(0x05);
		writeS(_account);
		writeC(_size);
		writeD(_deleteChars.size());
		for (final long time : _deleteChars)
		{
			writeQ(time);
		}
	}
}
