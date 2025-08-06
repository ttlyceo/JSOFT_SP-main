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
package l2e.gameserver.network.clientpackets;

import org.strixplatform.StrixPlatform;

import l2e.commons.log.Log;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.Shutdown;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.SessionKey;
import l2e.gameserver.network.communication.gameserverpackets.PlayerAuthRequest;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.LoginFail;
import l2e.gameserver.network.serverpackets.ServerClose;
import top.jsoft.jguard.JGuard;
import top.jsoft.jguard.manager.session.JClientSessionManager;
import top.jsoft.jguard.manager.session.model.JClientSession;

public final class RequestLogin extends GameClientPacket
{
	private String _loginName;
	private int _playKey1;
	private int _playKey2;
	private int _loginKey1;
	private int _loginKey2;
	private int _lang;
	private byte[] _guardData = null;
	
	@Override
	protected void readImpl()
	{
		_loginName = readS().toLowerCase();
		_playKey2 = readD();
		_playKey1 = readD();
		_loginKey1 = readD();
		_loginKey2 = readD();
		_lang = readD();
		if (Config.PROTECTION.equalsIgnoreCase("ANTICHEAT"))
		{
			if (getAvaliableBytes() >= 32)
			{
				_guardData = new byte[32];
				readB((getByteBuffer().limit() - 32), _guardData, 0, 32);
			}
		}
	}

	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		if (_loginName.isEmpty())
		{
			client.closeNow(false);
			return;
		}
		
		final SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);
		client.setSessionId(key);
		client.setLogin(_loginName);
		client.setLang(_lang);
		
		if (Shutdown.getInstance().getMode() != Shutdown.SIGTERM && Shutdown.getInstance().getSeconds() <= 60)
		{
			client.closeNow(false);
		}
		else
		{
			if (AuthServerCommunication.getInstance().isShutdown())
			{
				client.close(new LoginFail(LoginFail.SYSTEM_ERROR_LOGIN_LATER));
				return;
			}

			if(JGuard.isProtectEnabled())
			{
				boolean result = JGuard.checkClientAuth(client, _loginName);
				if(!result)
				{
					client.close(null);
					return;
				}

				JClientSession sess = JClientSessionManager.getSession(client);
				if(!sess.hasAccountSession(_loginName))
				{
					client.close(null);
					return;
				}
			}

			final GameClient oldClient = AuthServerCommunication.getInstance().addWaitingClient(client);
			if (oldClient != null)
			{
				oldClient.close(ServerClose.STATIC_PACKET);
			}

			if (Config.PROTECTION.equalsIgnoreCase("ANTICHEAT") && _guardData != null)
			{
				client.setHWID(Util.bytesToHex(_guardData));
			}

			JGuard.getInstance().enterToServer(client, client.getHWID());

			AuthServerCommunication.getInstance().sendPacket(new PlayerAuthRequest(client));
			if (StrixPlatform.getInstance().isPlatformEnabled())
			{
				if (client.getStrixClientData() != null)
				{
					client.getStrixClientData().setClientAccount(_loginName);
					if (StrixPlatform.getInstance().isAuthLogEnabled())
					{
						Log.addLogGame("STRIX GUARD:", "HWID: [" + client.getStrixClientData().getClientHWID() + "] SessionID: [" + client.getStrixClientData().getSessionId() + "] connect to server!", _loginName);
					}
				}
				else
				{
					client.close((GameServerPacket) null);
					return;
				}
			}
		}
	}
}