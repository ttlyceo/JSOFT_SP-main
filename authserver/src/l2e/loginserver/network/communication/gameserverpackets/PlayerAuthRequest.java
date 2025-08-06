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
package l2e.loginserver.network.communication.gameserverpackets;

import l2e.loginserver.accounts.SessionManager;
import l2e.loginserver.accounts.SessionManager.Session;
import l2e.loginserver.network.SessionKey;
import l2e.loginserver.network.communication.ReceivablePacket;
import l2e.loginserver.network.communication.loginserverpackets.PlayerAuthResponse;

public class PlayerAuthRequest extends ReceivablePacket
{
	private String _account;
	private int _playOkId1;
	private int _playOkId2;
	private int _loginOkId1;
	private int _loginOkId2;
	
	@Override
	protected void readImpl()
	{
		_account = readS();
		_playOkId1 = readD();
		_playOkId2 = readD();
		_loginOkId1 = readD();
		_loginOkId2 = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final SessionKey skey = new SessionKey(_loginOkId1, _loginOkId2, _playOkId1, _playOkId2);
		final Session session = SessionManager.getInstance().closeSession(skey);
		if (session == null || !session.getAccount().getLogin().equals(_account))
		{
			sendPacket(new PlayerAuthResponse(_account));
			return;
		}
		sendPacket(new PlayerAuthResponse(session, session.getSessionKey().equals(skey), session.getIP(), session.getAccount().getLastServer()));
	}
}