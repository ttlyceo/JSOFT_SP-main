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
import l2e.gameserver.data.parser.HennaParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Henna;

public class CharacterHennaDAO extends LoggerObject
{
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";

	public void add(Player player, int symbolId, int slot)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(ADD_CHAR_HENNA);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, symbolId);
			statement.setInt(3, slot);
			statement.setInt(4, player.getClassIndex());
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed saving character henna.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void delete(Player player, int slot)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_CHAR_HENNA);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, slot);
			statement.setInt(3, player.getClassIndex());
			
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
	
	public void restore(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, player.getClassIndex());
			rset = statement.executeQuery();
			final Henna[] henna = new Henna[3];
			while (rset.next())
			{
				final var slot = rset.getInt("slot");
				if ((slot < 1) || (slot > 3))
				{
					continue;
				}
				
				final var symbolId = rset.getInt("symbol_id");
				if (symbolId == 0)
				{
					continue;
				}
				henna[slot - 1] = HennaParser.getInstance().getHenna(symbolId);
				player.setHenna(henna);
			}
		}
		catch (final Exception e)
		{
			warn("Failed restoing character " + this + " hennas.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public static CharacterHennaDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterHennaDAO _instance = new CharacterHennaDAO();
	}
}