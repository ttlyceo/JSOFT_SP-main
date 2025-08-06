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
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.PlayerTaskTemplate;

public class DailyTasksDAO extends LoggerObject
{
	private static final String INSERT_DAILY = "INSERT INTO daily_tasks (obj_Id, taskId, type, params, status, rewarded) VALUES (?,?,?,?,?,?)";
	private static final String UPDATE_DAILY = "UPDATE daily_tasks SET status=? WHERE obj_Id=? and taskId=?";
	private static final String UPDATE_DAILY_REWARD = "UPDATE daily_tasks SET rewarded=? WHERE obj_Id=? and taskId=?";
	private static final String UPDATE_DAILY_PARAM = "UPDATE daily_tasks SET params=? WHERE obj_Id=? and taskId=?";
	private static final String RESTORE_DAILY = "SELECT * FROM daily_tasks WHERE obj_Id=?";
	private static final String REMOVE_DAILY = "DELETE FROM daily_tasks WHERE obj_Id=? and taskId=? LIMIT 1";
	
	private static final String INSERT_TASK_COUNT = "INSERT INTO daily_tasks_count (" + DailyTaskManager.getInstance().getColumnCheck() + ", dailyCount, weeklyCount, monthCount) VALUES (?,?,?,?)";
	private static final String UPDATE_TASK_COUNT = "UPDATE daily_tasks_count SET dailyCount=?,weeklyCount=?,monthCount=? WHERE " + DailyTaskManager.getInstance().getColumnCheck() + "=?";
	public void addNewDailyTask(Player player, final PlayerTaskTemplate template)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_DAILY);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, template.getId());
			statement.setString(3, template.getSort());
			statement.setInt(4, 0);
			statement.setInt(5, 0);
			statement.setInt(6, 0);
			statement.executeUpdate();
			
			if (template.getSort().equalsIgnoreCase("daily"))
			{
				player.addActiveDailyTasks(template.getId(), template);
				DailyTaskManager.getInstance().updateTaskCount(player, 1, "daily");
			}
			else if (template.getSort().equalsIgnoreCase("weekly"))
			{
				player.addActiveDailyTasks(template.getId(), template);
				DailyTaskManager.getInstance().updateTaskCount(player, 1, "weekly");
			}
			else if (template.getSort().equalsIgnoreCase("month"))
			{
				player.addActiveDailyTasks(template.getId(), template);
				DailyTaskManager.getInstance().updateTaskCount(player, 1, "month");
			}
		}
		catch (final Exception e)
		{
			warn("Could not insert tasks count: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void restoreDailyTasks(final Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(RESTORE_DAILY);
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var task = DailyTaskManager.getInstance().getDailyTask(rset.getInt("taskId"));
				final var playerTask = new PlayerTaskTemplate(task.getId(), task.getType(), task.getSort());
				switch (task.getType())
				{
					case "Farm" :
						playerTask.setCurrentNpcCount(rset.getInt("params"));
						break;
					case "Pvp" :
						playerTask.setCurrentPvpCount(rset.getInt("params"));
						break;
					case "Pk" :
						playerTask.setCurrentPkCount(rset.getInt("params"));
						break;
					case "Olympiad" :
						playerTask.setCurrentOlyMatchCount(rset.getInt("params"));
						break;
				}
				
				if (rset.getInt("status") == 1)
				{
					playerTask.setIsComplete(true);
				}
				
				if (rset.getInt("rewarded") == 1)
				{
					playerTask.setIsRewarded(true);
				}
				player.addActiveDailyTasks(playerTask.getId(), playerTask);
			}
		}
		catch (final Exception e)
		{
			warn("Failed restore daily tasks.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void updateTaskStatus(Player player, final PlayerTaskTemplate template)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_DAILY);
			statement.setInt(1, template.isComplete() ? 1 : 0);
			statement.setInt(2, player.getObjectId());
			statement.setInt(3, template.getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update daily task.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateTaskRewardStatus(Player player, final PlayerTaskTemplate template)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_DAILY_REWARD);
			statement.setInt(1, template.isRewarded() ? 1 : 0);
			statement.setInt(2, player.getObjectId());
			statement.setInt(3, template.getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update daily task.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateTaskParams(Player player, final int taskId, final int params)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_DAILY_PARAM);
			statement.setInt(1, params);
			statement.setInt(2, player.getObjectId());
			statement.setInt(3, taskId);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update daily task.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void removeTask(Player player, final int taskId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(REMOVE_DAILY);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, taskId);
			statement.execute();
			player.removeActiveDailyTasks(taskId);
		}
		catch (final Exception e)
		{
			warn("Failed remove daily task.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void addDailyTasksCount(String checkHwid, int[] tasks)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_TASK_COUNT);
			statement.setString(1, checkHwid);
			statement.setInt(2, tasks[0]);
			statement.setInt(3, tasks[1]);
			statement.setInt(4, tasks[2]);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("Could not insert daily tasks count: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateDailyTasksCount(final String checkHwid, int[] tasks)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_TASK_COUNT);
			statement.setInt(1, tasks[0]);
			statement.setInt(2, tasks[1]);
			statement.setInt(3, tasks[2]);
			statement.setString(4, checkHwid);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("Failed update tasks count.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static DailyTasksDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DailyTasksDAO _instance = new DailyTasksDAO();
	}
}