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

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Strings;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.instance.player.CharacterVariable;

public class ClanVariablesDAO extends LoggerObject
{
	public static final String SELECT_SQL_QUERY = "SELECT * FROM clan_variables";
	public static final String DELETE_SQL_QUERY = "DELETE FROM clan_variables WHERE obj_id = ? AND name = ? LIMIT 1";
	public static final String DELETE_EXPIRED_SQL_QUERY = "DELETE FROM clan_variables WHERE expire_time > 0 AND expire_time < ?";
	public static final String INSERT_SQL_QUERY = "REPLACE INTO clan_variables (obj_id, name, value, expire_time) VALUES (?,?,?,?)";

	public ClanVariablesDAO()
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

	public boolean delete(int clanId, String varName)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_SQL_QUERY);
			statement.setInt(1, clanId);
			statement.setString(2, varName);
			statement.execute();
		}
		catch(final Exception e)
		{
			warn("delete(clanId,varName)", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	public boolean insert(int clanId, CharacterVariable var)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_SQL_QUERY);
			statement.setInt(1, clanId);
			statement.setString(2, var.getName());
			statement.setString(3, var.getValue());
			statement.setLong(4, var.getExpireTime());
			statement.executeUpdate();
		}
		catch(final Exception e)
		{
			warn("insert(clanId,var)", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	public void restore()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_SQL_QUERY);
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var clanId = rset.getInt("obj_id");
				final Clan clan = ClanHolder.getInstance().getClan(clanId);
				if (clan != null)
				{
					final long expireTime = rset.getLong("expire_time");
					if (expireTime > 0 && expireTime < System.currentTimeMillis())
					{
						continue;
					}
					clan.restoreVar(new CharacterVariable(rset.getString("name"), Strings.stripSlashes(rset.getString("value")), expireTime));
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error restoring ClanVariables.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public static ClanVariablesDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanVariablesDAO _instance = new ClanVariablesDAO();
	}
}