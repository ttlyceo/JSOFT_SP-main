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
package l2e.loginserver.network.clientpackets;

import l2e.loginserver.network.LoginClient;
import l2e.loginserver.network.LoginClient.LoginClientState;
import l2e.loginserver.network.serverpackets.GGAuth;
import l2e.loginserver.network.serverpackets.LoginFail;

public class AuthGameGuard extends LoginClientPacket
{
	private int _sessionId;
	@Override
	protected void readImpl()
	{
		_sessionId = readD();
	}

	@Override
	protected void runImpl()
	{
		final LoginClient client = getClient();
		if(_sessionId == 0 || _sessionId == client.getSessionId())
		{
			client.setState(LoginClientState.AUTHED_GG);
			client.sendPacket(new GGAuth(client.getSessionId()));
		}
		else
		{
			client.close(LoginFail.LoginFailReason.REASON_ACCESS_FAILED);
		}
	}
}
