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
package l2e.gameserver.network.communication.gameserverpackets;

import org.HostInfo;

import l2e.commons.net.IPSettings;
import l2e.gameserver.Config;
import l2e.gameserver.GameServer;
import l2e.gameserver.network.communication.SendablePacket;

public class AuthRequest extends SendablePacket
{
	@Override
	protected void writeImpl()
	{
		writeC(0x00);
		writeD(0x02);
		writeD(Config.SERVER_LIST_TYPE);
		writeD(Config.SERVER_LIST_AGE);
		writeC(Config.SERVER_GMONLY ? 0x01 : 0x00);
		writeC(Config.SERVER_LIST_BRACKET ? 0x01 : 0x00);
		writeC(Config.SERVER_LIST_IS_PVP ? 0x01 : 0x00);
		writeD(GameServer.getInstance().getOnlineLimit());
		
		final HostInfo[] hosts = IPSettings.getInstance().getGameServerHosts();
		writeC(hosts.length);
		for (final var host : hosts)
		{
			writeC(host.getId());
			writeD(host.isAllowHaProxy() ? 0x01 : 0x00);
			writeS(host.getAddress());
			writeH(host.getPort());
			writeS(host.getKey());
			writeC(host.getSubnets().size());
			for (final var m : host.getSubnets().entrySet())
			{
				writeS(m.getValue());
				final byte[] address = m.getKey().getAddress();
				writeD(address.length);
				writeB(address);
				final byte[] mask = m.getKey().getMask();
				writeD(mask.length);
				writeB(mask);
			}
		}
	}
}