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
package l2e.loginserver.network.serverpackets;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.apache.ArrayUtils;
import l2e.loginserver.GameServerManager;
import l2e.loginserver.accounts.Account;

public final class ServerList extends LoginServerPacket
{
	private static final Logger _log = LoggerFactory.getLogger(ServerList.class);

	private final List<ServerData> _servers = new ArrayList<>();
	private final int _lastServer;
	private int _paddedBytes;

	private static class ServerData
	{
		int _serverId;
		InetAddress _adress;
		int _port;
		int _online;
		int _maxPlayers;
		boolean _status;
		boolean _pvp;
		boolean _brackets;
		int _type;
		int _ageLimit;
		int _playerSize;
		int[] _deleteChars;

		ServerData(int serverId, InetAddress adress, int port, boolean pvp, boolean brackets, int type, int online, int maxPlayers, boolean status, int size, int ageLimit, int[] d)
		{
			_serverId = serverId;
			_adress = adress;
			_port = port;
			_pvp = pvp;
			_brackets = brackets;
			_type = type;
			_online = online;
			_maxPlayers = maxPlayers;
			_status = status;
			_playerSize = size;
			_ageLimit = ageLimit;
			_deleteChars = d;
		}
	}

	public ServerList(Account account)
	{
		_lastServer = account.getLastServer();
		_paddedBytes = 1;

		for (final var gs : GameServerManager.getInstance().getGameServers())
		{
			for (final var host : gs.getHosts())
			{
				InetAddress adress;
				try
				{
					String adrStr = host.checkAddress(account.getLastIP());
					if(adrStr == null)
					{
						continue;
					}

					if (adrStr.equals("*"))
					{
						adrStr = gs.getConnection() != null ? gs.getConnection().getIpAddress() : "127.0.0.1";
					}
					adress = InetAddress.getByName(adrStr);
				}
				catch(final UnknownHostException e)
				{
					_log.warn("Error with gameserver host adress: " + e, e);
					continue;
				}

				int playerSize;
				int[] deleteChars;

				final var entry = account.getAccountInfo(host.getId());
				if(entry != null)
				{
					playerSize = entry.getKey();
					deleteChars = entry.getValue();
				}
				else
				{
					playerSize = 0;
					deleteChars = ArrayUtils.EMPTY_INT_ARRAY;
				}

				_paddedBytes += (3 + (4 * deleteChars.length));

				final var proxyServers = GameServerManager.getInstance().getProxyServersList(host.getId());
				if (!proxyServers.isEmpty())
				{
					var isHide = false;
					for (final var ps : proxyServers)
					{
						if (account.getAccessLevel() < ps.getMinAccessLevel() || account.getAccessLevel() > ps.getMaxAccessLevel())
						{
							continue;
						}
						
						if (ps.isHideMain())
						{
							isHide = true;
						}
						_servers.add(new ServerData(ps.getProxyServerId(), ps.getProxyAddr(), ps.getProxyPort(), gs.isPvp(), gs.isShowingBrackets(), gs.getServerType(), gs.getOnline(), gs.getMaxPlayers(), gs.isOnline(), playerSize, gs.getAgeLimit(), deleteChars));
					}
					
					if (!isHide)
					{
						_servers.add(new ServerData(host.getId(), adress, host.getPort(), gs.isPvp(), gs.isShowingBrackets(), gs.getServerType(), gs.getOnline(), gs.getMaxPlayers(), gs.isOnline(), playerSize, gs.getAgeLimit(), deleteChars));
					}
				}
				else
				{
					_servers.add(new ServerData(host.getId(), adress, host.getPort(), gs.isPvp(), gs.isShowingBrackets(), gs.getServerType(), gs.getOnline(), gs.getMaxPlayers(), gs.isOnline(), playerSize, gs.getAgeLimit(), deleteChars));
				}
			}
		}
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x04);
		writeC(_servers.size());
		writeC(_lastServer);
		for (final var server : _servers)
		{
			writeC(server._serverId);
			final byte[] raw = server._adress.getAddress();
			writeC(raw[0] & 0xff);
			writeC(raw[1] & 0xff);
			writeC(raw[2] & 0xff);
			writeC(raw[3] & 0xff);
			writeD(server._port);
			writeC(server._ageLimit);
			writeC(server._pvp ? 0x01 : 0x00);
			writeH(server._online);
			writeH(server._maxPlayers);
			writeC(server._status ? 0x01 : 0x00);
			writeD(server._type);
			writeC(server._brackets ? 0x01 : 0x00);
		}

		writeH(_paddedBytes);
		writeC(_servers.size());
		for (final var server : _servers)
		{
			writeC(server._serverId);
			writeC(server._playerSize);
			writeC(server._deleteChars.length);
			for (final int t : server._deleteChars)
			{
				writeD((int)(t - System.currentTimeMillis() / 1000L));
			}
		}
	}
}