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
import java.util.Date;
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
import l2e.commons.time.cron.SchedulingPattern;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.daily.DailyRewardTemplate;

/**
 * Created by LordWinter
 */
public class DailyRewardManager extends LoggerObject
{
	private final List<DailyRewardTemplate> _rewards = new ArrayList<>();
	private final Map<String, Long> _rewardedList = new HashMap<>();
	private final Map<String, Integer> _rewardedDays = new HashMap<>();
	
	private DailyType _type;
	private int _lastDay;
	private String _dailyTime;
	private String _monthTime;
	private ScheduledFuture<?> _dailyTask = null;
	
	private enum DailyType
	{
		CHAR, ACCOUNT, IP, HWID
	}
	
	public DailyRewardManager()
	{
		if (Config.ALLOW_DAILY_REWARD)
		{
			_rewards.clear();
			_rewardedList.clear();
			_rewardedDays.clear();
			loadRewards();
			loadRewardedList();
			checkTimeTask();
		}
	}
	
	private void loadRewards()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/services/dailyRewards.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);

			int counter = 0;
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					_type = DailyType.valueOf(n1.getAttributes().getNamedItem("type").getNodeValue());
					_lastDay = n1.getAttributes().getNamedItem("lastDay") != null ? Integer.parseInt(n1.getAttributes().getNamedItem("lastDay").getNodeValue()) : 1;
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("refreshTime".equalsIgnoreCase(d1.getNodeName()))
						{
							_dailyTime = d1.getAttributes().getNamedItem("daily").getNodeValue();
							_monthTime = d1.getAttributes().getNamedItem("month").getNodeValue();
						}
						else if ("day".equalsIgnoreCase(d1.getNodeName()))
						{
							counter++;
							DailyRewardTemplate template = null;
							final Map<Integer, Integer> rewards = new HashMap<>();
							final int number = Integer.parseInt(d1.getAttributes().getNamedItem("number").getNodeValue());
							String image = d1.getAttributes().getNamedItem("image") != null ? d1.getAttributes().getNamedItem("image").getNodeValue() : "";
							for (Node s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("reward".equalsIgnoreCase(s1.getNodeName()))
								{
									final int itemId = Integer.parseInt(s1.getAttributes().getNamedItem("itemId").getNodeValue());
									final int count = Integer.parseInt(s1.getAttributes().getNamedItem("count").getNodeValue());
									rewards.put(itemId, count);
									
									final boolean isDisplayId = s1.getAttributes().getNamedItem("displayId") != null && Boolean.parseBoolean(s1.getAttributes().getNamedItem("displayId").getNodeValue());
									if (isDisplayId && image.isEmpty())
									{
										image = ItemsParser.getInstance().getTemplate(itemId).getIcon();
									}
								}
							}
							template = new DailyRewardTemplate(number, rewards);
							if (!image.isEmpty())
							{
								template.setDisplayImage(image);
							}
							_rewards.add(template);
						}
					}
				}
			}
			info("Loaded " + counter + " daily rewards.");
		}
		catch (NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("dailyRewards.xml could not be initialized.", e);
		}
		catch (IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	public DailyRewardTemplate getDailyReward(int day)
	{
		for(final DailyRewardTemplate dayReward : _rewards)
		{
			if (dayReward.getDay() == day)
			{
				return dayReward;
			}
		}
		return null;
	}
	
	public List<DailyRewardTemplate> getDailyRewards()
	{
		return _rewards;
	}
	
	public int size()
	{
		return _rewards.size();
	}
	
	private void checkTimeTask()
	{
		final long lastUpdate = ServerVariables.getLong("Daily_Rewards", 0);
		if (System.currentTimeMillis() > lastUpdate)
		{
			cleanDailyRewards();
		}
		else
		{
			_dailyTask = ThreadPoolManager.getInstance().schedule(() -> cleanDailyRewards(), (lastUpdate - System.currentTimeMillis()));
			info("Next tasks refresh at: " + new Date(lastUpdate));
		}
	}
	
	private void loadRewardedList()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM daily_rewards");
			rset = statement.executeQuery();
			while (rset.next())
			{
				_rewardedList.put(rset.getString("charInfo"), rset.getLong("value"));
				_rewardedDays.put(rset.getString("charInfo"), rset.getInt("last_day"));
			}
			info("Loaded " + _rewardedList.size() + " rewarded players.");
		}
		catch (final Exception e)
		{
			warn(getClass().getSimpleName() + ": " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public boolean isRewardedToday(Player player)
	{
		final String charInfo = getCharInfo(player);
		if (charInfo == null)
		{
			return true;
		}
		
		if (_rewardedList.containsKey(charInfo))
		{
			return _rewardedList.get(charInfo) > System.currentTimeMillis();
		}
		return false;
	}
	
	public boolean isHaveCharInfo(String charInfo)
	{
		return _rewardedList.get(charInfo) != null;
	}
	
	public long getRewardTime(Player player)
	{
		final String charInfo = getCharInfo(player);
		if (charInfo == null)
		{
			return -1;
		}
		
		if (_rewardedList.containsKey(charInfo))
		{
			return _rewardedList.get(charInfo);
		}
		return -1;
	}
	
	public void setRewarded(Player player)
	{
		updateRewardTime(player);
		updateRewardDay(player);
	}
	
	private void updateRewardTime(Player player)
	{
		final String charInfo = getCharInfo(player);
		if (charInfo == null)
		{
			return;
		}
		
		final long time = new SchedulingPattern(_dailyTime).next(System.currentTimeMillis());
		if (_rewardedList.get(charInfo) != null)
		{
			updateDailyRewardTime(charInfo, time);
		}
		_rewardedList.put(charInfo, time);
	}
	
	private void updateDailyRewardTime(String charInfo, final long time)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE daily_rewards SET value=? WHERE charInfo=?");
			statement.setLong(1, time);
			statement.setString(2, charInfo);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update daily rewards time.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public int getRewardDay(Player player)
	{
		final String charInfo = getCharInfo(player);
		if (charInfo == null)
		{
			return 0;
		}
		
		if (_rewardedDays.containsKey(charInfo))
		{
			return _rewardedDays.get(charInfo);
		}
		return 0;
	}
	
	private void updateRewardDay(Player player)
	{
		final String charInfo = getCharInfo(player);
		if (charInfo == null)
		{
			return;
		}
		
		if (_rewardedDays.containsKey(charInfo))
		{
			final int nextDay = _rewardedDays.get(charInfo) + 1;
			updateDailyRewardDay(charInfo, nextDay);
			_rewardedDays.put(charInfo, nextDay);
		}
	}
	
	public void updateDailyRewardDay(String charInfo, int day)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE daily_rewards SET last_day=? WHERE charInfo=?");
			statement.setInt(1, day);
			statement.setString(2, charInfo);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update daily rewards day.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void addNewDailyPlayer(String charInfo)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO daily_rewards (charInfo, value, last_day) VALUES (?,?,?)");
			statement.setString(1, charInfo);
			statement.setLong(2, 0);
			statement.setInt(3, _lastDay);
			statement.executeUpdate();

			_rewardedList.put(charInfo, 0L);
			_rewardedDays.put(charInfo, _lastDay);
		}
		catch (final Exception e)
		{
			warn("Could not insert daily rewards: " + e.getMessage(), e);
			return;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void cleanDailyRewards()
	{
		for (final String charInfo : _rewardedDays.keySet())
		{
			_rewardedDays.put(charInfo, 1);
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE daily_rewards SET last_day=?");
			statement.setInt(1, 1);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update daily rewards last_day.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		final long newTime = new SchedulingPattern(_monthTime).next(System.currentTimeMillis());
		ServerVariables.set("Daily_Rewards", newTime);
		if (_dailyTask != null)
		{
			_dailyTask.cancel(false);
			_dailyTask = null;
		}
		_dailyTask = ThreadPoolManager.getInstance().schedule(() -> cleanDailyRewards(), (newTime - System.currentTimeMillis()));
		
		info("Refresh rewards completed!");
		info("Next tasks refresh at: " + new Date(newTime));
	}
	
	public String getCharInfo(Player player)
	{
		switch (getType())
		{
			case CHAR :
				return String.valueOf(player.getObjectId());
			case ACCOUNT :
				return player.getAccountName();
			case IP :
				return player.getIPAddress();
			case HWID :
				return player.getHWID();
		}
		return null;
	}
	
	public DailyType getType()
	{
		return _type;
	}
	
	public static final DailyRewardManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DailyRewardManager _instance = new DailyRewardManager();
	}
}