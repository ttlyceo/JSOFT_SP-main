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
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SellBuffHolder;

public class CharacterSellBuffsDAO extends LoggerObject
{
	public static final String SELECT_SQL_QUERY = "SELECT * FROM character_offline_buffs WHERE charId = ?";
	public static final String DELETE_SQL_QUERY = "DELETE FROM character_offline_buffs WHERE charId = ?";
	public static final String INSERT_SQL_QUERY = "INSERT INTO character_offline_buffs (`charId`,`skillId`,`level`,`itemId`,`price`) VALUES (?,?,?,?,?)";

	public void saveSellBuffList(Player player)
	{
		cleanSellBuffList(player);
		if (player.isSellingBuffs())
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(INSERT_SQL_QUERY);
				for (final var holder : player.getSellingBuffs())
				{
					statement.setInt(1, player.getObjectId());
					statement.setInt(2, holder.getId());
					statement.setLong(3, holder.getLvl());
					statement.setLong(4, holder.getItemId());
					statement.setLong(5, holder.getPrice());
					statement.executeUpdate();
					statement.clearParameters();
				}
			}
			catch (final Exception e)
			{
				warn("Error while saving offline sellbuffs: " + player.getObjectId() + " " + e, e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public void restoreSellBuffList(Player player)
	{
		player.getSellingBuffs().clear();
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
				player.getSellingBuffs().add(new SellBuffHolder(rset.getInt("skillId"), rset.getInt("level"), rset.getInt("itemId"), rset.getLong("price")));
			}
		}
		catch (final Exception e)
		{
			warn("Error while restore offline sellbuffs: " + player.getObjectId() + " " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void cleanSellBuffList(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_SQL_QUERY);
			statement.setInt(1, player.getObjectId());
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed to clean up offline sellbuffs.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static CharacterSellBuffsDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterSellBuffsDAO _instance = new CharacterSellBuffsDAO();
	}
}