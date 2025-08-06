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

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.games.MiniGameScoreManager;

public class CharMiniGameHolder extends LoggerObject
{
	public void select()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT characters.char_name AS name, character_minigame_score.score AS score, character_minigame_score.charId AS charId FROM characters, character_minigame_score WHERE characters.charId=character_minigame_score.charId");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var name = rset.getString("name");
				final var score = rset.getInt("score");
				final var objectId = rset.getInt("charId");
				
				MiniGameScoreManager.getInstance().addScore(objectId, score, name);
			}
		}
		catch (final Exception e)
		{
			warn("Exception: " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void replace(int objectId, int score)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO character_minigame_score(charId, score) VALUES (?, ?)");
			statement.setInt(1, objectId);
			statement.setInt(2, score);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Exception: " + e, e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static CharMiniGameHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharMiniGameHolder _instance = new CharMiniGameHolder();
	}
}