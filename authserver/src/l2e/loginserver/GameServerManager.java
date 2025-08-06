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
package l2e.loginserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.apache.StringUtils;
import l2e.commons.dbutils.DbUtils;
import l2e.loginserver.database.DatabaseFactory;
import l2e.loginserver.network.ProxyServer;
import l2e.loginserver.network.communication.GameServer;

public class GameServerManager
{
	public static final int SUCCESS_GS_REGISTER = 0;
	public static final int FAIL_GS_REGISTER_DIFF_KEYS = 1;
	public static final int FAIL_GS_REGISTER_ID_ALREADY_USE = 2;
	public static final int FAIL_GS_REGISTER_ERROR = 3;
	
	private static final Logger _log = LoggerFactory.getLogger(GameServerManager.class);

	private final Map<Integer, GameServer> _gameServers = new TreeMap<>();
	private final Map<Integer, List<ProxyServer>> _gameServerProxys = new TreeMap<>();
	private final Map<Integer, ProxyServer> _proxyServers = new TreeMap<>();
	private final ReadWriteLock _lock = new ReentrantReadWriteLock();
	private final Lock _readLock = _lock.readLock();
	private final Lock _writeLock = _lock.writeLock();

	public GameServerManager()
	{
		load();
	}

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT `id`, `ip`, `port`, `age_limit`, `pvp`, `max_players`, `type`, `brackets`, `key`, `haProxy` FROM gameservers");
			rset = statement.executeQuery();

			while(rset.next())
			{
				final int id = rset.getInt("id");
				final GameServer gs = new GameServer(id, rset.getString("ip"), rset.getInt("port"), rset.getString("key"), rset.getInt("haProxy") == 1);
				gs.setAgeLimit(rset.getInt("age_limit"));
				gs.setPvp(rset.getInt("pvp") > 0);
				gs.setMaxPlayers(rset.getInt("max_players"));
				gs.setServerType(rset.getInt("type"));
				gs.setShowingBrackets(rset.getInt("brackets") > 0);
				_gameServers.put(id, gs);
			}
		}
		catch(final Exception e)
		{
			_log.warn("", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		_log.info(getClass().getSimpleName() + ": Loaded " + _gameServers.size() + " registered GameServer(s).");
		
		loadProxyServers();
		_log.info(getClass().getSimpleName() + ": Loaded " + _proxyServers.size() + " proxy server(s).");
		
	}

	public GameServer[] getGameServers()
	{
		_readLock.lock();
		try
		{
			final Set<GameServer> gameservers = new HashSet<>(_gameServers.values());
			return gameservers.toArray(new GameServer[gameservers.size()]);
		}
		finally
		{
			_readLock.unlock();
		}
	}

	public GameServer getGameServerById(int id)
	{
		_readLock.lock();
		try
		{
			return _gameServers.get(id);
		}
		finally
		{
			_readLock.unlock();
		}
	}

	public int registerGameServer(HostInfo host, GameServer gs)
	{
		_writeLock.lock();
		try
		{
			final GameServer pgs = _gameServers.get(host.getId());
			if(pgs != null)
			{
				final HostInfo phost = pgs.getHost(host.getId());
				if(phost == null || !StringUtils.equals(host.getKey(), phost.getKey()))
				{
					return FAIL_GS_REGISTER_DIFF_KEYS;
				}
			}
			else if(!Config.ACCEPT_NEW_GAMESERVER)
			{
				return FAIL_GS_REGISTER_ID_ALREADY_USE;
			}
			
			if(pgs == null || !pgs.isAuthed())
			{
				if(pgs != null)
				{
					pgs.removeHost(host.getId());
				}

				_gameServers.put(host.getId(), gs);
				return SUCCESS_GS_REGISTER;
			}
		}
		finally
		{
			_writeLock.unlock();
		}
		return FAIL_GS_REGISTER_ERROR;
	}
	
	private void loadProxyServers()
	{
		for (final Config.ProxyServerConfig psc : Config.PROXY_SERVERS_CONFIGS)
		{
			if (psc != null)
			{
				if (_gameServers.containsKey(psc.getProxyId()))
				{
					_log.warn("Won't load collided proxy with id " + psc.getProxyId() + ".");
				}
				else
				{
					final ProxyServer ps = new ProxyServer(psc.getOrigServerId(), psc.getProxyId(), psc.getMinAccessLevel(), psc.getMaxAccessLevel(), psc.isHideMain());
					try
					{
						final InetAddress inetAddress = InetAddress.getByName(psc.getPorxyHost());
						ps.setProxyAddr(inetAddress);
					}
					catch (final UnknownHostException uhe)
					{
						_log.warn("Can't load proxy", uhe);
						continue;
					}
					ps.setProxyPort(psc.getProxyPort());
					List<ProxyServer> proxyList = _gameServerProxys.get(ps.getOrigServerId());
					if (proxyList == null)
					{
						_gameServerProxys.put(ps.getOrigServerId(), proxyList = new LinkedList<>());
					}
					proxyList.add(ps);
					_proxyServers.put(ps.getProxyServerId(), ps);
				}
			}
		}
	}
	
	public List<ProxyServer> getProxyServersList(int gameServerId)
	{
		final List<ProxyServer> result = _gameServerProxys.get(gameServerId);
		return result != null ? result : Collections.emptyList();
	}
	
	public ProxyServer getProxyServerById(int proxyServerId)
	{
		return _proxyServers.get(proxyServerId);
	}
	
	public static final GameServerManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final GameServerManager _instance = new GameServerManager();
	}
}
