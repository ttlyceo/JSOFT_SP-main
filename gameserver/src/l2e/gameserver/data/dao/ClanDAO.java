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
import java.util.HashMap;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;

public class ClanDAO extends LoggerObject
{
	private static final String SELECT_CLAN_PRIVILEGES = "SELECT `privs`, `rank`, `party` FROM `clan_privs` WHERE clan_id=?";
	private static final String INSERT_CLAN_PRIVILEGES = "INSERT INTO `clan_privs` (`clan_id`, `rank`, `party`, `privs`) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `privs`=?";
	
	public Map<Integer, Integer> getPrivileges(int clanId)
	{
		final Map<Integer, Integer> result = new HashMap<>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_CLAN_PRIVILEGES);
			statement.setInt(1, clanId);
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var rank = rset.getInt("rank");
				if (rank == -1)
				{
					continue;
				}
				result.put(rank, rset.getInt("privs"));
			}
		}
		catch (final Exception ex)
		{
			warn("Unable to restore clan privileges for clan Id " + clanId, ex);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}
	
	public void storePrivileges(int clanId, int rank, int privileges)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_CLAN_PRIVILEGES);
			statement.setInt(1, clanId);
			statement.setInt(2, rank);
			statement.setInt(3, 0);
			statement.setInt(4, privileges);
			statement.setInt(5, privileges);
			statement.execute();
		}
		catch (final Exception ex)
		{
			warn("Unable to store clan privileges for clan Id " + clanId, ex);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static ClanDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanDAO _instance = new ClanDAO();
	}
}
