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

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.templates.TimeSkillTemplate;

public class TimeSkillsDAO extends LoggerObject
{
	public static final String SELECT_SQL_QUERY = "SELECT * FROM donate_time_skills WHERE ORDER BY objId";
	public static final String DELETE_SQL_QUERY = "DELETE FROM donate_time_skills WHERE objId = ? AND skillId = ? AND skillLevel = ? LIMIT 1";
	public static final String DELETE_EXPIRED_SQL_QUERY = "DELETE FROM donate_time_skills WHERE expire_time > 0 AND expire_time < ?";
	public static final String INSERT_SQL_QUERY = "REPLACE INTO donate_time_skills (objId, skillId, skillLevel, clan_skill, expire_time) VALUES (?,?,?,?,?)";

	public void deleteExpiredSkills()
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
			warn("deleteExpiredSkills()", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean delete(int objId, int skillId, int skillLevel)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_SQL_QUERY);
			statement.setInt(1, objId);
			statement.setInt(2, skillId);
			statement.setInt(3, skillLevel);
			statement.execute();
		}
		catch(final Exception e)
		{
			warn("delete(objId,skillId,skillLevel)", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	public boolean insert(TimeSkillTemplate tpl, long time)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_SQL_QUERY);
			statement.setInt(1, tpl.getId());
			statement.setInt(2, tpl.getSkillId());
			statement.setInt(3, tpl.getSkillLevel());
			statement.setInt(4, tpl.isClanSkill() ? 1 : 0);
			statement.setLong(5, time);
			statement.executeUpdate();
		}
		catch(final Exception e)
		{
			warn("insert(tpl,time)", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}
	
	public static TimeSkillsDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final TimeSkillsDAO _instance = new TimeSkillsDAO();
	}
}