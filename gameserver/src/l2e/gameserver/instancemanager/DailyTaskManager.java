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
package l2e.gameserver.instancemanager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.dao.DailyTasksDAO;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.daily.DailyTaskTemplate;
import l2e.gameserver.model.actor.templates.player.PlayerTaskTemplate;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;

/**
 * Created by LordWinter
 */
public class DailyTaskManager extends LoggerObject
{
	private final List<DailyTaskTemplate> _tasks = new ArrayList<>();
	private boolean _hwidCheck;
	private String _columnCheck;
	private int _taskPerDay;
	private int _taskPerWeek;
	private int _taskPerMonth;
	private final int[] _taskPrice = new int[2];
	
	private ScheduledFuture<?> _weeklyTask = null;
	private ScheduledFuture<?> _monthTask = null;
	
	private final Map<String, int[]> _tasksList = new HashMap<>();
	
	public DailyTaskManager()
	{
		if (Config.ALLOW_DAILY_TASKS)
		{
			_tasks.clear();
			_tasksList.clear();
			getTaskLoad();
			checkDailyTimeTask(false);
			checkWeeklyTimeTask();
			checkMonthTimeTask();
			_columnCheck = isHwidCheck() ? "hwid" : "ip";
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT * FROM daily_tasks_count");
				rset = statement.executeQuery();
				while (rset.next())
				{
					final var column = rset.getString(_columnCheck);
					final var tasks = new int[]
					{
					        rset.getInt("dailyCount"), rset.getInt("weeklyCount"), rset.getInt("monthCount")
					};
					
					if (column != null && !column.isEmpty())
					{
						_tasksList.put(column, tasks);
					}
				}
			}
			catch (final Exception e)
			{
				warn("Failed restore daily tasks count.", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
	}
	
	public int[] getTaskCount(Player player)
	{
		final var checkHwid = isHwidCheck() ? player.getHWID() : player.getIPAddress();
		if (!_tasksList.containsKey(checkHwid))
		{
			final var tasks = new int[]
			{
			        getTaskPerDay(), getTaskPerWeek(), getTaskPerMonth()
			};
			_tasksList.put(checkHwid, tasks);
			DailyTasksDAO.getInstance().addDailyTasksCount(checkHwid, tasks);
		}
		return _tasksList.get(checkHwid);
	}
	
	public void updateTaskCount(Player player, int count, String type)
	{
		final var checkHwid = isHwidCheck() ? player.getHWID() : player.getIPAddress();
		if (_tasksList.containsKey(checkHwid))
		{
			final var counts = _tasksList.get(checkHwid);
			switch (type)
			{
				case "daily" :
					counts[0] = counts[0] - count;
					break;
				case "weekly" :
					counts[1] = counts[1] - count;
					break;
				case "month" :
					counts[2] = counts[2] - count;
					break;
			}
			_tasksList.put(checkHwid, counts);
			DailyTasksDAO.getInstance().updateDailyTasksCount(checkHwid, counts);
		}
	}
	
	private void getTaskLoad()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/services/dailyTasks.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);

			int counter = 0;
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					_hwidCheck = Boolean.parseBoolean(n1.getAttributes().getNamedItem("hwidCheck").getNodeValue());
					_taskPerDay = Integer.parseInt(n1.getAttributes().getNamedItem("taskPerDay").getNodeValue());
					_taskPerWeek = Integer.parseInt(n1.getAttributes().getNamedItem("taskPerWeek").getNodeValue());
					_taskPerMonth = Integer.parseInt(n1.getAttributes().getNamedItem("taskPerMonth").getNodeValue());
					_taskPrice[0] = n1.getAttributes().getNamedItem("priceId") != null ? Integer.parseInt(n1.getAttributes().getNamedItem("priceId").getNodeValue()) : 0;
					_taskPrice[1] = n1.getAttributes().getNamedItem("amount") != null ? Integer.parseInt(n1.getAttributes().getNamedItem("amount").getNodeValue()) : 0;
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("task".equalsIgnoreCase(d1.getNodeName()))
						{
							counter++;
							DailyTaskTemplate template = null;
							final int id = Integer.parseInt(d1.getAttributes().getNamedItem("id").getNodeValue());
							final String type = d1.getAttributes().getNamedItem("type").getNodeValue();
							final String sort = d1.getAttributes().getNamedItem("sort").getNodeValue();
							final String name = d1.getAttributes().getNamedItem("name").getNodeValue();
							final boolean isForAll = d1.getAttributes().getNamedItem("isForAll") != null ? Boolean.parseBoolean(d1.getAttributes().getNamedItem("isForAll").getNodeValue()) : true;
							final String image = d1.getAttributes().getNamedItem("image") != null ? d1.getAttributes().getNamedItem("image").getNodeValue() : "";
							String descr = "";
							final Map<Integer, Long> rewards = new HashMap<>();
							for (Node s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("description".equalsIgnoreCase(s1.getNodeName()))
								{
									descr = s1.getAttributes().getNamedItem("val").getNodeValue();
									if (descr.isEmpty())
									{
										warn("Error! Must be a description for task id: " + id);
										continue;
									}
									template = new DailyTaskTemplate(id, type, sort, name, descr, image, isForAll);
								}
								else if ("reward".equalsIgnoreCase(s1.getNodeName()))
								{
									for (Node r1 = s1.getFirstChild(); r1 != null; r1 = r1.getNextSibling())
									{
										if ("item".equalsIgnoreCase(r1.getNodeName()))
										{
											final int itemId = Integer.parseInt(r1.getAttributes().getNamedItem("id").getNodeValue());
											final long amount = Long.parseLong(r1.getAttributes().getNamedItem("count").getNodeValue());
											rewards.put(itemId, amount);
										}
									}
									template.setRewards(rewards);
								}
								else if ("npc".equalsIgnoreCase(s1.getNodeName()))
								{
									template.setNpcId(Integer.parseInt(s1.getAttributes().getNamedItem("id").getNodeValue()));
									template.setNpcCount(Integer.parseInt(s1.getAttributes().getNamedItem("count").getNodeValue()));
								}
								else if ("quest".equalsIgnoreCase(s1.getNodeName()))
								{
									template.setQuestId(Integer.parseInt(s1.getAttributes().getNamedItem("id").getNodeValue()));
								}
								else if ("reflection".equalsIgnoreCase(s1.getNodeName()))
								{
									template.setReflectionId(Integer.parseInt(s1.getAttributes().getNamedItem("id").getNodeValue()));
								}
								else if ("pvp".equalsIgnoreCase(s1.getNodeName()))
								{
									template.setPvpCount(Integer.parseInt(s1.getAttributes().getNamedItem("count").getNodeValue()));
								}
								else if ("pk".equalsIgnoreCase(s1.getNodeName()))
								{
									template.setPkCount(Integer.parseInt(s1.getAttributes().getNamedItem("count").getNodeValue()));
								}
								else if ("olympiad".equalsIgnoreCase(s1.getNodeName()))
								{
									template.setOlyMatchCount(Integer.parseInt(s1.getAttributes().getNamedItem("match").getNodeValue()));
								}
								else if ("event".equalsIgnoreCase(s1.getNodeName()))
								{
									template.setEventsCount(Integer.parseInt(s1.getAttributes().getNamedItem("count").getNodeValue()));
								}
								else if ("siege".equalsIgnoreCase(s1.getNodeName()))
								{
									final boolean castle = s1.getAttributes().getNamedItem("castle") != null && Boolean.parseBoolean(s1.getAttributes().getNamedItem("castle").getNodeValue());
									final boolean fortress = s1.getAttributes().getNamedItem("fortress") != null && Boolean.parseBoolean(s1.getAttributes().getNamedItem("fortress").getNodeValue());
									final boolean isAttack = s1.getAttributes().getNamedItem("isAttack") != null ? Boolean.parseBoolean(s1.getAttributes().getNamedItem("isAttack").getNodeValue()) : true;
									if (castle)
									{
										 template.setSiegeCastle(Boolean.parseBoolean(s1.getAttributes().getNamedItem("castle").getNodeValue()));
										 template.isAttack(isAttack);
									}
									else if (fortress)
									{
										template.setSiegeFort(Boolean.parseBoolean(s1.getAttributes().getNamedItem("fortress").getNodeValue()));
										template.isAttack(isAttack);
									}
								}
							}
							
							if (checkTask(template))
							{
								_tasks.add(template);
							}
						}
					}
				}
			}
			info("Loaded " + counter + " daily tasks.");
		}
		catch (NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("dailyTasks.xml could not be initialized.", e);
		}
		catch (IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	private boolean checkTask(DailyTaskTemplate template)
	{
		if (template == null || template.getType().isEmpty())
		{
			warn("Error! Daily Task Template null!");
			return false;
		}
		
		switch (template.getType())
		{
			case "Farm":
				if (template.getNpcId() <= 0 || template.getNpcCount() <= 0)
				{
					warn("Error! npcId or npcCount template not correct for daily task id:" + template.getId());
					return false;
				}
				break;
			case "Quest":
				if (template.getQuestId() <= 0)
				{
					warn("Error! questId template not correct for daily task id:" + template.getId());
					return false;
				}
				break;
			case "Reflection":
				if (template.getReflectionId() <= 0)
				{
					warn(" Error! reflection id template not correct for daily task id:" + template.getId());
					return false;
				}
				break;
			case "Pvp":
				if (template.getPvpCount() <= 0)
				{
					warn("Error! pvp count template not correct for daily task id:" + template.getId());
					return false;
				}
				break;
			case "Pk":
				if (template.getPkCount() <= 0)
				{
					warn("Error! pk count template not correct for daily task id:" + template.getId());
					return false;
				}
				break;
			case "Olympiad":
				if (template.getOlyMatchCount() <= 0)
				{
					warn("Error! olimpiad match template not correct for daily task id:" + template.getId());
					return false;
				}
				break;
			case "Event":
				if (template.getEventsCount() <= 0)
				{
					warn("Error! events template not correct for daily task id:" + template.getId());
					return false;
				}
				break;
			case "Siege":
				if ((!template.getSiegeFort() && !template.getSiegeCastle()) || (template.getSiegeFort() && template.getSiegeCastle()))
				{
					warn("Error! siegge template not correct for daily task id:" + template.getId());
					return false;
				}
				break;
		}
		return true;
	}
	
	public DailyTaskTemplate getDailyTask(int id)
	{
		for(final DailyTaskTemplate task : _tasks)
		{
			if (task.getId() == id)
			{
				return task;
			}
		}
		return null;
	}
	
	public List<DailyTaskTemplate> getDailyTasks()
	{
		return _tasks;
	}
	
	public List<DailyTaskTemplate> getTypeTasks(Player player, String type)
	{
		final List<DailyTaskTemplate> list = new ArrayList<>();
		for (final var task : _tasks)
		{
			if (task != null && task.getSort().equalsIgnoreCase(type))
			{
				var isFound = false;
				for (final PlayerTaskTemplate plTask : player.getActiveDailyTasks())
				{
					if (plTask.getId() == task.getId())
					{
						isFound = true;
						break;
					}
				}
				
				if (isFound)
				{
					continue;
				}
				
				if (task.getType().equalsIgnoreCase("Olympiad"))
				{
					if (!player.isNoble() || Olympiad.getInstance().inCompPeriod())
					{
						continue;
					}
				}
				
				if (task.getType().equalsIgnoreCase("Quest"))
				{
					final Quest quest = QuestManager.getInstance().getQuest(task.getQuestId());
					if (quest != null)
					{
						final QuestState qs = player.getQuestState(quest.getName());
						if ((qs != null) && qs.isCompleted())
						{
							continue;
						}
					}
				}
				
				if (task.getType().equalsIgnoreCase("Siege"))
				{
					if (player.getClan() == null)
					{
						continue;
					}
				}
				list.add(task);
			}
		}
		return list;
	}
	
	public int size()
	{
		return _tasks.size();
	}
	
	public void addAllValidTasks(Player player)
	{
		if (player.getActiveTasks("daily") < getTaskPerDay() && getTaskCount(player)[0] > 0)
		{
			addDailyTask(player);
		}
		
		if (player.getActiveTasks("weekly") < getTaskPerWeek() && getTaskCount(player)[1] > 0)
		{
			addWeeklyTask(player);
		}
		
		if (player.getActiveTasks("month") < getTaskPerMonth() && getTaskCount(player)[2] > 0)
		{
			addMonthTask(player);
		}
	}
	
	private void addDailyTask(Player player)
	{
		final var rndTask = rndTaskSelect(player, "daily");
		boolean added = false;
		if (rndTask != null)
		{
			final var playerTask = new PlayerTaskTemplate(rndTask.getId(), rndTask.getType(), rndTask.getSort());
			player.addDailyTask(playerTask);
			added = true;
		}
		
		if (added)
		{
			if (player.getActiveTasks("daily") < getTaskPerDay() && getTaskCount(player)[0] > 0)
			{
				addDailyTask(player);
			}
		}
	}
	
	private void addWeeklyTask(Player player)
	{
		final var rndTask = rndTaskSelect(player, "weekly");
		boolean added = false;
		if (rndTask != null)
		{
			final var playerTask = new PlayerTaskTemplate(rndTask.getId(), rndTask.getType(), rndTask.getSort());
			player.addDailyTask(playerTask);
			added = true;
		}
		
		if (added)
		{
			if (player.getActiveTasks("weekly") < getTaskPerWeek() && getTaskCount(player)[1] > 0)
			{
				addDailyTask(player);
			}
		}
	}
	
	private void addMonthTask(Player player)
	{
		final var rndTask = rndTaskSelect(player, "month");
		boolean added = false;
		if (rndTask != null)
		{
			final var playerTask = new PlayerTaskTemplate(rndTask.getId(), rndTask.getType(), rndTask.getSort());
			player.addDailyTask(playerTask);
			added = true;
		}
		
		if (added)
		{
			if (player.getActiveTasks("month") < getTaskPerMonth() && getTaskCount(player)[2] > 0)
			{
				addDailyTask(player);
			}
		}
	}
	
	private DailyTaskTemplate rndTaskSelect(final Player player, String sort)
	{
		final var list = getTypeTasks(player, sort);
		if (list.isEmpty())
		{
			return null;
		}
		return list.get(Rnd.get(list.size()));
	}
	
	public boolean checkHWID(Player attacker, Player defender)
	{
		if (isHwidCheck())
		{
			if (defender.getHWID() != null && !defender.getHWID().equalsIgnoreCase(attacker.getHWID()))
			{
				return true;
			}
		}
		else
		{
			if (defender.getIPAddress() != null && !defender.getIPAddress().equalsIgnoreCase(attacker.getIPAddress()))
			{
				return true;
			}
		}
		return false;
	}
	
	public void checkDailyTimeTask(boolean isClear)
	{
		if (!Config.ALLOW_DAILY_TASKS)
		{
			return;
		}
		
		if (isClear)
		{
			ServerVariables.set("Daily_Tasks", 0);
		}
		
		final Calendar currentTime = Calendar.getInstance();
		
		final long lastUpdate = ServerVariables.getLong("Daily_Tasks", 0);
		if (currentTime.getTimeInMillis() > lastUpdate)
		{
			final Calendar newTime = Calendar.getInstance();
			newTime.setLenient(true);
			newTime.set(Calendar.HOUR_OF_DAY, 6);
			newTime.set(Calendar.MINUTE, 30);
			if (newTime.getTimeInMillis() < currentTime.getTimeInMillis())
			{
				newTime.add(Calendar.DAY_OF_MONTH, 1);
			}
			ServerVariables.set("Daily_Tasks", newTime.getTimeInMillis());
			
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM daily_tasks WHERE type = ?");
				statement.setString(1, "daily");
				statement.execute();
				statement.close();
				
				statement = con.prepareStatement("UPDATE daily_tasks_count SET dailyCount = ?");
				statement.setInt(1, getTaskPerDay());
				statement.execute();
			}
			catch (final Exception e)
			{
				warn("Failed to clean up daily tasks.", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
			
			for (final var entry : _tasksList.entrySet())
			{
				final var tasks = entry.getValue();
				tasks[0] = getTaskPerDay();
				_tasksList.put(entry.getKey(), tasks);
			}
    				
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (player != null)
				{
					player.cleanDailyTasks();
				}
			}
			
			info("Daily tasks reshresh completed.");
			info("Next daily tasks refresh throught: " + Util.formatTime((int) (newTime.getTimeInMillis() - System.currentTimeMillis()) / 1000));
		}
	}
	
	public void checkWeeklyTimeTask()
	{
		final long lastUpdate = ServerVariables.getLong("Weekly_Tasks", 0);
		if (System.currentTimeMillis() > lastUpdate)
		{
			clearWeeklyTasks();
		}
		else
		{
			_weeklyTask = ThreadPoolManager.getInstance().schedule(() -> clearWeeklyTasks(), (lastUpdate - System.currentTimeMillis()));
		}
	}
	
	public void checkMonthTimeTask()
	{
		final long lastUpdate = ServerVariables.getLong("Month_Tasks", 0);
		if (System.currentTimeMillis() > lastUpdate)
		{
			clearMonthTasks();
		}
		else
		{
			_monthTask = ThreadPoolManager.getInstance().schedule(() -> clearMonthTasks(), (lastUpdate - System.currentTimeMillis()));
		}
	}
	
	private void clearMonthTasks()
	{
		final Calendar curTime = Calendar.getInstance();
		final int month = curTime.get(Calendar.MONTH) + 1;
		
		final Calendar newTime = Calendar.getInstance();
		newTime.setLenient(true);
		newTime.set(Calendar.MONTH, month);
		newTime.set(Calendar.DAY_OF_MONTH, 1);
		newTime.set(Calendar.HOUR_OF_DAY, 6);
		newTime.set(Calendar.MINUTE, 32);
		ServerVariables.set("Month_Tasks", newTime.getTimeInMillis());
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM daily_tasks WHERE type = ?");
			statement.setString(1, "month");
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("UPDATE daily_tasks_count SET monthCount = ?");
			statement.setInt(1, getTaskPerMonth());
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed to clean up month tasks.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		for (final var entry : _tasksList.entrySet())
		{
			final var tasks = entry.getValue();
			tasks[2] = getTaskPerDay();
			_tasksList.put(entry.getKey(), tasks);
		}
		
		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				player.cleanMonthTasks();
			}
		}
		
		if (_monthTask != null)
		{
			_monthTask.cancel(false);
			_monthTask = null;
		}
		_monthTask = ThreadPoolManager.getInstance().schedule(() -> clearMonthTasks(), (newTime.getTimeInMillis() - System.currentTimeMillis()));
		
		info("Month tasks reshresh completed.");
		info("Next month tasks refresh throught: " + Util.formatTime((int) (newTime.getTimeInMillis() - System.currentTimeMillis()) / 1000));
	}
	
	private void clearWeeklyTasks()
	{
		final Calendar newTime = Calendar.getInstance();
		newTime.setLenient(true);
		newTime.add(Calendar.HOUR, 168);
		newTime.set(Calendar.DAY_OF_WEEK, 2);
		newTime.set(Calendar.HOUR_OF_DAY, 6);
		newTime.set(Calendar.MINUTE, 31);
		ServerVariables.set("Weekly_Tasks", newTime.getTimeInMillis());
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM daily_tasks WHERE type = ?");
			statement.setString(1, "weekly");
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("UPDATE daily_tasks_count SET weeklyCount = ?");
			statement.setInt(1, getTaskPerWeek());
			statement.execute();
			
		}
		catch (final Exception e)
		{
			warn("Failed to clean up weekly tasks.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		for (final var entry : _tasksList.entrySet())
		{
			final var tasks = entry.getValue();
			tasks[1] = getTaskPerDay();
			_tasksList.put(entry.getKey(), tasks);
		}
		
		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				player.cleanWeeklyTasks();
			}
		}
		
		if (_weeklyTask != null)
		{
			_weeklyTask.cancel(false);
			_weeklyTask = null;
		}
		_weeklyTask = ThreadPoolManager.getInstance().schedule(() -> clearWeeklyTasks(), (newTime.getTimeInMillis() - System.currentTimeMillis()));
		info("Weekly tasks reshresh completed.");
		info("Next weekly tasks refresh throught: " + Util.formatTime((int) (newTime.getTimeInMillis() - System.currentTimeMillis()) / 1000));
	}
	
	public boolean isHwidCheck()
	{
		return _hwidCheck;
	}
	
	public String getColumnCheck()
	{
		return _columnCheck;
	}
	
	public int getTaskPerDay()
	{
		return _taskPerDay;
	}
	
	public int getTaskPerWeek()
	{
		return _taskPerWeek;
	}
	
	public int getTaskPerMonth()
	{
		return _taskPerMonth;
	}
	
	public int[] getTaskPrice()
	{
		return _taskPrice;
	}
	
	public static final DailyTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DailyTaskManager _instance = new DailyTaskManager();
	}
}