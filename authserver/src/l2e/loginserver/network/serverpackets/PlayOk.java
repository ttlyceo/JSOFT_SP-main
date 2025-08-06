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
package l2e.loginserver.network.serverpackets;

import l2e.loginserver.network.SessionKey;

public final class PlayOk extends LoginServerPacket
{
	private final int _playOk1, _playOk2;
	private final int _serverId;

	public PlayOk(SessionKey sessionKey, int serverId)
	{
		_playOk1 = sessionKey._playOkID1;
		_playOk2 = sessionKey._playOkID2;
		_serverId = serverId;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x07);
		writeD(_playOk1);
		writeD(_playOk2);
		writeC(_serverId);
	}
}
