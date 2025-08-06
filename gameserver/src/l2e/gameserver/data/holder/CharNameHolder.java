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
package l2e.gameserver.data.holder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;

public class CharNameHolder extends LoggerObject
{
	private final Map<Integer, String> _chars = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _accessLevels = new ConcurrentHashMap<>();
	
	protected CharNameHolder()
	{
		if (Config.CACHE_CHAR_NAMES)
		{
			loadAll();
		}
	}

	public static CharNameHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public final void addName(Player player)
	{
		if (player != null && player.getName(null) != null)
		{
			if (!player.getName(null).equals(_chars.get(player.getObjectId())))
			{
				_chars.put(player.getObjectId(), player.getName(null));
				_accessLevels.put(player.getObjectId(), player.getAccessLevel().getLevel());
			}
		}
	}
	
	public final void removeName(int objId)
	{
		_chars.remove(objId);
		_accessLevels.remove(objId);
	}
	
	public final int getIdByName(String name)
	{
		if ((name == null) || name.isEmpty())
		{
			return -1;
		}
		
		final Iterator<Entry<Integer, String>> it = _chars.entrySet().iterator();
		
		Map.Entry<Integer, String> pair;
		while (it.hasNext())
		{
			pair = it.next();
			if (pair.getValue().equalsIgnoreCase(name))
			{
				return pair.getKey();
			}
		}
		
		if (Config.CACHE_CHAR_NAMES)
		{
			return -1;
		}
		
		var id = -1;
		var accessLevel = 0;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT charId,accesslevel FROM characters WHERE char_name=?");
			statement.setString(1, name);
			rset = statement.executeQuery();
			while (rset.next())
			{
				id = rset.getInt(1);
				accessLevel = rset.getInt(2);
			}
		}
		catch (final SQLException e)
		{
			warn("Could not check existing char name: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (id > 0)
		{
			_chars.put(id, name);
			_accessLevels.put(id, accessLevel);
			return id;
		}
		return -1;
	}
	
	public final String getNameById(int id)
	{
		if (id <= 0)
		{
			return null;
		}
		
		var name = _chars.get(id);
		if (name != null)
		{
			return name;
		}
		
		if (Config.CACHE_CHAR_NAMES)
		{
			return null;
		}
		
		var accessLevel = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name,accesslevel FROM characters WHERE charId=?");
			statement.setInt(1, id);
			rset = statement.executeQuery();
			while (rset.next())
			{
				name = rset.getString(1);
				accessLevel = rset.getInt(2);
			}
		}
		catch (final SQLException e)
		{
			warn("Could not check existing char id: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if ((name != null) && !name.isEmpty())
		{
			_chars.put(id, name);
			_accessLevels.put(id, accessLevel);
			return name;
		}
		return null;
	}
	
	public final int getAccessLevelById(int objectId)
	{
		if (getNameById(objectId) != null)
		{
			return _accessLevels.get(objectId);
		}
		
		return 0;
	}
	
	public synchronized boolean doesCharNameExist(String name)
	{
		var result = true;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
			statement.setString(1, name);
			rset = statement.executeQuery();
			result = rset.next();
		}
		catch (final SQLException e)
		{
			warn("Could not check existing charname: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}
	
	public int accountCharNumber(String account)
	{
		var number = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT COUNT(char_name) FROM characters WHERE account_name=?");
			statement.setString(1, account);
			rset = statement.executeQuery();
			while (rset.next())
			{
				number = rset.getInt(1);
			}
		}
		catch (final SQLException e)
		{
			warn("Could not check existing char number: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return number;
	}
	
	private void loadAll()
	{
		String name;
		var id = -1;
		var accessLevel = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT charId,char_name,accesslevel FROM characters");
			rset = statement.executeQuery();
			while (rset.next())
			{
				id = rset.getInt(1);
				name = rset.getString(2);
				accessLevel = rset.getInt(3);
				_chars.put(id, name);
				_accessLevels.put(id, accessLevel);
			}
		}
		catch (final SQLException e)
		{
			warn("Could not load char name: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		info("Loaded " + _chars.size() + " char names.");
	}
	
	private static class SingletonHolder
	{
		protected static final CharNameHolder _instance = new CharNameHolder();
	}
}