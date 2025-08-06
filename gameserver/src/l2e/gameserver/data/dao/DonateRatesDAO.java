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
import l2e.gameserver.data.parser.DonateRatesParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.DonateRateTempate;

public class DonateRatesDAO extends LoggerObject
{
	private static final String SELECT_SQL_QUERY = "SELECT id, expire_time FROM character_donate_rates WHERE charId=?";
	private static final String CLEANUP_SQL_QUERY = "DELETE FROM character_donate_rates WHERE expire_time > 0 AND expire_time < ?";
	private static final String INSERT_SQL_QUERY = "REPLACE INTO character_donate_rates(charId, id, expire_time) VALUES (?,?,?)";
	private static final String DELETE_SQL_QUERY = "DELETE FROM character_donate_rates WHERE charId=? AND id=?";

	public DonateRatesDAO()
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
			statement = con.prepareStatement(CLEANUP_SQL_QUERY);
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
	
	public void restore(Player player)
	{
		final Map<DonateRateTempate, Long> templates = new HashMap<>();
		final var instance = DonateRatesParser.getInstance();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_SQL_QUERY);
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final long expireTime = rset.getLong("expire_time");
				if (expireTime > 0 && expireTime < System.currentTimeMillis())
				{
					continue;
				}
				
				final var template = instance.getTemplate(rset.getInt("id"));
				if (template != null)
				{
					templates.put(template, expireTime);
				}
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
		
		if (!templates.isEmpty())
		{
			player.getDonateRates().addBonusRates(templates);
		}
	}

	public boolean insert(Player player, DonateRateTempate template, long expire)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_SQL_QUERY);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, template.getId());
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
	
	public void delete(Player player, int id)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_SQL_QUERY);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, id);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed remocing character henna.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static DonateRatesDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DonateRatesDAO _instance = new DonateRatesDAO();
	}
}