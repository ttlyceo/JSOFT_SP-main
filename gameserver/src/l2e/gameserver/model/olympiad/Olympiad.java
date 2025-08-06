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
package l2e.gameserver.model.olympiad;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;
import l2e.commons.util.GameSettings;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.OlympiadTemplate;
import l2e.gameserver.model.entity.Hero;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Olympiad
{
	protected static final Logger _log = LoggerFactory.getLogger(Olympiad.class);
	
	private static final Map<Integer, StatsSet> NOBLES = new ConcurrentHashMap<>();
	private static final List<StatsSet> HEROS_TO_BE = new ArrayList<>();
	private static final Map<Integer, Integer> NOBLES_RANK = new HashMap<>();
	
	public static final String OLYMPIAD_HTML_PATH = "data/html/olympiad/";
	private static final String OLYMPIAD_LOAD_DATA = "SELECT current_cycle, period, comp_start, comp_end, training_start, training_end, olympiad_end, validation_end, " + "next_weekly_change FROM olympiad_data WHERE id = 0";
	private static final String OLYMPIAD_SAVE_DATA = "INSERT INTO olympiad_data (id, current_cycle, " + "period, comp_start, comp_end, training_start, training_end, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?,?,?,?,?) " + "ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, " + "validation_end=?, next_weekly_change=?";
	private static final String OLYMPIAD_UPDATE_COMP_DATA = "UPDATE olympiad_data SET comp_start=?, comp_end=?";
	private static final String OLYMPIAD_UPDATE_TRAINING_DATA = "UPDATE olympiad_data SET training_start=?, training_end=?";
	private static final String OLYMPIAD_LOAD_NOBLES = "SELECT olympiad_nobles.charId, olympiad_nobles.class_id, " + "characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.olympiad_points_past, olympiad_nobles.competitions_done, " + "olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn, " + "olympiad_nobles.competitions_done_week, olympiad_nobles.competitions_done_week_classed, olympiad_nobles.competitions_done_week_non_classed, olympiad_nobles.competitions_done_week_team " + "FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId";
	private static final String OLYMPIAD_SAVE_NOBLES = "INSERT INTO olympiad_nobles " + "(`charId`,`class_id`,`olympiad_points`,`olympiad_points_past`,`competitions_done`,`competitions_won`,`competitions_lost`," + "`competitions_drawn`, `competitions_done_week`, `competitions_done_week_classed`, `competitions_done_week_non_classed`, `competitions_done_week_team`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String OLYMPIAD_UPDATE_NOBLES = "UPDATE olympiad_nobles SET " + "class_id = ?, olympiad_points = ?, olympiad_points_past = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ?, competitions_done_week = ?, competitions_done_week_classed = ?, competitions_done_week_non_classed = ?, competitions_done_week_team = ? WHERE charId = ?";
	private static final String OLYMPIAD_GET_HEROS = "SELECT olympiad_nobles.charId, characters.char_name " + "FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId " + "AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " AND olympiad_nobles.competitions_won > 0 " + "ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC";
	private static final String GET_ALL_CLASSIFIED_NOBLESS = "SELECT charId from olympiad_nobles_eom " + "WHERE competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC";
	private static final String GET_EACH_CLASS_LEADER = "SELECT olympiad_nobles_eom.olympiad_points, olympiad_nobles_eom.competitions_won, olympiad_nobles_eom.competitions_lost, characters.char_name from olympiad_nobles_eom, characters " + "WHERE characters.charId = olympiad_nobles_eom.charId AND olympiad_nobles_eom.class_id = ? " + "AND olympiad_nobles_eom.competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " " + "ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_CURRENT = "SELECT olympiad_nobles.olympiad_points, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, characters.char_name from olympiad_nobles, characters " + "WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? " + "AND olympiad_nobles.competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " " + "ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_SOULHOUND = "SELECT olympiad_nobles_eom.olympiad_points, olympiad_nobles_eom.competitions_won, olympiad_nobles_eom.competitions_lost, characters.char_name from olympiad_nobles_eom, characters " + "WHERE characters.charId = olympiad_nobles_eom.charId AND (olympiad_nobles_eom.class_id = ? OR olympiad_nobles_eom.class_id = 133) " + "AND olympiad_nobles_eom.competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " " + "ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_CURRENT_SOULHOUND = "SELECT olympiad_nobles.olympiad_points, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, characters.char_name from olympiad_nobles, characters " + "WHERE characters.charId = olympiad_nobles.charId AND (olympiad_nobles.class_id = ? OR olympiad_nobles.class_id = 133) " + "AND olympiad_nobles.competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " " + "ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10";
	
	private static final String OLYMPIAD_CALCULATE_POINTS = "UPDATE `olympiad_nobles` SET `olympiad_points_past` = `olympiad_points` WHERE `competitions_done` >= ?";
	private static final String OLYMPIAD_CLEANUP_NOBLES = "UPDATE `olympiad_nobles` SET `olympiad_points` = ?, `competitions_done` = 0, `competitions_won` = 0, `competitions_lost` = 0, `competitions_drawn` = 0, `competitions_done_week` = 0, `competitions_done_week_classed` = 0, `competitions_done_week_non_classed` = 0, `competitions_done_week_team` = 0";
	private static final String OLYMPIAD_MONTH_CLEAR = "TRUNCATE olympiad_nobles_eom";
	private static final String OLYMPIAD_MONTH_CREATE = "INSERT INTO olympiad_nobles_eom SELECT charId, class_id, olympiad_points, competitions_done, competitions_won, competitions_lost, competitions_drawn FROM olympiad_nobles";
	private static final int[] HERO_IDS =
	{
	        88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 131, 132, 133, 134
	};
	
	protected static final int DEFAULT_POINTS = Config.ALT_OLY_START_POINTS;
	protected static final int WEEKLY_POINTS = Config.ALT_OLY_WEEKLY_POINTS;
	protected static final int DAILY_POINTS = Config.ALT_OLY_DAILY_POINTS;
	
	public static final String CHAR_ID = "charId";
	public static final String CLASS_ID = "class_id";
	public static final String CHAR_NAME = "char_name";
	public static final String POINTS = "olympiad_points";
	public static final String POINTS_PAST = "olympiad_points_past";
	public static final String COMP_DONE = "competitions_done";
	public static final String COMP_WON = "competitions_won";
	public static final String COMP_LOST = "competitions_lost";
	public static final String COMP_DRAWN = "competitions_drawn";
	public static final String COMP_DONE_WEEK = "competitions_done_week";
	public static final String COMP_DONE_WEEK_CLASSED = "competitions_done_week_classed";
	public static final String COMP_DONE_WEEK_NON_CLASSED = "competitions_done_week_non_classed";
	public static final String COMP_DONE_WEEK_TEAM = "competitions_done_week_team";
	
	protected long _olympiadEnd;
	protected long _validationEnd;
	
	protected int _period;
	protected long _nextWeeklyChange;
	protected int _currentCycle;
	private long _compEnd;
	private long _compStart;
	private long _trainingEnd;
	private long _trainingStart;
	protected static boolean _inTrainingPeriod;
	protected static boolean _inCompPeriod;
	protected static boolean _compStarted = false;
	protected ScheduledFuture<?> _scheduledCompStart;
	protected ScheduledFuture<?> _scheduledCompEnd;
	protected ScheduledFuture<?> _scheduledTrainingStart;
	protected ScheduledFuture<?> _scheduledTrainingEnd;
	protected ScheduledFuture<?> _scheduledOlympiadEnd;
	protected ScheduledFuture<?> _scheduledWeeklyTask;
	protected ScheduledFuture<?> _scheduledValdationTask;
	protected ScheduledFuture<?> _gameManager = null;
	protected ScheduledFuture<?> _gameAnnouncer = null;
	
	public static Olympiad getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected Olympiad()
	{
		load();
		DoubleSessionManager.getInstance().registerEvent(DoubleSessionManager.OLYMPIAD_ID);
		
		if (_period == 0)
		{
			init();
		}
	}
	
	private void load()
	{
		NOBLES.clear();
		boolean loaded = false;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OLYMPIAD_LOAD_DATA);
			rset = statement.executeQuery();
			while (rset.next())
			{
				_currentCycle = rset.getInt("current_cycle");
				_period = rset.getInt("period");
				_olympiadEnd = rset.getLong("olympiad_end");
				_compStart = rset.getLong("comp_start");
				_compEnd = rset.getLong("comp_end");
				_trainingStart = rset.getLong("training_start");
				_trainingEnd = rset.getLong("training_end");
				_validationEnd = rset.getLong("validation_end");
				_nextWeeklyChange = rset.getLong("next_weekly_change");
				loaded = true;
			}
		}
		catch (final Exception e)
		{
			_log.warn("Olympiad: Error loading olympiad data from database: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(statement, rset);
		}
		
		if (!loaded)
		{
			_log.info("Olympiad: failed to load data from database, trying to load from file.");
			
			final GameSettings OlympiadProperties = new GameSettings();
			try (
			    InputStream is = new FileInputStream(Config.OLYMPIAD_CONFIG_FILE))
			{
				
				OlympiadProperties.load(is);
			}
			catch (final Exception e)
			{
				_log.warn("Olympiad: Error loading olympiad.ini: ", e);
				return;
			}
			
			_currentCycle = Integer.parseInt(OlympiadProperties.getProperty("CurrentCycle", "1"));
			_period = Integer.parseInt(OlympiadProperties.getProperty("Period", "0"));
			_olympiadEnd = Long.parseLong(OlympiadProperties.getProperty("OlympiadEnd", "0"));
			_validationEnd = Long.parseLong(OlympiadProperties.getProperty("ValidationEnd", "0"));
			_nextWeeklyChange = Long.parseLong(OlympiadProperties.getProperty("NextWeeklyChange", "0"));
		}
		
		boolean calcHeroPoints = false;
		
		switch (_period)
		{
			case 0 :
				if ((_olympiadEnd == 0) || (_olympiadEnd < System.currentTimeMillis()))
				{
					setNewOlympiadEnd();
				}
				else
				{
					scheduleWeeklyChange();
				}
				break;
			case 1 :
				if (_validationEnd > System.currentTimeMillis())
				{
					_scheduledValdationTask = ThreadPoolManager.getInstance().schedule(new ValidationEndTask(), getMillisToValidationEnd());
				}
				else
				{
					sortHerosToBe(false);
					Hero.getInstance().computeNewHeroes(HEROS_TO_BE);
					_currentCycle++;
					_period = 0;
					cleanupNobles();
					setNewOlympiadEnd();
					calcHeroPoints = true;
				}
				break;
			default :
				_log.warn("Olympiad: Omg something went wrong in loading!! Period = " + _period);
				return;
		}
		
		try
		{
			statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
			rset = statement.executeQuery();
			
			StatsSet statData;
			while (rset.next())
			{
				statData = new StatsSet();
				statData.set(CLASS_ID, rset.getInt(CLASS_ID));
				statData.set(CHAR_NAME, rset.getString(CHAR_NAME));
				statData.set(POINTS, rset.getInt(POINTS));
				statData.set(POINTS_PAST, rset.getInt(POINTS_PAST));
				statData.set(COMP_DONE, rset.getInt(COMP_DONE));
				statData.set(COMP_WON, rset.getInt(COMP_WON));
				statData.set(COMP_LOST, rset.getInt(COMP_LOST));
				statData.set(COMP_DRAWN, rset.getInt(COMP_DRAWN));
				statData.set(COMP_DONE_WEEK, rset.getInt(COMP_DONE_WEEK));
				statData.set(COMP_DONE_WEEK_CLASSED, rset.getInt(COMP_DONE_WEEK_CLASSED));
				statData.set(COMP_DONE_WEEK_NON_CLASSED, rset.getInt(COMP_DONE_WEEK_NON_CLASSED));
				statData.set(COMP_DONE_WEEK_TEAM, rset.getInt(COMP_DONE_WEEK_TEAM));
				statData.set("to_save", false);
				
				addNobleStats(rset.getInt(CHAR_ID), statData);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Olympiad: Error loading noblesse data from database: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (calcHeroPoints && !HEROS_TO_BE.isEmpty())
		{
			for (final var hero : HEROS_TO_BE)
			{
				final int charId = hero.getInteger(CHAR_ID);
				final var nobleInfo = NOBLES.get(charId);
				final int points = nobleInfo.getInteger(POINTS_PAST);
				nobleInfo.set(POINTS_PAST, (points + Config.ALT_OLY_HERO_POINTS));
			}
		}
		
		synchronized (this)
		{
			if (_period == 0)
			{
				_log.info("Olympiad: Currently in Olympiad period.");
				_log.info("Olympiad: Olympiad period will end " + new Date(_olympiadEnd));
			}
			else
			{
				_log.info("Olympiad: Currently in Validation period.");
				_log.info("Olympiad: Validation period will end " + new Date(_validationEnd));
			}
			
			if (_period == 0)
			{
				_log.info("Olympiad: Next weekly battle and point datas " + new Date(_nextWeeklyChange));
			}
		}
		_log.info("Olympiad: Loaded " + NOBLES.size() + " nobleses.");
	}
	
	public void loadNoblesRank()
	{
		NOBLES_RANK.clear();
		final Map<Integer, Integer> tmpPlace = new HashMap<>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(GET_ALL_CLASSIFIED_NOBLESS);
			rset = statement.executeQuery();
			int place = 1;
			while (rset.next())
			{
				tmpPlace.put(rset.getInt(CHAR_ID), place++);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Olympiad: Error loading noblesse data from database for Ranking: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		int rank1 = (int) Math.round(tmpPlace.size() * 0.01);
		int rank2 = (int) Math.round(tmpPlace.size() * 0.10);
		int rank3 = (int) Math.round(tmpPlace.size() * 0.25);
		int rank4 = (int) Math.round(tmpPlace.size() * 0.50);
		if (rank1 == 0)
		{
			rank1 = 1;
			rank2++;
			rank3++;
			rank4++;
		}
		
		for (final Entry<Integer, Integer> chr : tmpPlace.entrySet())
		{
			if (chr.getValue() <= rank1)
			{
				NOBLES_RANK.put(chr.getKey(), 1);
			}
			else if (tmpPlace.get(chr.getKey()) <= rank2)
			{
				NOBLES_RANK.put(chr.getKey(), 2);
			}
			else if (tmpPlace.get(chr.getKey()) <= rank3)
			{
				NOBLES_RANK.put(chr.getKey(), 3);
			}
			else if (tmpPlace.get(chr.getKey()) <= rank4)
			{
				NOBLES_RANK.put(chr.getKey(), 4);
			}
			else
			{
				NOBLES_RANK.put(chr.getKey(), 5);
			}
		}
	}
	
	protected void init()
	{
		if (_period == 1)
		{
			return;
		}
		
		final var now = System.currentTimeMillis();
		if ((_compStart < now) && (_compEnd < now))
		{
			final SchedulingPattern timePattern = new SchedulingPattern(Config.ALT_OLY_START_TIME);
			_compStart = timePattern.next(System.currentTimeMillis());
			_compEnd = _compStart + (Config.ALT_OLY_CPERIOD * 3600000);
			updateCompDbStatus();
		}
		final var task = _scheduledOlympiadEnd;
		if (task != null)
		{
			task.cancel(true);
		}
		_scheduledOlympiadEnd = ThreadPoolManager.getInstance().schedule(new OlympiadEndTask(), getMillisToOlympiadEnd());
		
		updateCompStatus();
		loadNoblesRank();
	}
	
	private void updateCompDbStatus()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OLYMPIAD_UPDATE_COMP_DATA);
			statement.setLong(1, _compStart);
			statement.setLong(2, _compEnd);
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn("Error could not update comp status: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	private void updateTrainingDbStatus()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OLYMPIAD_UPDATE_TRAINING_DATA);
			statement.setLong(1, _trainingStart);
			statement.setLong(2, _trainingEnd);
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn("Error could not update comp status: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	protected class OlympiadEndTask implements Runnable
	{
		@Override
		public void run()
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED);
			sm.addNumber(_currentCycle);
			Announcements.getInstance().announceToAll(sm);
			
			final var task = _scheduledWeeklyTask;
			if (task != null)
			{
				task.cancel(true);
				_scheduledWeeklyTask = null;
			}
			
			saveNobleData();
			
			_period = 1;
			Hero.getInstance().clearHeroes();
			Hero.getInstance().resetData();
			
			_validationEnd = System.currentTimeMillis() + (Config.ALT_OLY_VPERIOD * 3600000);
			
			saveOlympiadStatus();
			updateMonthlyData();
			
			_scheduledValdationTask = ThreadPoolManager.getInstance().schedule(new ValidationEndTask(), getMillisToValidationEnd());
		}
	}
	
	protected class ValidationEndTask implements Runnable
	{
		@Override
		public void run()
		{
			sortHerosToBe(true);
			Hero.getInstance().computeNewHeroes(HEROS_TO_BE);
			_period = 0;
			_currentCycle++;
			cleanupNobles();
			setNewOlympiadEnd();
			init();
		}
	}
	
	protected static int getNobleCount()
	{
		return NOBLES.size();
	}
	
	public static StatsSet getNobleStats(int playerId)
	{
		return NOBLES.get(playerId);
	}
	
	private void updateCompStatus()
	{
		final var isTrainingEnable = Config.ALLOW_TRAINING_BATTLES;
		var isValidTrainingTime = true;
		synchronized (this)
		{
			getMillisToCompBegin();
			if (isTrainingEnable)
			{
				final var time = getMillisToTrainingBegin();
				if (time <= 0)
				{
					isValidTrainingTime = false;
				}
			}
			_log.info("Olympiad: Battles will begin: " + new Date(_compStart));
			if (isTrainingEnable && isValidTrainingTime)
			{
				_log.info("Olympiad: Training Battles will begin: " + new Date(_trainingStart));
			}
		}
		_scheduledCompStart = ThreadPoolManager.getInstance().schedule(new CompStartTask(), getMillisToCompBegin());
		if (isTrainingEnable)
		{
			if (!isValidTrainingTime)
			{
				_log.info("Olympiad: Training Battles have wrong time!");
				return;
			}
			_scheduledTrainingStart = ThreadPoolManager.getInstance().schedule(new TrainingStartTask(), getMillisToTrainingBegin());
		}
	}
	
	private class TrainingStartTask implements Runnable
	{
		@Override
		public void run()
		{
			if (isOlympiadEnd())
			{
				return;
			}
			_inCompPeriod = true;
			_inTrainingPeriod = true;
			Announcements.getInstance().announceToAll(new ServerMessage("Olympiad.TRAINING_START", true));
			_log.info("Olympiad: Olympiad training battles started.");
			_gameManager = ThreadPoolManager.getInstance().scheduleAtFixedRate(OlympiadGameManager.getInstance(), 30000, 30000);
			if (Config.ALT_OLY_ANNOUNCE_GAMES)
			{
				_gameAnnouncer = ThreadPoolManager.getInstance().scheduleAtFixedRate(new OlympiadAnnouncer(), 30000, 500);
			}
			_scheduledTrainingEnd = ThreadPoolManager.getInstance().schedule(new TrainingEndTask(1), (getMillisToTrainingEnd() - 600000));
		}
	}
	
	private class TrainingEndTask implements Runnable
	{
		private final int _status;
		
		public TrainingEndTask(int status)
		{
			_status = status;
		}
		
		@Override
		public void run()
		{
			if (isOlympiadEnd())
			{
				return;
			}
			
			switch (_status)
			{
				case 1 :
					Announcements.getInstance().announceToAll(new ServerMessage("Olympiad.TRAINING_REG_END", true));
					_scheduledTrainingEnd = ThreadPoolManager.getInstance().schedule(new TrainingEndTask(2), 60000);
					break;
				case 2 :
					_inCompPeriod = false;
					_inTrainingPeriod = false;
					if (OlympiadGameManager.getInstance().isBattleStarted())
					{
						_scheduledTrainingEnd = ThreadPoolManager.getInstance().schedule(new TrainingEndTask(2), 60000);
					}
					else
					{
						Announcements.getInstance().announceToAll(new ServerMessage("Olympiad.TRAINING_END", true));
						_log.info("Olympiad: Olympiad training battles ended.");
						var task = _gameManager;
						if (task != null)
						{
							task.cancel(false);
							_gameManager = null;
						}
						task = _gameAnnouncer;
						if (task != null)
						{
							task.cancel(false);
							_gameAnnouncer = null;
						}
					}
					break;
			}
		}
	}
	
	private class CompStartTask implements Runnable
	{
		@Override
		public void run()
		{
			if (isOlympiadEnd())
			{
				return;
			}
			_inCompPeriod = true;
			Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_STARTED));
			_log.info("Olympiad: Olympiad game started.");
			_gameManager = ThreadPoolManager.getInstance().scheduleAtFixedRate(OlympiadGameManager.getInstance(), 30000, 30000);
			if (Config.ALT_OLY_ANNOUNCE_GAMES)
			{
				_gameAnnouncer = ThreadPoolManager.getInstance().scheduleAtFixedRate(new OlympiadAnnouncer(), 30000, 500);
			}
			_scheduledCompEnd = ThreadPoolManager.getInstance().schedule(new CompEndTask(1), (getMillisToCompEnd() - 600000));
		}
	}
	
	private class CompEndTask implements Runnable
	{
		private final int _status;
		
		public CompEndTask(int status)
		{
			_status = status;
		}

		@Override
		public void run()
		{
			if (isOlympiadEnd())
			{
				return;
			}
			
			switch (_status)
			{
				case 1 :
					Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_REGISTRATION_PERIOD_ENDED));
					_scheduledCompEnd = ThreadPoolManager.getInstance().schedule(new CompEndTask(2), 60000);
					break;
				case 2 :
					_inCompPeriod = false;
					if (OlympiadGameManager.getInstance().isBattleStarted())
					{
						_scheduledCompEnd = ThreadPoolManager.getInstance().schedule(new CompEndTask(2), 60000);
					}
					else
					{
						Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_ENDED));
						_log.info("Olympiad: Olympiad game ended.");
						var task = _gameManager;
						if (task != null)
						{
							task.cancel(false);
							_gameManager = null;
						}
						task = _gameAnnouncer;
						if (task != null)
						{
							task.cancel(false);
							_gameAnnouncer = null;
						}
						saveOlympiadStatus();
						init();
					}
					break;
			}
		}
	}
	
	private long getMillisToOlympiadEnd()
	{
		return (_olympiadEnd - System.currentTimeMillis());
	}
	
	public long getOlympiadEndDate()
	{
		return _olympiadEnd;
	}
	
	public void manualSelectHeroes()
	{
		final var task = _scheduledOlympiadEnd;
		if (task != null)
		{
			task.cancel(true);
		}
		_scheduledOlympiadEnd = ThreadPoolManager.getInstance().schedule(new OlympiadEndTask(), 0);
	}
	
	public void manualStartNewOlympiad()
	{
		final var task = _scheduledValdationTask;
		if (task != null)
		{
			task.cancel(true);
		}
		_scheduledValdationTask = ThreadPoolManager.getInstance().schedule(new ValidationEndTask(), 0);
	}
	
	protected long getMillisToValidationEnd()
	{
		final var now = System.currentTimeMillis();
		if (_validationEnd > now)
		{
			return (_validationEnd - now);
		}
		return 10L;
	}
	
	public long getValidationEndDate()
	{
		return _validationEnd;
	}
	
	public boolean isOlympiadEnd()
	{
		return (_period != 0);
	}
	
	protected void setNewOlympiadEnd()
	{
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_STARTED);
		sm.addNumber(_currentCycle);
		
		Announcements.getInstance().announceToAll(sm);
		
		SchedulingPattern cronTime;
		try
		{
			cronTime = new SchedulingPattern(Config.OLYMPIAD_PERIOD);
		}
		catch (final InvalidPatternException e)
		{
			return;
		}
		_olympiadEnd = cronTime.next(System.currentTimeMillis());
		_nextWeeklyChange = getWeeklyChangeDate();
		scheduleWeeklyChange();
	}
	
	private long getWeeklyChangeDate()
	{
		SchedulingPattern cronTime;
		try
		{
			cronTime = new SchedulingPattern(Config.OLYMPIAD_WEEKLY_PERIOD);
		}
		catch (final InvalidPatternException e)
		{
			return 0;
		}
		return cronTime.next(System.currentTimeMillis());
	}
	
	public boolean inCompPeriod()
	{
		return _inCompPeriod;
	}
	
	public boolean isTrainingPeriod()
	{
		return _inTrainingPeriod;
	}
	
	private long getMillisToCompBegin()
	{
		final var now = System.currentTimeMillis();
		if ((_compStart < now) && (_compEnd > now))
		{
			return 10L;
		}
		
		if (_compStart > now)
		{
			return (_compStart - now);
		}
		return setNewCompBegin();
	}
	
	private long getMillisToTrainingBegin()
	{
		final var now = System.currentTimeMillis();
		if ((_trainingStart < now) && (_trainingEnd > now))
		{
			return 10L;
		}
		
		if (_trainingStart > now)
		{
			return (_trainingStart - now);
		}
		return setNewTrainingBegin();
	}
	
	private long setNewCompBegin()
	{
		final var timePattern = new SchedulingPattern(Config.ALT_OLY_START_TIME);
		_compStart = timePattern.next(System.currentTimeMillis());
		_compEnd = _compStart + (Config.ALT_OLY_CPERIOD * 3600000);
		updateCompDbStatus();
		return (_compStart - System.currentTimeMillis());
	}
	
	private long setNewTrainingBegin()
	{
		final var timePattern = new SchedulingPattern(Config.ALT_OLY_TRAINING_TIME);
		_trainingStart = timePattern.next(System.currentTimeMillis());
		_trainingEnd = _trainingStart + (Config.ALT_OLY_TPERIOD * 3600000);
		
		if (_trainingStart > _compStart || _trainingEnd > _compStart)
		{
			return -1;
		}
		updateTrainingDbStatus();
		return (_trainingStart - System.currentTimeMillis());
	}
	
	protected long getMillisToCompEnd()
	{
		return (_compEnd - System.currentTimeMillis());
	}
	
	protected long getMillisToTrainingEnd()
	{
		return (_trainingEnd - System.currentTimeMillis());
	}
	
	private long getMillisToWeekChange()
	{
		final var now = System.currentTimeMillis();
		if (_nextWeeklyChange > now)
		{
			return (_nextWeeklyChange - now);
		}
		return getWeeklyChangeDate();
	}
	
	private void scheduleWeeklyChange()
	{
		_scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				addWeeklyPoints();
				_log.info("Olympiad: Added weekly points to nobles.");
				resetWeeklyMatches();
				_log.info("Olympiad: Reset weekly matches to nobles.");
				
				_nextWeeklyChange = getWeeklyChangeDate();
			}
		}, getMillisToWeekChange(), getWeeklyChangeDate());
	}
	
	protected synchronized void addWeeklyPoints()
	{
		if (_period == 1)
		{
			return;
		}
		
		int currentPoints;
		for (final StatsSet nobleInfo : NOBLES.values())
		{
			currentPoints = nobleInfo.getInteger(POINTS);
			currentPoints += WEEKLY_POINTS;
			nobleInfo.set(POINTS, currentPoints);
		}
	}
	
	public synchronized static void addDailyPoints()
	{
		if (DAILY_POINTS <= 0)
		{
			return;
		}
		
		int currentPoints;
		for (final StatsSet nobleInfo : NOBLES.values())
		{
			currentPoints = nobleInfo.getInteger(POINTS);
			currentPoints += DAILY_POINTS;
			nobleInfo.set(POINTS, currentPoints);
		}
		_log.info("Olympiad: Added daily points to nobles.");
	}
	
	protected synchronized void resetWeeklyMatches()
	{
		if (_period == 1)
		{
			return;
		}
		
		for (final StatsSet nobleInfo : NOBLES.values())
		{
			nobleInfo.set(COMP_DONE_WEEK, 0);
			nobleInfo.set(COMP_DONE_WEEK_CLASSED, 0);
			nobleInfo.set(COMP_DONE_WEEK_NON_CLASSED, 0);
			nobleInfo.set(COMP_DONE_WEEK_TEAM, 0);
		}
	}
	
	public int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public boolean playerInStadia(Player player)
	{
		return (ZoneManager.getInstance().getOlympiadStadium(player) != null);
	}
	
	protected synchronized void saveNobleData()
	{
		if (NOBLES.isEmpty())
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			
			for (final Entry<Integer, StatsSet> entry : NOBLES.entrySet())
			{
				final StatsSet nobleInfo = entry.getValue();
				
				if (nobleInfo == null)
				{
					continue;
				}
				
				final int charId = entry.getKey();
				final int classId = nobleInfo.getInteger(CLASS_ID);
				final int points = nobleInfo.getInteger(POINTS);
				final int points_past = nobleInfo.getInteger(POINTS_PAST);
				final int compDone = nobleInfo.getInteger(COMP_DONE);
				final int compWon = nobleInfo.getInteger(COMP_WON);
				final int compLost = nobleInfo.getInteger(COMP_LOST);
				final int compDrawn = nobleInfo.getInteger(COMP_DRAWN);
				final int compDoneWeek = nobleInfo.getInteger(COMP_DONE_WEEK);
				final int compDoneWeekClassed = nobleInfo.getInteger(COMP_DONE_WEEK_CLASSED);
				final int compDoneWeekNonClassed = nobleInfo.getInteger(COMP_DONE_WEEK_NON_CLASSED);
				final int compDoneWeekTeam = nobleInfo.getInteger(COMP_DONE_WEEK_TEAM);
				final boolean toSave = nobleInfo.getBool("to_save");
				
				statement = con.prepareStatement(toSave ? OLYMPIAD_SAVE_NOBLES : OLYMPIAD_UPDATE_NOBLES);
				if (toSave)
				{
					statement.setInt(1, charId);
					statement.setInt(2, classId);
					statement.setInt(3, points);
					statement.setInt(4, points_past);
					statement.setInt(5, compDone);
					statement.setInt(6, compWon);
					statement.setInt(7, compLost);
					statement.setInt(8, compDrawn);
					statement.setInt(9, compDoneWeek);
					statement.setInt(10, compDoneWeekClassed);
					statement.setInt(11, compDoneWeekNonClassed);
					statement.setInt(12, compDoneWeekTeam);
					
					nobleInfo.set("to_save", false);
				}
				else
				{
					statement.setInt(1, classId);
					statement.setInt(2, points);
					statement.setInt(3, points_past);
					statement.setInt(4, compDone);
					statement.setInt(5, compWon);
					statement.setInt(6, compLost);
					statement.setInt(7, compDrawn);
					statement.setInt(8, compDoneWeek);
					statement.setInt(9, compDoneWeekClassed);
					statement.setInt(10, compDoneWeekNonClassed);
					statement.setInt(11, compDoneWeekTeam);
					statement.setInt(12, charId);
				}
				statement.execute();
				statement.close();
			}
		}
		catch (final SQLException e)
		{
			_log.warn("Olympiad: Failed to save noblesse data to database: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void saveOlympiadStatus()
	{
		saveNobleData();
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OLYMPIAD_SAVE_DATA);
			statement.setInt(1, _currentCycle);
			statement.setInt(2, _period);
			statement.setLong(3, _compStart);
			statement.setLong(4, _compEnd);
			statement.setLong(5, _trainingStart);
			statement.setLong(6, _trainingEnd);
			statement.setLong(7, _olympiadEnd);
			statement.setLong(8, _validationEnd);
			statement.setLong(9, _nextWeeklyChange);
			statement.setInt(10, _currentCycle);
			statement.setInt(11, _period);
			statement.setLong(12, _olympiadEnd);
			statement.setLong(13, _validationEnd);
			statement.setLong(14, _nextWeeklyChange);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.warn("Olympiad: Failed to save olympiad data to database: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	protected void updateMonthlyData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OLYMPIAD_MONTH_CLEAR);
			final var statement2 = con.prepareStatement(OLYMPIAD_MONTH_CREATE);
			statement.execute();
			statement2.execute();
			statement2.close();
		}
		catch (final SQLException e)
		{
			_log.warn("Olympiad: Failed to update monthly noblese data: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(statement);
		}
		
		try
		{
			statement = con.prepareStatement(OLYMPIAD_CALCULATE_POINTS);
			statement.setInt(1, Config.ALT_OLY_MIN_MATCHES);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.warn("Olympiad: Failed to calculate noblese points: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		for (final Integer nobleId : NOBLES.keySet())
		{
			final StatsSet nobleInfo = NOBLES.get(nobleId);
			final int points = nobleInfo.getInteger(Olympiad.POINTS);
			final int compDone = nobleInfo.getInteger(Olympiad.COMP_DONE);
			if (compDone >= Config.ALT_OLY_MIN_MATCHES)
			{
				nobleInfo.set(Olympiad.POINTS_PAST, points);
			}
			else
			{
				nobleInfo.set(Olympiad.POINTS_PAST, 0);
			}
		}
	}
	
	protected void sortHerosToBe(boolean calcHeroPoints)
	{
		if (_period != 1)
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OLYMPIAD_GET_HEROS);
			StatsSet hero;
			final List<StatsSet> soulHounds = new ArrayList<>();
			for (final int element : HERO_IDS)
			{
				statement.setInt(1, element);
				rset = statement.executeQuery();
				if (rset.next())
				{
					hero = new StatsSet();
					hero.set(CLASS_ID, element);
					hero.set(CHAR_ID, rset.getInt(CHAR_ID));
					hero.set(CHAR_NAME, rset.getString(CHAR_NAME));
					
					if ((element == 132) || (element == 133))
					{
						hero = NOBLES.get(hero.getInteger(CHAR_ID));
						hero.set(CHAR_ID, rset.getInt(CHAR_ID));
						soulHounds.add(hero);
					}
					else
					{
						HEROS_TO_BE.add(hero);
					}
				}
			}
			
			switch (soulHounds.size())
			{
				case 0 :
				{
					break;
				}
				case 1 :
				{
					hero = new StatsSet();
					final StatsSet winner = soulHounds.get(0);
					hero.set(CLASS_ID, winner.getInteger(CLASS_ID));
					hero.set(CHAR_ID, winner.getInteger(CHAR_ID));
					hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
					HEROS_TO_BE.add(hero);
					break;
				}
				case 2 :
				{
					if (Config.ALLOW_SOULHOOD_DOUBLE)
					{
						for (final var winner : soulHounds)
						{
							hero = new StatsSet();
							hero.set(CLASS_ID, winner.getInteger(CLASS_ID));
							hero.set(CHAR_ID, winner.getInteger(CHAR_ID));
							hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
							HEROS_TO_BE.add(hero);
						}
					}
					else
					{
						hero = new StatsSet();
						StatsSet winner;
						final StatsSet hero1 = soulHounds.get(0);
						final StatsSet hero2 = soulHounds.get(1);
						final int hero1Points = hero1.getInteger(POINTS);
						final int hero2Points = hero2.getInteger(POINTS);
						final int hero1Comps = hero1.getInteger(COMP_DONE);
						final int hero2Comps = hero2.getInteger(COMP_DONE);
						final int hero1Wins = hero1.getInteger(COMP_WON);
						final int hero2Wins = hero2.getInteger(COMP_WON);
						
						if (hero1Points > hero2Points)
						{
							winner = hero1;
						}
						else if (hero2Points > hero1Points)
						{
							winner = hero2;
						}
						else
						{
							if (hero1Comps > hero2Comps)
							{
								winner = hero1;
							}
							else if (hero2Comps > hero1Comps)
							{
								winner = hero2;
							}
							else
							{
								if (hero1Wins > hero2Wins)
								{
									winner = hero1;
								}
								else
								{
									winner = hero2;
								}
							}
						}
						
						hero.set(CLASS_ID, winner.getInteger(CLASS_ID));
						hero.set(CHAR_ID, winner.getInteger(CHAR_ID));
						hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
						HEROS_TO_BE.add(hero);
					}
					break;
				}
			}
		}
		catch (final SQLException e)
		{
			_log.warn("Olympiad: Couldnt load heros from DB");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (calcHeroPoints && !HEROS_TO_BE.isEmpty())
		{
			for (final var hero : HEROS_TO_BE)
			{
				final int charId = hero.getInteger(CHAR_ID);
				final var nobleInfo = NOBLES.get(charId);
				final int points = nobleInfo.getInteger(POINTS_PAST);
				nobleInfo.set(POINTS_PAST, (points + Config.ALT_OLY_HERO_POINTS));
			}
		}
	}
	
	public List<OlympiadTemplate> getClassLeaderBoard(int classId)
	{
		final List<OlympiadTemplate> list = new ArrayList<>();
		int rank = 1;
		final String query = Config.ALT_OLY_SHOW_MONTHLY_WINNERS ? ((classId == 132) ? GET_EACH_CLASS_LEADER_SOULHOUND : GET_EACH_CLASS_LEADER) : ((classId == 132) ? GET_EACH_CLASS_LEADER_CURRENT_SOULHOUND : GET_EACH_CLASS_LEADER_CURRENT);
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			statement.setInt(1, classId);
			rset = statement.executeQuery();
			while (rset.next())
			{
				final long points = rset.getLong("olympiad_points");
				final double win = rset.getDouble("competitions_won");
				final double lose = rset.getDouble("competitions_lost");
				final double mod = (win / (win + lose)) * 100;
				list.add(new OlympiadTemplate(rank, rset.getString(CHAR_NAME), points, (int) win, (int) lose, (int) mod));
				rank++;
			}
		}
		catch (final SQLException e)
		{
			_log.warn("Olympiad: Couldn't load olympiad leaders from DB!");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return list;
	}
	
	public int getNoblessePasses(Player player, boolean clear)
	{
		if (player == null || NOBLES_RANK.isEmpty())
		{
			return 0;
		}
		
		final int objId = player.getObjectId();
		if (!NOBLES_RANK.containsKey(objId))
		{
			return 0;
		}
		
		final StatsSet noble = NOBLES.get(objId);
		if ((noble == null) || (noble.getInteger(POINTS_PAST) == 0))
		{
			return 0;
		}
		
		final int rank = NOBLES_RANK.get(objId);
		int points = getNoblePointsPast(objId);
		switch (rank)
		{
			case 1 :
				points += Config.ALT_OLY_RANK1_POINTS;
				break;
			case 2 :
				points += Config.ALT_OLY_RANK2_POINTS;
				break;
			case 3 :
				points += Config.ALT_OLY_RANK3_POINTS;
				break;
			case 4 :
				points += Config.ALT_OLY_RANK4_POINTS;
				break;
			case 5 :
			default :
				points += Config.ALT_OLY_RANK5_POINTS;
		}
		
		if (clear)
		{
			noble.set(POINTS_PAST, 0);
		}
		points *= Config.ALT_OLY_GP_PER_POINT;
		return points;
	}
	
	public int getNoblePoints(int objId)
	{
		return !NOBLES.containsKey(objId) ? 0 : NOBLES.get(objId).getInteger(POINTS);
	}
	
	public int getNoblePointsPast(int objId)
	{
		return !NOBLES.containsKey(objId) ? 0 : NOBLES.get(objId).getInteger(POINTS_PAST);
	}
	
	public int getLastNobleOlympiadPoints(int objId)
	{
		int result = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT olympiad_points FROM olympiad_nobles_eom WHERE charId = ?");
			statement.setInt(1, objId);
			rset = statement.executeQuery();
			if (rset.first())
			{
				result = rset.getInt(1);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not load last olympiad points:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}
	
	public int getCompetitionDone(int objId)
	{
		if (!NOBLES.containsKey(objId))
		{
			return 0;
		}
		return NOBLES.get(objId).getInteger(COMP_DONE);
	}
	
	public int getCompetitionWon(int objId)
	{
		if (!NOBLES.containsKey(objId))
		{
			return 0;
		}
		return NOBLES.get(objId).getInteger(COMP_WON);
	}
	
	public int getCompetitionLost(int objId)
	{
		if (!NOBLES.containsKey(objId))
		{
			return 0;
		}
		return NOBLES.get(objId).getInteger(COMP_LOST);
	}
	
	public int getCompetitionDoneWeek(int objId)
	{
		if (!NOBLES.containsKey(objId))
		{
			return 0;
		}
		return NOBLES.get(objId).getInteger(COMP_DONE_WEEK);
	}
	
	public int getCompetitionDoneWeekClassed(int objId)
	{
		if (!NOBLES.containsKey(objId))
		{
			return 0;
		}
		return NOBLES.get(objId).getInteger(COMP_DONE_WEEK_CLASSED);
	}
	
	public int getCompetitionDoneWeekNonClassed(int objId)
	{
		if (!NOBLES.containsKey(objId))
		{
			return 0;
		}
		return NOBLES.get(objId).getInteger(COMP_DONE_WEEK_NON_CLASSED);
	}
	
	public int getCompetitionDoneWeekTeam(int objId)
	{
		if (!NOBLES.containsKey(objId))
		{
			return 0;
		}
		return NOBLES.get(objId).getInteger(COMP_DONE_WEEK_TEAM);
	}
	
	public int getRemainingWeeklyMatches(int objId)
	{
		return Math.max(Config.ALT_OLY_MAX_WEEKLY_MATCHES - getCompetitionDoneWeek(objId), 0);
	}
	
	public int getRemainingWeeklyMatchesClassed(int objId)
	{
		return Math.max(Config.ALT_OLY_MAX_WEEKLY_MATCHES_CLASSED - getCompetitionDoneWeekClassed(objId), 0);
	}
	
	public int getRemainingWeeklyMatchesNonClassed(int objId)
	{
		return Math.max(Config.ALT_OLY_MAX_WEEKLY_MATCHES_NON_CLASSED - getCompetitionDoneWeekNonClassed(objId), 0);
	}
	
	public int getRemainingWeeklyMatchesTeam(int objId)
	{
		return Math.max(Config.ALT_OLY_MAX_WEEKLY_MATCHES_TEAM - getCompetitionDoneWeekTeam(objId), 0);
	}
	
	protected void cleanupNobles()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OLYMPIAD_CLEANUP_NOBLES);
			statement.setInt(1, Olympiad.DEFAULT_POINTS);
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.warn("Olympiad: Couldn't clean up nobles from DB!");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		for (final StatsSet nobleInfo : NOBLES.values())
		{
			nobleInfo.set(Olympiad.POINTS, Olympiad.DEFAULT_POINTS);
			nobleInfo.set(Olympiad.COMP_DONE, 0);
			nobleInfo.set(Olympiad.COMP_WON, 0);
			nobleInfo.set(Olympiad.COMP_LOST, 0);
			nobleInfo.set(Olympiad.COMP_DRAWN, 0);
			nobleInfo.set(Olympiad.COMP_DONE_WEEK, 0);
			nobleInfo.set(Olympiad.COMP_DONE_WEEK_CLASSED, 0);
			nobleInfo.set(Olympiad.COMP_DONE_WEEK_NON_CLASSED, 0);
			nobleInfo.set(Olympiad.COMP_DONE_WEEK_TEAM, 0);
			nobleInfo.set("to_save", false);
		}
	}
	
	public int getNobleFights(int objId)
	{
		return !NOBLES.containsKey(objId) ? 0 : NOBLES.get(objId).getInteger(COMP_DONE);
	}
	
	public int getPeriod()
	{
		return _period;
	}
	
	protected static StatsSet addNobleStats(int charId, StatsSet data)
	{
		return NOBLES.put(Integer.valueOf(charId), data);
	}
	
	public void updateNobleClass(Player noble)
	{
		final StatsSet statDat = getNobleStats(noble.getObjectId());
		if (statDat != null)
		{
			final int classId = NOBLES.get(noble.getObjectId()).getInteger(CLASS_ID);
			if (classId != noble.getBaseClass())
			{
				statDat.set(Olympiad.CLASS_ID, noble.getBaseClass());
			}
		}
	}
	
	public static synchronized void addNoble(Player noble)
	{
		StatsSet statDat = getNobleStats(noble.getObjectId());
		if (statDat != null)
		{
			final int classId = NOBLES.get(noble.getObjectId()).getInteger(CLASS_ID);
			if (classId != noble.getBaseClass())
			{
				statDat.set(Olympiad.CLASS_ID, noble.getBaseClass());
			}
		}
		else
		{
			statDat = new StatsSet();
			statDat.set(Olympiad.CLASS_ID, noble.getBaseClass());
			statDat.set(Olympiad.CHAR_NAME, noble.getName(null));
			statDat.set(Olympiad.POINTS, Olympiad.DEFAULT_POINTS);
			statDat.set(Olympiad.POINTS_PAST, 0);
			statDat.set(Olympiad.COMP_DONE, 0);
			statDat.set(Olympiad.COMP_WON, 0);
			statDat.set(Olympiad.COMP_LOST, 0);
			statDat.set(Olympiad.COMP_DRAWN, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK_CLASSED, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK_NON_CLASSED, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK_TEAM, 0);
			statDat.set("to_save", true);
			addNobleStats(noble.getObjectId(), statDat);
			noble.getCounters().addAchivementInfo("setNobless", 0, -1, false, false, false);
		}
	}
	
	public static synchronized void removeNoble(Player noble)
	{
		NOBLES.remove(noble.getObjectId());
	}
	
	private static class SingletonHolder
	{
		protected static final Olympiad _instance = new Olympiad();
	}
}