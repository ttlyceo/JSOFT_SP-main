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

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.buffer.PlayerScheme;
import l2e.gameserver.model.service.buffer.SchemeBuff;

/**
 * Rework by LordWinter 05.09.2019
 */
public class CharSchemesHolder extends LoggerObject
{
	public CharSchemesHolder()
	{
	}
	
	public void loadSchemes(Player player, Connection con)
	{
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			statement = con.prepareStatement("SELECT id, scheme_name, icon FROM character_scheme_list WHERE charId=?");
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var schemeId = rset.getInt("id");
				final var schemeName = rset.getString("scheme_name");
				final var iconId = rset.getInt("icon");
				player.getBuffSchemes().add(new PlayerScheme(schemeId, schemeName, iconId));
			}
			rset.close();
			statement.close();
		}
		catch (final SQLException e)
		{
			warn("Error while loading Scheme Content of the Player", e);
		}
		
		for (final var scheme : player.getBuffSchemes())
		{
			try
			{
				statement = con.prepareStatement("SELECT skill_id, skill_level, premium_lvl, buff_class FROM character_scheme_contents WHERE schemeId=?");
				statement.setInt(1, scheme.getSchemeId());
				rset = statement.executeQuery();
				while (rset.next())
				{
					final var skillId = rset.getInt("skill_id");
					final var skillLevel = rset.getInt("skill_level");
					final var premiumLvl = rset.getInt("premium_lvl");
					final var isDanceSlot = rset.getInt("buff_class") == 1 || rset.getInt("buff_class") == 2 ? true : false;
					scheme.addBuff(new SchemeBuff(skillId, skillLevel, premiumLvl, isDanceSlot));
				}
				rset.close();
				statement.close();
			}
			catch (final SQLException e)
			{
				warn("Error while loading Scheme Content of the Player", e);
			}
		}
	}
	
	public void addBuff(String scheme, String skill, String level, String premiumLvl, boolean isDanceSlot)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_scheme_contents (schemeId,skill_id,skill_level,premium_lvl,buff_class) VALUES (?,?,?,?,?)");
			statement.setString(1, scheme);
			statement.setString(2, skill);
			statement.setString(3, level);
			statement.setString(4, premiumLvl);
			statement.setInt(5, isDanceSlot ? 1 : 0);
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			warn("Error while adding Scheme Content", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void removeBuff(String scheme, String skill, String level)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_scheme_contents WHERE schemeId=? AND skill_id=? AND skill_level=? LIMIT 1");
			statement.setString(1, scheme);
			statement.setString(2, skill);
			statement.setString(3, level);
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			warn("Error while deleting Scheme Content", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void deleteScheme(int eventParam1)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_scheme_list WHERE id=? LIMIT 1");
			statement.setString(1, String.valueOf(eventParam1));
			statement.executeUpdate();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_scheme_contents WHERE schemeId=?");
			statement.setString(1, String.valueOf(eventParam1));
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			warn("Error while deleting Scheme Content", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateScheme(String name, int schemeId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE character_scheme_list SET scheme_name=? WHERE id=?");
			statement.setString(1, name);
			statement.setInt(2, schemeId);
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			warn("Error while updating Scheme List", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateIcon(int iconId, int schemeId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE character_scheme_list SET icon=? WHERE id=?");
			statement.setInt(1, iconId);
			statement.setInt(2, schemeId);
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			warn("Error while updating Scheme List", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static CharSchemesHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharSchemesHolder _instance = new CharSchemesHolder();
	}
}