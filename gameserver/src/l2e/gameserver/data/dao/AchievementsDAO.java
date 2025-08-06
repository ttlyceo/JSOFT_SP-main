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
import java.sql.SQLException;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;

public class AchievementsDAO extends LoggerObject
{
	private static final String RESTORE_CHAR_ACHIVEMENTS = "SELECT id,points FROM character_achievements WHERE charId=?";
	private static final String ADD_CHAR_ACHIVEMENTS = "REPLACE INTO character_achievements (charId,id,points) VALUES (?,?,?)";
	private static final String DELETE_CHAR_ACHIVEMENTS = "DELETE FROM character_achievements WHERE charId=?";
	
	public void removeAchievements(Player player)
	{
		if (!AchievementManager.getInstance().isActive())
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_CHAR_ACHIVEMENTS);
			statement.setInt(1, player.getObjectId());
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Error could not delete skill: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void saveAchievements(Player player)
	{
		if (!AchievementManager.getInstance().isActive())
		{
			return;
		}
		
		if (player.getCounters().getAchievements() == null || player.getCounters().getAchievements().isEmpty())
		{
			return;
		}
		
		removeAchievements(player);
		
		Connection con = null;
		PreparedStatement ps = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement(ADD_CHAR_ACHIVEMENTS);
			con.setAutoCommit(false);
			for (final var id : player.getCounters().getAchievements().keySet())
			{
				final var points = player.getCounters().getAchievements().get(id);
				if (points <= 0)
				{
					continue;
				}
				
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, id);
				ps.setLong(3, points);
				ps.addBatch();
			}
			ps.executeBatch();
			con.commit();
		}
		catch (final SQLException e)
		{
			warn("Error could not save char achievements: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, ps);
		}
	}
	
	public void restoreAchievements(Player player)
	{
		if (!AchievementManager.getInstance().isActive())
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(RESTORE_CHAR_ACHIVEMENTS);
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				player.getCounters().setAchievementInfo(rset.getInt("id"), rset.getLong("points"), true);
			}
		}
		catch (final Exception e)
		{
			warn("Could not restore character achievements " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static AchievementsDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AchievementsDAO _instance = new AchievementsDAO();
	}
}