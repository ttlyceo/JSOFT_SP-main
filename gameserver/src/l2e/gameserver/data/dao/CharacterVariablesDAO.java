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
package l2e.gameserver.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Strings;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.instance.player.CharacterVariable;

public class CharacterVariablesDAO extends LoggerObject
{
	public static final String SELECT_SQL_QUERY = "SELECT name, value, expire_time FROM character_variables WHERE obj_id = ?";
	public static final String SELECT_FROM_PLAYER_SQL_QUERY = "SELECT value, expire_time FROM character_variables WHERE obj_id = ? AND name = ?";
	public static final String DELETE_SQL_QUERY = "DELETE FROM character_variables WHERE obj_id = ? AND name = ? LIMIT 1";
	public static final String DELETE_EXPIRED_SQL_QUERY = "DELETE FROM character_variables WHERE expire_time > 0 AND expire_time < ?";
	public static final String INSERT_SQL_QUERY = "REPLACE INTO character_variables (obj_id, name, value, expire_time) VALUES (?,?,?,?)";

	public CharacterVariablesDAO()
	{
		deleteExpiredVars();
	}

	private void deleteExpiredVars()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_EXPIRED_SQL_QUERY);
			statement.setLong(1, System.currentTimeMillis());
			statement.execute();
		}
		catch(final Exception e)
		{
			warn("deleteExpiredVars()", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean delete(int playerObjId, String varName)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_SQL_QUERY);
			statement.setInt(1, playerObjId);
			statement.setString(2, varName);
			statement.execute();
		}
		catch(final Exception e)
		{
			warn("delete(playerObjId,varName)", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	public boolean insert(int playerObjId, CharacterVariable var)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_SQL_QUERY);
			statement.setInt(1, playerObjId);
			statement.setString(2, var.getName());
			statement.setString(3, var.getValue());
			statement.setLong(4, var.getExpireTime());
			statement.executeUpdate();
		}
		catch(final Exception e)
		{
			warn("insert(playerObjId,var)", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	public List<CharacterVariable> restore(int playerObjId)
	{
		final List<CharacterVariable> result = new ArrayList<>();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_SQL_QUERY);
			statement.setInt(1, playerObjId);
			rset = statement.executeQuery();
			while(rset.next())
			{
				final long expireTime = rset.getLong("expire_time");
				if (expireTime > 0 && expireTime < System.currentTimeMillis())
				{
					continue;
				}
				result.add(new CharacterVariable(rset.getString("name"), Strings.stripSlashes(rset.getString("value")), expireTime));
			}
		}
		catch(final Exception e)
		{
			warn("restore(playerObjId)", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}

	public String getVarFromPlayer(int playerObjId, String var)
	{
		String value = null;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_FROM_PLAYER_SQL_QUERY);
			statement.setInt(1, playerObjId);
			statement.setString(2, var);
			rset = statement.executeQuery();
			if(rset.next())
			{
				final var expireTime = rset.getLong("expire_time");
				if(expireTime <= 0 || expireTime >= System.currentTimeMillis())
				{
					value = Strings.stripSlashes(rset.getString("value"));
				}
			}
		}
		catch(final Exception e)
		{
			warn("getVarFromPlayer(playerObjId,var)", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return value;
	}
	
	public static CharacterVariablesDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterVariablesDAO _instance = new CharacterVariablesDAO();
	}
}