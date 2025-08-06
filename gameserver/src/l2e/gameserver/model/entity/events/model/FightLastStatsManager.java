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
package l2e.gameserver.model.entity.events.model;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.data.parser.FightEventParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventLastPlayerStats;

/**
 * Created by LordWinter
 */
public class FightLastStatsManager extends LoggerObject
{
	public static enum FightEventStatType
	{
		KILL_PLAYER("Kill Player");
		
		private final String _name;

		private FightEventStatType(String name)
		{
			_name = name;
		}

		public String getName()
		{
			return _name;
		}
	}

	private final Map<Integer, List<FightEventLastPlayerStats>> _allStats = new HashMap<>();
	
	public void updateStat(int eventId, Player player, FightEventStatType type, int score)
	{
		FightEventLastPlayerStats myStat = getMyStat(eventId, player);
		
		if (myStat == null)
		{
			myStat = new FightEventLastPlayerStats(player, type.getName(), score);
			_allStats.get(eventId).add(myStat);
		}
		else
		{
			myStat.setScore(score);
		}
	}

	public FightEventLastPlayerStats getMyStat(int id, Player player)
	{
		for (final int eventId : _allStats.keySet())
		{
			if (eventId == id)
			{
				for (final FightEventLastPlayerStats stat : _allStats.get(eventId))
				{
					if (stat.isMyStat(player))
					{
						return stat;
					}
				}
			}
		}
		return null;
	}
	
	public void updateEventStats(int id)
	{
		for (final int eventId : _allStats.keySet())
		{
			if (eventId == id)
			{
				for (final FightEventLastPlayerStats stat : _allStats.get(eventId))
				{
					addEventStats(eventId, stat);
				}
			}
		}
	}
	
	private void addEventStats(int eventId, FightEventLastPlayerStats stat)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO events_statistic (eventId, char_name, clan_name, ally_name, classId, scores) VALUES (?,?,?,?,?,?)");
			statement.setInt(1, eventId);
			statement.setString(2, stat.getPlayerName());
			statement.setString(3, stat.getClanName());
			statement.setString(4, stat.getAllyName());
			statement.setInt(5, stat.getClassId());
			statement.setInt(6, stat.getScore());
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("Could not insert new event stat: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	private void clearEventStats(int eventId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM events_statistic WHERE eventId=?");
			statement.setInt(1, eventId);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed to clean up event statistic.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public List<FightEventLastPlayerStats> getStats(int eventId, boolean sortByScore)
	{
		final List<FightEventLastPlayerStats> listToSort = new ArrayList<>();
		listToSort.addAll(_allStats.get(eventId));
		if (sortByScore)
		{
			final Comparator<FightEventLastPlayerStats> statsComparator = new SortRanking();
			Collections.sort(listToSort, statsComparator);
		}

		return listToSort;
	}

	public void clearStats(int eventId)
	{
		_allStats.get(eventId).clear();
		clearEventStats(eventId);
	}

	private static class SortRanking implements Comparator<FightEventLastPlayerStats>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;

		@Override
		public int compare(FightEventLastPlayerStats o1, FightEventLastPlayerStats o2)
		{
			return Integer.compare(o2.getScore(), o1.getScore());
		}
	}

	public void restoreStats()
	{
		for (final AbstractFightEvent event : FightEventParser.getInstance().getEvents().valueCollection())
		{
			if (event != null)
			{
				if (!isFoundStats(event.getId()))
				{
					final List<FightEventLastPlayerStats> list = new ArrayList<>();
					_allStats.put(event.getId(), list);
				}
			}
		}
		info("Clean up all event statistics.");
	}
	
	private boolean isFoundStats(int id)
	{
		final List<FightEventLastPlayerStats> list = new ArrayList<>();
		boolean found = false;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM events_statistic WHERE eventId=?");
			statement.setInt(1, id);
			rset = statement.executeQuery();
			while (rset.next())
			{
				found = true;
				final String charName = rset.getString("char_name");
				final String clan_name = rset.getString("clan_name");
				final String ally_name = rset.getString("ally_name");
				final int classId = rset.getInt("classId");
				final int scores = rset.getInt("scores");
				list.add(new FightEventLastPlayerStats(charName, clan_name, ally_name, classId, scores));
			}
		}
		catch (final Exception e)
		{
			warn("Failed restore event statistic.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (found)
		{
			_allStats.put(id, list);
		}
		return found;
	}

	private static class FightLastStatsManagerHolder
	{
		private static final FightLastStatsManager _instance = new FightLastStatsManager();
	}

	public static FightLastStatsManager getInstance()
	{
		return FightLastStatsManagerHolder._instance;
	}
}