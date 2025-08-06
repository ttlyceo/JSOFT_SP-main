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

import l2e.loginserver.Config;
import l2e.loginserver.GameServerManager;
import l2e.loginserver.accounts.Account;
import l2e.loginserver.network.LoginClient;
import l2e.loginserver.network.ProxyServer;
import l2e.loginserver.network.SessionKey;
import l2e.loginserver.network.communication.GameServer;
import l2e.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import l2e.loginserver.network.serverpackets.PlayOk;

public class RequestServerLogin extends LoginClientPacket
{
	private int _loginOkID1;
	private int _loginOkID2;
	private int _serverId;

	@Override
	protected void readImpl()
	{
		_loginOkID1 = readD();
		_loginOkID2 = readD();
		_serverId = readC();
	}

	@Override
	protected void runImpl()
	{
		final LoginClient client = getClient();
		if(!client.isPasswordCorrect())
		{
			client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
			return;
		}

		final SessionKey skey = client.getSessionKey();
		if (skey != null && (skey.checkLoginPair(_loginOkID1, _loginOkID2) || !Config.SHOW_LICENCE))
		{
			final Account account = client.getAccount();
			GameServer gs = GameServerManager.getInstance().getGameServerById(_serverId);
			if (gs == null)
			{
				final ProxyServer ps = GameServerManager.getInstance().getProxyServerById(_serverId);
				if (ps != null)
				{
					gs = GameServerManager.getInstance().getGameServerById(ps.getOrigServerId());
				}
			}
			
			if (gs == null || !gs.isAuthed() || gs.isGmOnly() && account.getAccessLevel() < 5 || gs.getOnline() >= gs.getMaxPlayers() && account.getAccessLevel() < 1)
			{
				client.close(LoginFailReason.REASON_ACCESS_FAILED);
				return;
			}
			
			account.setLastServer(_serverId);
			account.update();
			
			client.close(new PlayOk(skey, _serverId));
		}
		else
		{
			client.close(LoginFailReason.REASON_ACCESS_FAILED);
			return;
		}
	}
}