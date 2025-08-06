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

import org.HostInfo;

import l2e.commons.apache.ArrayUtils;
import l2e.loginserver.accounts.SessionManager;
import l2e.loginserver.network.communication.GameServer;
import l2e.loginserver.network.communication.ReceivablePacket;

public class SetAccountInfo extends ReceivablePacket
{
	private String _account;
	private int _size;
	private int[] _deleteChars;
	
	@Override
	protected void readImpl()
	{
		_account = readS();
		_size = readC();
		final int size = readD();
		if (size > 7 || size <= 0)
		{
			_deleteChars = ArrayUtils.EMPTY_INT_ARRAY;
		}
		else
		{
			_deleteChars = new int[size];
			for (int i = 0; i < _deleteChars.length; i++)
			{
				_deleteChars[i] = readD();
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final GameServer gs = getGameServer();
		if (gs.isAuthed())
		{
			final SessionManager.Session session = SessionManager.getInstance().getSessionByName(_account);
			if (session == null)
			{
				return;
			}
			
			for (final HostInfo host : gs.getHosts())
			{
				session.getAccount().addAccountInfo(host.getId(), _size, _deleteChars);
			}
		}
	}
}