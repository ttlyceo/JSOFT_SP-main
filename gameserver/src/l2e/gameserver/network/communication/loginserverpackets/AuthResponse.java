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
package l2e.gameserver.network.communication.loginserverpackets;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.GameServer;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.ReceivablePacket;
import l2e.gameserver.network.communication.gameserverpackets.OnlineStatus;
import l2e.gameserver.network.communication.gameserverpackets.PlayerInGame;

public class AuthResponse extends ReceivablePacket
{
	private static class ServerInfo
	{
		private final int _id;
		private final String _name;
		
		public ServerInfo(int id, String name)
		{
			_id = id;
			_name = name;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getName()
		{
			return _name;
		}
	}
	
	private List<ServerInfo> _servers;
	
	@Override
	protected void readImpl()
	{
		final int serverId = readC();
		final String serverName = readS();
		if (!getByteBuffer().hasRemaining())
		{
			_servers = new ArrayList<>(1);
			_servers.add(new ServerInfo(serverId, serverName));
		}
		else
		{
			final int serversCount = readC();
			_servers = new ArrayList<>(serversCount);
			for (int i = 0; i < serversCount; i++)
			{
				_servers.add(new ServerInfo(readC(), readS()));
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		for (final ServerInfo info : _servers)
		{
			_log.info("Registered on login as Server " + info.getId() + " : " + info.getName() + " [Players Limit: " + GameServer.getInstance().getOnlineLimit() + "]");
		}
		
		sendPacket(new OnlineStatus(true));
		
		final String[] accounts = AuthServerCommunication.getInstance().getAccounts();
		for (final String account : accounts)
		{
			sendPacket(new PlayerInGame(account));
		}
	}
}