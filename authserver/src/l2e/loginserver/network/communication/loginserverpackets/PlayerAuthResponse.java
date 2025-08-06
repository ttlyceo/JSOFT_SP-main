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

import l2e.loginserver.accounts.Account;
import l2e.loginserver.accounts.SessionManager.Session;
import l2e.loginserver.network.SessionKey;
import l2e.loginserver.network.communication.SendablePacket;

public class PlayerAuthResponse extends SendablePacket
{
	private final String _login;
	private final boolean _authed;
	private int _playOkID1;
	private int _playOkID2;
	private int _loginOkID1;
	private int _loginOkID2;
	private String _ip, _hwid, _lockedIp;
	private int _requestId;
  
	public PlayerAuthResponse(Session session, boolean authed, String ip, int requestId)
	{
		final Account account = session.getAccount();
		_login = account.getLogin();
		_authed = authed;
		if (authed)
		{
			final SessionKey skey = session.getSessionKey();
			_playOkID1 = skey._playOkID1;
			_playOkID2 = skey._playOkID2;
			_loginOkID1 = skey._loginOkID1;
			_loginOkID2 = skey._loginOkID2;
			_ip = ip;
			_hwid = account.getAllowedHwid();
			_lockedIp = account.getAllowedIp();
			_requestId = requestId;
		}
	}
  
	public PlayerAuthResponse(String account)
	{
		_login = account;
		_authed = false;
	}
  
	@Override
	protected void writeImpl()
	{
		writeC(0x02);
		writeS(_login);
		writeC(_authed ? 1 : 0);
		if (_authed)
		{
			writeD(_playOkID1);
			writeD(_playOkID2);
			writeD(_loginOkID1);
			writeD(_loginOkID2);
			writeS(_ip);
			writeD(_requestId);
			writeS(_hwid);
			writeS(_lockedIp);
		}
	}
}
