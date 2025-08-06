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
import org.strixplatform.managers.ClientGameSessionManager;
import org.strixplatform.managers.ClientProtocolDataManager;

import l2e.gameserver.Config;
import l2e.gameserver.network.serverpackets.VersionCheck;
import top.jsoft.jguard.JGuard;
import top.jsoft.jguard.JGuardConfig;

import java.nio.BufferUnderflowException;

public final class SendProtocolVersion extends GameClientPacket
{
	private static final short BasePacketSize = 4 + 256;
	private int _version;
	private byte[] _data;
	private int _dataChecksum;
	private String HWID;
	
	@Override
	protected void readImpl()
	{
		_version = readD();
		if (StrixPlatform.getInstance().isPlatformEnabled())
		{
			try
			{
				if (_buf.remaining() >= StrixPlatform.getInstance().getProtocolVersionDataSize())
				{
					_data = new byte[StrixPlatform.getInstance().getClientDataSize()];
					readB(_data);
					_dataChecksum = readD();
				}
			}
			catch (final Exception e)
			{
				final var client = getClient();
				if (client != null)
				{
					client.close(new VersionCheck(null));
				}
				return;
			}
		}
		else if (JGuard.isProtectEnabled() && _buf.remaining() > BasePacketSize) {
			_data = new byte[BasePacketSize];
			readB(_data);
			if (JGuardConfig.JGUARD_ENABLED_HWID_REQUEST) {
				try {
					HWID = readS();
				} catch (BufferUnderflowException e) {
					_log.warn(getClient().toString() + " - trying to connect with broken HWID. Connection closed.");
					getClient().close(new VersionCheck(null));
				}
			}
		} else if (JGuard.isProtectEnabled()) {
			_log.warn(getClient().toString() + " - trying to connect with broken protection. Connection closed.");
			getClient().close(new VersionCheck(null));
		}
	}
	
	@Override
	protected void runImpl()
	{
		final var client = getClient();
		if (client == null)
		{
			return;
		}
		
		if (_version == -2)
		{
			client.closeNow(false);
		}
		else if (!Config.PROTOCOL_LIST.contains(_version))
		{
			client.close(new VersionCheck(null));
		}
		
		if (!StrixPlatform.getInstance().isPlatformEnabled())
		{
			if(JGuard.isProtectEnabled() && JGuardConfig.JGUARD_ENABLED_HWID_REQUEST && HWID != null) {
				getClient().setHWID(HWID);
			}
			client.setRevision(_version);
			client.sendPacket(new VersionCheck(_client.enableCrypt()));
			return;
		}
		else
		{
			if (_data == null)
			{
				client.close(new VersionCheck(null));
				return;
			}
			else
			{
				final var clientData = ClientProtocolDataManager.getInstance().getDecodedData(_data, _dataChecksum);
				if (clientData != null)
				{
					if (!ClientGameSessionManager.getInstance().checkServerResponse(clientData))
					{
						client.close(new VersionCheck(null, clientData));
						return;
					}
					client.setStrixClientData(clientData);
					client.setRevision(_version);
					sendPacket(new VersionCheck(_client.enableCrypt()));
					return;
				}
				client.close(new VersionCheck(null));
			}
		}
	}
}