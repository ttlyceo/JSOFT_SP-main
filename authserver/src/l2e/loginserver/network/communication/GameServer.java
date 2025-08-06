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
package l2e.loginserver.network.communication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.loginserver.Config;
import l2e.loginserver.database.DatabaseFactory;

public class GameServer
{
	private static final Logger _log = LoggerFactory.getLogger(GameServer.class);
	
	private final Map<Integer, HostInfo> _hosts = new HashMap<>();
	private int _serverType;
	private int _ageLimit;
	private int _protocol;
	private boolean _isOnline;
	private boolean _isPvp;
	private boolean _isShowingBrackets;
	private boolean _isGmOnly;
	private int _maxPlayers;
	private GameServerConnection _conn;
	private boolean _isAuthed;
	private final Set<String> _accounts = new CopyOnWriteArraySet<>();
	
	public GameServer(GameServerConnection conn)
	{
		_conn = conn;
	}
	
	public GameServer(int id, String ip, int port, String key, boolean allowHaProxy)
	{
		_conn = null;
		addHost(new HostInfo(id, ip, port, key, allowHaProxy));
	}
	
	public void addHost(HostInfo host)
	{
		_hosts.put(host.getId(), host);
	}
	
	public HostInfo removeHost(int id)
	{
		return _hosts.remove(id);
	}
	
	public HostInfo getHost(int id)
	{
		return _hosts.get(id);
	}
	
	public HostInfo[] getHosts()
	{
		return _hosts.values().toArray(new HostInfo[_hosts.size()]);
	}
	
	public void setAuthed(boolean isAuthed)
	{
		_isAuthed = isAuthed;
	}
	
	public boolean isAuthed()
	{
		return _isAuthed;
	}
	
	public void setConnection(GameServerConnection conn)
	{
		_conn = conn;
	}
	
	public GameServerConnection getConnection()
	{
		return _conn;
	}
	
	public void setMaxPlayers(int maxPlayers)
	{
		_maxPlayers = maxPlayers;
	}
	
	public int getMaxPlayers()
	{
		return _maxPlayers;
	}
	
	public int getOnline()
	{
		return (int) ((_accounts.size() * Config.FAKE_ONLINE) + Config.FAKE_ONLINE_MULTIPLIER);
	}
	
	public Set<String> getAccounts()
	{
		return _accounts;
	}
	
	public void addAccount(String account)
	{
		_accounts.add(account);
	}
	
	public void removeAccount(String account)
	{
		_accounts.remove(account);
	}
	
	public void setDown()
	{
		setAuthed(false);
		setConnection(null);
		setOnline(false);
		_accounts.clear();
	}
	
	public void sendPacket(SendablePacket packet)
	{
		final GameServerConnection conn = getConnection();
		if (conn != null)
		{
			conn.sendPacket(packet);
		}
	}
	
	public int getServerType()
	{
		return _serverType;
	}
	
	public boolean isOnline()
	{
		return _isOnline;
	}
	
	public void setOnline(boolean online)
	{
		_isOnline = online;
	}
	
	public void setServerType(int serverType)
	{
		_serverType = serverType;
	}
	
	public boolean isPvp()
	{
		return _isPvp;
	}
	
	public void setPvp(boolean pvp)
	{
		_isPvp = pvp;
	}
	
	public boolean isShowingBrackets()
	{
		return _isShowingBrackets;
	}
	
	public void setShowingBrackets(boolean showingBrackets)
	{
		_isShowingBrackets = showingBrackets;
	}
	
	public boolean isGmOnly()
	{
		return _isGmOnly;
	}
	
	public void setGmOnly(boolean gmOnly)
	{
		_isGmOnly = gmOnly;
	}
	
	public int getAgeLimit()
	{
		return _ageLimit;
	}
	
	public void setAgeLimit(int ageLimit)
	{
		_ageLimit = ageLimit;
	}
	
	public int getProtocol()
	{
		return _protocol;
	}

	public void setProtocol(int protocol)
	{
		_protocol = protocol;
	}

	public boolean store()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			for (final HostInfo host : _hosts.values())
			{
				statement = con.prepareStatement("REPLACE INTO gameservers (`id`, `ip`, `port`, `age_limit`, `pvp`, `max_players`, `type`, `brackets`, `key`, `haProxy`) VALUES(?,?,?,?,?,?,?,?,?,?)");
				int i = 0;
				statement.setInt(++i, host.getId());
				statement.setString(++i, host.getAddress());
				statement.setShort(++i, (short) host.getPort());
				statement.setByte(++i, (byte) getAgeLimit());
				statement.setByte(++i, (byte) (isPvp() ? 1 : 0));
				statement.setShort(++i, (short) getMaxPlayers());
				statement.setInt(++i, getServerType());
				statement.setByte(++i, (byte) (isShowingBrackets() ? 1 : 0));
				statement.setString(++i, host.getKey());
				statement.setInt(++i, host.isAllowHaProxy() ? 1 : 0);
				statement.execute();
				DbUtils.closeQuietly(statement);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error while store gameserver: " + e, e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}
}