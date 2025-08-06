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

import org.HostInfo;

import l2e.loginserver.Config;
import l2e.loginserver.network.communication.GameServer;
import l2e.loginserver.network.communication.SendablePacket;

public class AuthResponse extends SendablePacket
{
	private final HostInfo[] _hosts;
	
	public AuthResponse(GameServer gs)
	{
		_hosts = gs.getHosts();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0x00);
		writeC(0x00);
		writeS("");
		writeC(_hosts.length);
		for (final HostInfo host : _hosts)
		{
			writeC(host.getId());
			writeS(Config.SERVER_NAMES.get(host.getId()));
		}
	}
}