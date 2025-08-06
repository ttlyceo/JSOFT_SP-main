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

import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.communication.SendablePacket;

public class PlayerAuthRequest extends SendablePacket
{
	private final String _account;
	private final int _playOkID1;
	private final int _playOkID2;
	private final int _loginOkID1;
	private final int _loginOkID2;
  
	public PlayerAuthRequest(GameClient client)
	{
		_account = client.getLogin();
		_playOkID1 = client.getSessionId().playOkID1;
		_playOkID2 = client.getSessionId().playOkID2;
		_loginOkID1 = client.getSessionId().loginOkID1;
		_loginOkID2 = client.getSessionId().loginOkID2;
	}
  
	@Override
	protected void writeImpl()
	{
		writeC(0x02);
		writeS(_account);
		writeD(_playOkID1);
		writeD(_playOkID2);
		writeD(_loginOkID1);
		writeD(_loginOkID2);
	}
}
