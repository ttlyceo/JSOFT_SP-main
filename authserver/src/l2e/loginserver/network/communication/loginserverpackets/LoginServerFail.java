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
package l2e.loginserver.network.communication.loginserverpackets;

import l2e.loginserver.network.communication.SendablePacket;

public class LoginServerFail extends SendablePacket
{
	private final String _reason;
	private final boolean _restartConnection;
	
	public LoginServerFail(String reason, boolean restartConnection)
	{
		_reason = reason;
		_restartConnection = restartConnection;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0x01);
		writeC(0x00);
		writeS(_reason);
		writeC(_restartConnection ? 0x01 : 0x00);
	}
}