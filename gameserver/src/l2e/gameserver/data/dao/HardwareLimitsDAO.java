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
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;

public class HardwareLimitsDAO extends LoggerObject
{
	private static final String SELECT_SQL_QUERY = "SELECT hardware, windows_limit, limit_expire FROM hardware_limits";
	private static final String DELETE_SQL_QUERY = "DELETE FROM hardware_limits WHERE limit_expire > 0 AND limit_expire < ?";
	private static final String INSERT_SQL_QUERY = "REPLACE INTO hardware_limits(hardware, windows_limit, limit_expire) VALUES (?,?,?)";

	public HardwareLimitsDAO()
	{
		deleteExpired();
	}
	
	private void deleteExpired()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_SQL_QUERY);
			statement.setLong(1, System.currentTimeMillis());
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("deleteExpired ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void restore(Map<String, long[]> hardWareList)
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
				final long expireTime = rset.getLong("limit_expire");
				if (expireTime > 0 && expireTime < System.currentTimeMillis())
				{
					continue;
				}
				hardWareList.put(rset.getString("hardware"), new long[]
				{
				        rset.getInt("windows_limit"), rset.getLong("limit_expire")
				});
			}
		}
		catch (final Exception e)
		{
			warn("restore ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public boolean insert(String hardware, int limit, long expire)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_SQL_QUERY);
			statement.setString(1, hardware);
			statement.setInt(2, limit);
			statement.setLong(3, expire);
			statement.execute();
		}
		catch(final Exception e)
		{
			warn("insert " + e, e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}
	
	public static HardwareLimitsDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final HardwareLimitsDAO _instance = new HardwareLimitsDAO();
	}
}