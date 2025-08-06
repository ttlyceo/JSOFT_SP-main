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
package l2e.loginserver.accounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.HashIntObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utils.Net;
import org.utils.NetList;

import l2e.commons.apache.tuple.ImmutablePair;
import l2e.commons.apache.tuple.Pair;
import l2e.commons.dbutils.DbUtils;
import l2e.loginserver.database.DatabaseFactory;

public class Account
{
	private static final Logger _log = LoggerFactory.getLogger(Account.class);
	
	private final String _login;
	private String _passwordHash;
	private int _accessLevel;
	private int _banExpire;
	private String _lastIP;
	private int _lastAccess;
	private int _lastServer;
	private String _allowedHwid = "", _allowedIp = "";
	private final NetList _allowedIpList = new NetList();
	
	private final IntObjectMap<Pair<Integer, int[]>> _serversInfo = new HashIntObjectMap<>(2);
	
	public Account(String login)
	{
		_login = login;
	}
	
	public String getLogin()
	{
		return _login;
	}
	
	public String getPasswordHash()
	{
		return _passwordHash;
	}
	
	public void setPasswordHash(String passwordHash)
	{
		_passwordHash = passwordHash;
	}
	
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	public void setAccessLevel(int accessLevel)
	{
		_accessLevel = accessLevel;
	}
	
	public int getBanExpire()
	{
		return _banExpire;
	}
	
	public void setBanExpire(int banExpire)
	{
		_banExpire = banExpire;
	}
	
	public void setLastIP(String lastIP)
	{
		_lastIP = lastIP;
	}
	
	public String getLastIP()
	{
		return _lastIP;
	}
	
	public int getLastAccess()
	{
		return _lastAccess;
	}
	
	public void setLastAccess(int lastAccess)
	{
		_lastAccess = lastAccess;
	}
	
	public int getLastServer()
	{
		return _lastServer;
	}
	
	public void setLastServer(int lastServer)
	{
		_lastServer = lastServer;
	}
	
	public void addAccountInfo(int serverId, int size, int[] deleteChars)
	{
		_serversInfo.put(serverId, new ImmutablePair<>(size, deleteChars));
	}
	
	public Pair<Integer, int[]> getAccountInfo(int serverId)
	{
		return _serversInfo.get(serverId);
	}
	
	public String getAllowedIp()
	{
		return _allowedIp;
	}
	
	public boolean isAllowedIP(String ip)
	{
		return _allowedIpList.isEmpty() || _allowedIpList.matches(ip);
	}
	
	public void setAllowedIp(String allowedIP)
	{
		_allowedIpList.clear();
		_allowedIp = allowedIP;
		
		if (allowedIP.isEmpty())
		{
			return;
		}
		
		final String[] masks = allowedIP.split("[\\s,;]+");
		for (final String mask : masks)
		{
			try
			{
				_allowedIpList.add(Net.valueOf(mask));
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}
	
	public String getAllowedHwid()
	{
		return _allowedHwid;
	}
	
	public void setAllowedHwid(String allowedHwid)
	{
		_allowedHwid = allowedHwid;
	}
	
	@Override
	public String toString()
	{
		return _login;
	}
	
	public void restore()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT password, accessLevel, banExpire, allowIp, lastServer, lastIP, lastAccess FROM accounts WHERE login = ?");
			statement.setString(1, _login);
			rset = statement.executeQuery();
			if (rset.next())
			{
				setPasswordHash(rset.getString("password"));
				setAccessLevel(rset.getInt("accessLevel"));
				setBanExpire(rset.getInt("banExpire"));
				setAllowedIp(rset.getString("allowIp"));
				setLastServer(rset.getInt("lastServer"));
				setLastIP(rset.getString("lastIP"));
				setLastAccess(rset.getInt("lastAccess"));
			}
			statement.close();
			rset.close();
			statement = con.prepareStatement("SELECT hwid FROM hwid_lock WHERE login = ?");
			statement.setString(1, _login);
			rset = statement.executeQuery();
			if (rset.next())
			{
				setAllowedHwid(rset.getString("hwid"));
			}
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void save()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO accounts (login, password) VALUES(?,?)");
			statement.setString(1, getLogin());
			statement.setString(2, getPasswordHash());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		_log.info("Auto created account '" + getLogin() + "'.");
	}

	public void update()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET password = ?, accessLevel = ?, banExpire = ?, allowIp = ?, lastServer = ?, lastIP = ?, lastAccess = ? WHERE login = ?");
			statement.setString(1, getPasswordHash());
			statement.setInt(2, getAccessLevel());
			statement.setInt(3, getBanExpire());
			statement.setString(4, getAllowedIp());
			statement.setInt(5, getLastServer());
			statement.setString(6, getLastIP());
			statement.setInt(7, getLastAccess());
			statement.setString(8, getLogin());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warn("", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}