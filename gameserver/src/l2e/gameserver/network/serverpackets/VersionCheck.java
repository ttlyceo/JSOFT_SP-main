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
package l2e.gameserver.network.serverpackets;

import org.strixplatform.StrixPlatform;
import org.strixplatform.utils.StrixClientData;

public final class VersionCheck extends GameServerPacket
{
	private final byte[] _key;
	private final StrixClientData _clientData;
	
	public VersionCheck(final byte[] key)
	{
		_key = key;
		_clientData = null;
	}

	public VersionCheck(final byte[] key, final StrixClientData clientData)
	{
		_key = key;
		_clientData = clientData;
	}

	@Override
	public void writeImpl()
	{
		if ((_key == null) || (_key.length == 0))
		{

			if (StrixPlatform.getInstance().isBackNotificationEnabled() && _clientData != null)
			{
				writeC(_clientData.getServerResponse().ordinal() + 1);
			}
			else
			{
				writeC(0x00);
			}
			return;
		}
		writeC(0x01);
		writeB(_key);
		writeD(0x01);
		writeD(0x00);
		writeC(0x00);
		writeD(0x00);
	}
}