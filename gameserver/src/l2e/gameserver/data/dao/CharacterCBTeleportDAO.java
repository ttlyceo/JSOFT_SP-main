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
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.PcTeleportTemplate;

public class CharacterCBTeleportDAO extends LoggerObject
{
	private static final String INSERT_TELEPORT = "INSERT INTO character_teleport (charId,xPos,yPos,zPos,name) VALUES(?,?,?,?,?)";
	private static final String UPDATE_TELEPORT = "UPDATE character_teleport SET xPos=?, yPos=?, zPos=? WHERE charId=? AND name=?;";
	private static final String SELECT_TELEPORT_ALL = "SELECT COUNT(*) FROM character_teleport WHERE charId=?;";
	private static final String SELECT_TELEPORT_ALL2 = "SELECT * FROM character_teleport WHERE charId=? AND name=?;";
	private static final String SELECT_TELEPORT_NAME = "SELECT COUNT(*) FROM character_teleport WHERE charId=? AND name=?;";
	private static final String RESTORE_TELEPORT = "SELECT * FROM character_teleport WHERE charId=?;";
	private static final String DELETE_TELEPORT = "DELETE FROM character_teleport WHERE charId=? AND TpId=?;";
	
	public boolean add(Player player, String name)
	{
		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement statement1 = null;
		PreparedStatement statement2 = null;
		ResultSet rset = null;
		ResultSet rset1 = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_TELEPORT_ALL);
			statement.setLong(1, player.getObjectId());
			rset = statement.executeQuery();
			rset.next();
			if (rset.getInt(1) < Config.COMMUNITY_TELEPORT_TABS)
			{
				statement1 = con.prepareStatement(SELECT_TELEPORT_NAME);
				statement1.setLong(1, player.getObjectId());
				statement1.setString(2, name);
				rset1 = statement1.executeQuery();
				rset1.next();
				if (rset1.getInt(1) == 0)
				{
					statement2 = con.prepareStatement(INSERT_TELEPORT);
					statement2.setInt(1, player.getObjectId());
					statement2.setInt(2, player.getX());
					statement2.setInt(3, player.getY());
					statement2.setInt(4, player.getZ());
					statement2.setString(5, name);
					statement2.execute();
					addToPlayer(player, name);
					statement2.close();
				}
				else
				{
					statement2 = con.prepareStatement(UPDATE_TELEPORT);
					statement2.setInt(1, player.getObjectId());
					statement2.setInt(2, player.getX());
					statement2.setInt(3, player.getY());
					statement2.setInt(4, player.getZ());
					statement2.setString(5, name);
					statement2.execute();
					for (final var tpl : player.getCBTeleports())
					{
						if (tpl != null && tpl.getName().equals(name))
						{
							tpl.setX(player.getX());
							tpl.setY(player.getY());
							tpl.setZ(player.getZ());
						}
					}
					statement2.close();
				}
				statement1.close();
				rset1.close();
				return true;
			}
			return false;
		}
		catch (final Exception e)
		{
			warn("Could not insert character community teleport data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return false;
	}
	
	private void addToPlayer(Player player, String name)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_TELEPORT_ALL2);
			statement.setLong(1, player.getObjectId());
			statement.setString(2, name);
			rset = statement.executeQuery();
			while (rset.next())
			{
				player.addCBTeleport(rset.getInt("TpId"), new PcTeleportTemplate(rset.getInt("TpId"), rset.getString("name"), rset.getInt("xPos"), rset.getInt("yPos"), rset.getInt("zPos")));
			}
		}
		catch (final Exception e)
		{
			warn("Could not restore character community teleport data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void restore(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(RESTORE_TELEPORT);
			statement.setLong(1, player.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				player.addCBTeleport(rset.getInt("TpId"), new PcTeleportTemplate(rset.getInt("TpId"), rset.getString("name"), rset.getInt("xPos"), rset.getInt("yPos"), rset.getInt("zPos")));
			}
		}
		catch (final Exception e)
		{
			warn("Could not restore character community teleport data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void delete(Player player, int id)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_TELEPORT);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, id);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Could not delete character community teleport data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static CharacterCBTeleportDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterCBTeleportDAO _instance = new CharacterCBTeleportDAO();
	}
}