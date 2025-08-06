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
package l2e.gameserver.network.communication.loginserverpackets;

import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.ReceivablePacket;

public class LoginServerFail extends ReceivablePacket
{
	private static final String[] REASONS =
	{
	        "none", "IP banned", "IP reserved", "wrong hexid", "ID reserved", "no free ID", "not authed", "already logged in"
	};
	
	private String _reason;
	private boolean _restartConnection = true;
	
	@Override
	protected void readImpl()
	{
		final int reasonId = readC();
		if (!getByteBuffer().hasRemaining())
		{
			_reason = "Authserver registration failed! Reason: " + REASONS[reasonId];
		}
		else
		{
			_reason = readS();
			_restartConnection = readC() > 0;
		}
	}
	
	@Override
	protected void runImpl()
	{
		_log.warn(_reason);
		if (_restartConnection)
		{
			AuthServerCommunication.getInstance().restart();
		}
	}
}
