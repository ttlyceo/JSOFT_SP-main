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
package l2e.gameserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.AutoSpawnHandler;
import l2e.gameserver.model.AutoSpawnHandler.AutoSpawnInstance;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SSQInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class SevenSigns extends LoggerObject
{
	public static final String SEVEN_SIGNS_HTML_PATH = "data/html/seven_signs/";
	
	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;

	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;
	
	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;
	
	public static final int PERIOD_START_HOUR = 18;
	public static final int PERIOD_START_MINS = 00;
	public static final int PERIOD_START_DAY = Calendar.MONDAY;
	
	public static final int PERIOD_MINOR_LENGTH = 900000;
	public static final int PERIOD_MAJOR_LENGTH = 604800000 - PERIOD_MINOR_LENGTH;
	
	public static final int RECORD_SEVEN_SIGNS_ID = 5707;
	public static final int RECORD_SEVEN_SIGNS_COST = 500;
	
	public static final int ORATOR_NPC_ID = 31094;
	public static final int PREACHER_NPC_ID = 31093;
	public static final int MAMMON_MERCHANT_ID = 31113;
	public static final int MAMMON_BLACKSMITH_ID = 31126;
	public static final int MAMMON_MARKETEER_ID = 31092;
	public static final int SPIRIT_IN_ID = 31111;
	public static final int SPIRIT_OUT_ID = 31112;
	public static final int LILITH_NPC_ID = 25283;
	public static final int ANAKIM_NPC_ID = 25286;
	public static final int CREST_OF_DAWN_ID = 31170;
	public static final int CREST_OF_DUSK_ID = 31171;
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;
	
	public static final int[] SEAL_STONE_IDS =
	{
	        SEAL_STONE_BLUE_ID, SEAL_STONE_GREEN_ID, SEAL_STONE_RED_ID
	};
	
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;
	
	public static final int BLUE_CONTRIB_POINTS = 3;
	public static final int GREEN_CONTRIB_POINTS = 5;
	public static final int RED_CONTRIB_POINTS = 10;
	
	private final Calendar _nextPeriodChange = Calendar.getInstance();
	
	protected int _activePeriod;
	protected int _currentCycle;
	protected double _dawnStoneScore;
	protected double _duskStoneScore;
	protected int _dawnFestivalScore;
	protected int _duskFestivalScore;
	protected int _compWinner;
	protected int _previousWinner;
	protected Calendar _lastSave = Calendar.getInstance();
	
	protected Map<Integer, StatsSet> _signsPlayerData = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsSealOwners = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsDuskSealTotals = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsDawnSealTotals = new LinkedHashMap<>();
	
	private AutoSpawnInstance _merchantSpawn;
	private AutoSpawnInstance _blacksmithSpawn;
	private AutoSpawnInstance _spiritInSpawn;
	private AutoSpawnInstance _spiritOutSpawn;
	private AutoSpawnInstance _lilithSpawn;
	private AutoSpawnInstance _anakimSpawn;
	
	private static final String LOAD_DATA = "SELECT charId, cabal, seal, red_stones, green_stones, blue_stones, " + "ancient_adena_amount, contribution_score FROM seven_signs";
	
	private static final String LOAD_STATUS = "SELECT * FROM seven_signs_status WHERE id=0";
	
	private static final String INSERT_PLAYER = "INSERT INTO seven_signs (charId, cabal, seal) VALUES (?,?,?)";
	
	private static final String UPDATE_PLAYER = "UPDATE seven_signs SET cabal=?, seal=?, red_stones=?, green_stones=?, blue_stones=?, " + "ancient_adena_amount=?, contribution_score=? WHERE charId=?";
	
	private static final String UPDATE_STATUS = "UPDATE seven_signs_status SET current_cycle=?, active_period=?, previous_winner=?, " + "dawn_stone_score=?, dawn_festival_score=?, dusk_stone_score=?, dusk_festival_score=?, " + "avarice_owner=?, gnosis_owner=?, strife_owner=?, avarice_dawn_score=?, gnosis_dawn_score=?, " + "strife_dawn_score=?, avarice_dusk_score=?, gnosis_dusk_score=?, strife_dusk_score=?, " + "festival_cycle=?, accumulated_bonus0=?, accumulated_bonus1=?, accumulated_bonus2=?," + "accumulated_bonus3=?, accumulated_bonus4=?, date=? WHERE id=0";
	
	protected void despawnAll()
	{
		SpawnParser.getInstance().despawnGroup("dawn_spawn");
		SpawnParser.getInstance().despawnGroup("dusk_spawn");
		SpawnParser.getInstance().despawnGroup("dawn_victory_spawn");
		SpawnParser.getInstance().despawnGroup("dusk_victory_spawn");
	}

	private void changePeriodSpawn()
	{
		int mode = 0;
		if (isSealValidationPeriod())
		{
			mode = getCabalHighestScore();
		}
		
		despawnAll();
		switch (mode)
		{
			case CABAL_NULL :
				SpawnParser.getInstance().spawnGroup("dawn_spawn");
				SpawnParser.getInstance().spawnGroup("dusk_spawn");
				break;
			case CABAL_DUSK :
				SpawnParser.getInstance().spawnGroup("dusk_spawn");
				SpawnParser.getInstance().spawnGroup("dusk_victory_spawn");
				break;
			case CABAL_DAWN :
				SpawnParser.getInstance().spawnGroup("dawn_spawn");
				SpawnParser.getInstance().spawnGroup("dawn_victory_spawn");
				break;
		}
	}

	protected SevenSigns()
	{
		try
		{
			restoreSevenSignsData();
		}
		catch (final Exception e)
		{
			warn("Failed to load configuration: " + e.getMessage(), e);
		}
		
		info("Currently in the " + getCurrentPeriodName() + " period!");
		initializeSeals();
		
		if (isSealValidationPeriod())
		{
			if (getCabalHighestScore() == CABAL_NULL)
			{
				info("The competition ended with a tie last week.");
			}
			else
			{
				info("The " + getCabalName(getCabalHighestScore()) + " were victorious last week.");
			}
		}
		else if (getCabalHighestScore() == CABAL_NULL)
		{
			info("If trend continues, will end in a tie this week.");
		}
		else
		{
			info("The " + getCabalName(getCabalHighestScore()) + " are in the lead this week.");
		}
		
		var milliToChange = 0L;
		if (isNextPeriodChangeInPast())
		{
			info("Next period change was in the past (server was offline), changing periods now!");
		}
		else
		{
			setCalendarForNextPeriodChange();
			milliToChange = getMilliToPeriodChange();
		}
		
		final SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
		ThreadPoolManager.getInstance().schedule(sspc, milliToChange);
		
		final var numSecs = (milliToChange / 1000) % 60;
		var countDown = ((milliToChange / 1000.0) - numSecs) / 60;
		final var numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		final var numHours = (int) Math.floor(countDown % 24);
		final var numDays = (int) Math.floor((countDown - numHours) / 24);
		
		changePeriodSpawn();
		
		info("Next period begins in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
		
	}
	
	private boolean isNextPeriodChangeInPast()
	{
		final var lastPeriodChange = Calendar.getInstance();
		switch (getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION :
			case PERIOD_COMPETITION :
				lastPeriodChange.set(Calendar.DAY_OF_WEEK, PERIOD_START_DAY);
				lastPeriodChange.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				lastPeriodChange.set(Calendar.MINUTE, PERIOD_START_MINS);
				lastPeriodChange.set(Calendar.SECOND, 0);
				
				if (Calendar.getInstance().before(lastPeriodChange))
				{
					lastPeriodChange.add(Calendar.HOUR, -24 * 7);
				}
				break;
			case PERIOD_COMP_RECRUITING :
			case PERIOD_COMP_RESULTS :
				lastPeriodChange.setTimeInMillis(_lastSave.getTimeInMillis() + PERIOD_MINOR_LENGTH);
				break;
		}
		
		if ((_lastSave.getTimeInMillis() > 7) && _lastSave.before(lastPeriodChange))
		{
			return true;
		}
		return false;
	}
	
	public void spawnSevenSignsNPC()
	{
		_merchantSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(MAMMON_MERCHANT_ID, false);
		_blacksmithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(MAMMON_BLACKSMITH_ID, false);
		_spiritInSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(SPIRIT_IN_ID, false);
		_spiritOutSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(SPIRIT_OUT_ID, false);
		_lilithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(LILITH_NPC_ID, false);
		_anakimSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(ANAKIM_NPC_ID, false);
		final List<AutoSpawnInstance> marketeerSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(MAMMON_MARKETEER_ID);
		final List<AutoSpawnInstance> crestofdawnspawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(CREST_OF_DAWN_ID);
		final List<AutoSpawnInstance> crestofduskspawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(CREST_OF_DUSK_ID);
		final List<AutoSpawnInstance> oratorSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(ORATOR_NPC_ID);
		final List<AutoSpawnInstance> preacherSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(PREACHER_NPC_ID);
		
		if (isSealValidationPeriod() || isCompResultsPeriod())
		{
			for (final var spawnInst : marketeerSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
			}
			
			if ((getSealOwner(SEAL_GNOSIS) == getCabalHighestScore()) && (getSealOwner(SEAL_GNOSIS) != CABAL_NULL))
			{
				if (!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					_blacksmithSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_blacksmithSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(_blacksmithSpawn, true);
				}
				
				for (final var spawnInst : oratorSpawns)
				{
					if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
					}
				}
				
				for (final var spawnInst : preacherSpawns)
				{
					if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
					}
				}
			}
			else
			{
				AutoSpawnHandler.getInstance().setSpawnActive(_blacksmithSpawn, false);
				
				for (final var spawnInst : oratorSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
				}
				
				for (final var spawnInst : preacherSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
				}
			}
			
			if ((getSealOwner(SEAL_AVARICE) == getCabalHighestScore()) && (getSealOwner(SEAL_AVARICE) != CABAL_NULL))
			{
				if (!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					_merchantSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_merchantSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(_merchantSpawn, true);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_spiritInSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(_spiritInSpawn, true);
				}
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_spiritOutSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(_spiritOutSpawn, true);
				}
				
				switch (getCabalHighestScore())
				{
					case CABAL_DAWN :
						if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_lilithSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawnHandler.getInstance().setSpawnActive(_lilithSpawn, true);
						}
						
						AutoSpawnHandler.getInstance().setSpawnActive(_anakimSpawn, false);
						
						for (final var dawnCrest : crestofdawnspawns)
						{
							if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(dawnCrest.getObjectId(), true).isSpawnActive())
							{
								AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, true);
							}
						}
						
						for (final var duskCrest : crestofduskspawns)
						{
							AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
						}
						break;
					
					case CABAL_DUSK :
						if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_anakimSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawnHandler.getInstance().setSpawnActive(_anakimSpawn, true);
						}
						
						AutoSpawnHandler.getInstance().setSpawnActive(_lilithSpawn, false);
						
						for (final var duskCrest : crestofduskspawns)
						{
							if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(duskCrest.getObjectId(), true).isSpawnActive())
							{
								AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, true);
							}
						}
						
						for (final var dawnCrest : crestofdawnspawns)
						{
							AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
						}
						break;
				}
			}
			else
			{
				AutoSpawnHandler.getInstance().setSpawnActive(_merchantSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(_spiritInSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(_spiritOutSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(_lilithSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(_anakimSpawn, false);
				for (final var dawnCrest : crestofdawnspawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
				}
				for (final var duskCrest : crestofduskspawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
				}
			}
		}
		else
		{
			AutoSpawnHandler.getInstance().setSpawnActive(_merchantSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_blacksmithSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_lilithSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_anakimSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_spiritInSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_spiritOutSpawn, false);
			for (final var dawnCrest : crestofdawnspawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
			}
			for (final var duskCrest : crestofduskspawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
			}
			for (final var spawnInst : oratorSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (final var spawnInst : preacherSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (final var spawnInst : marketeerSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
		}
	}
	
	public static SevenSigns getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static long calcContributionScore(long blueCount, long greenCount, long redCount)
	{
		var contrib = blueCount * BLUE_CONTRIB_POINTS;
		contrib += greenCount * GREEN_CONTRIB_POINTS;
		contrib += redCount * RED_CONTRIB_POINTS;
		
		return contrib;
	}
	
	public static long calcAncientAdenaReward(long blueCount, long greenCount, long redCount)
	{
		var reward = blueCount * SEAL_STONE_BLUE_VALUE;
		reward += greenCount * SEAL_STONE_GREEN_VALUE;
		reward += redCount * SEAL_STONE_RED_VALUE;
		
		return reward;
	}
	
	public static final String getCabalShortName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN :
				return "dawn";
			case CABAL_DUSK :
				return "dusk";
		}
		return "No Cabal";
	}
	
	public static int getCabalNumber(String cabal)
	{
		switch (cabal)
		{
			case "dawn" :
				return CABAL_DAWN;
			case "dusk" :
				return CABAL_DUSK;
		}
		return CABAL_NULL;
	}
	
	public static final String getCabalName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN :
				return "Lords of Dawn";
			case CABAL_DUSK :
				return "Revolutionaries of Dusk";
		}
		
		return "No Cabal";
	}
	
	public static final String getSealName(int seal, boolean shortName)
	{
		var sealName = (!shortName) ? "Seal of " : "";
		
		switch (seal)
		{
			case SEAL_AVARICE :
				sealName += "Avarice";
				break;
			case SEAL_GNOSIS :
				sealName += "Gnosis";
				break;
			case SEAL_STRIFE :
				sealName += "Strife";
				break;
		}
		
		return sealName;
	}
	
	public final int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public final int getCurrentPeriod()
	{
		return _activePeriod;
	}
	
	private final int getDaysToPeriodChange()
	{
		final var numDays = _nextPeriodChange.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;
		if (numDays < 0)
		{
			return 0 - numDays;
		}
		return 7 - numDays;
	}
	
	public final long getMilliToPeriodChange()
	{
		return (_nextPeriodChange.getTimeInMillis() - System.currentTimeMillis());
	}
	
	protected void setCalendarForNextPeriodChange()
	{
		switch (getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION :
			case PERIOD_COMPETITION :
				var daysToChange = getDaysToPeriodChange();
				
				if (daysToChange == 7)
				{
					if (_nextPeriodChange.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR)
					{
						daysToChange = 0;
					}
					else if ((_nextPeriodChange.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR) && (_nextPeriodChange.get(Calendar.MINUTE) < PERIOD_START_MINS))
					{
						daysToChange = 0;
					}
				}
				
				if (daysToChange > 0)
				{
					_nextPeriodChange.add(Calendar.DATE, daysToChange);
				}
				
				_nextPeriodChange.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				_nextPeriodChange.set(Calendar.MINUTE, PERIOD_START_MINS);
				break;
			case PERIOD_COMP_RECRUITING :
			case PERIOD_COMP_RESULTS :
				_nextPeriodChange.add(Calendar.MILLISECOND, PERIOD_MINOR_LENGTH);
				break;
		}
		info("Next period change set to " + _nextPeriodChange.getTime());
	}
	
	public final String getCurrentPeriodName()
	{
		String periodName = null;
		
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING :
				periodName = "Quest Event Initialization";
				break;
			case PERIOD_COMPETITION :
				periodName = "Competition (Quest Event)";
				break;
			case PERIOD_COMP_RESULTS :
				periodName = "Quest Event Results";
				break;
			case PERIOD_SEAL_VALIDATION :
				periodName = "Seal Validation";
				break;
		}
		return periodName;
	}
	
	public final boolean isCompetitionPeriod()
	{
		return (_activePeriod == PERIOD_COMPETITION);
	}
	
	public final boolean isSealValidationPeriod()
	{
		return (_activePeriod == PERIOD_SEAL_VALIDATION);
	}
	
	public final boolean isCompResultsPeriod()
	{
		return (_activePeriod == PERIOD_COMP_RESULTS);
	}
	
	public boolean isDateInSealValidPeriod(Calendar date)
	{
		final var nextPeriodChange = getMilliToPeriodChange();
		var nextQuestStart = 0L;
		var nextValidStart = 0L;
		var tillDate = date.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
		while (((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH)) < tillDate)
		{
			tillDate -= ((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH));
		}
		while (tillDate < 0)
		{
			tillDate += ((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH));
		}
		
		switch (getCurrentPeriod())
		{
			case PERIOD_COMP_RECRUITING :
				nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH;
				nextQuestStart = nextValidStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_COMPETITION :
				nextValidStart = nextPeriodChange;
				nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_COMP_RESULTS :
				nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH;
				nextValidStart = nextQuestStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_SEAL_VALIDATION :
				nextQuestStart = nextPeriodChange;
				nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
		}
		
		if (((nextQuestStart < tillDate) && (tillDate < nextValidStart)) || ((nextValidStart < nextQuestStart) && ((tillDate < nextValidStart) || (nextQuestStart < tillDate))))
		{
			return false;
		}
		return true;
	}
	
	public final int getCurrentScore(int cabal)
	{
		final var totalStoneScore = _dawnStoneScore + _duskStoneScore;
		
		switch (cabal)
		{
			case CABAL_NULL :
				return 0;
			case CABAL_DAWN :
				return Math.round((float) (_dawnStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _dawnFestivalScore;
			case CABAL_DUSK :
				return Math.round((float) (_duskStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _duskFestivalScore;
		}
		return 0;
	}
	
	public final double getCurrentStoneScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL :
				return 0;
			case CABAL_DAWN :
				return _dawnStoneScore;
			case CABAL_DUSK :
				return _duskStoneScore;
		}
		return 0;
	}
	
	public final int getCurrentFestivalScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL :
				return 0;
			case CABAL_DAWN :
				return _dawnFestivalScore;
			case CABAL_DUSK :
				return _duskFestivalScore;
		}
		return 0;
	}
	
	public final int getCabalHighestScore()
	{
		if (getCurrentScore(CABAL_DUSK) == getCurrentScore(CABAL_DAWN))
		{
			return CABAL_NULL;
		}
		else if (getCurrentScore(CABAL_DUSK) > getCurrentScore(CABAL_DAWN))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_DAWN;
		}
	}
	
	public final int getSealOwner(int seal)
	{
		return _signsSealOwners.get(seal);
	}
	
	public final int getSealProportion(int seal, int cabal)
	{
		if (cabal == CABAL_NULL)
		{
			return 0;
		}
		else if (cabal == CABAL_DUSK)
		{
			return _signsDuskSealTotals.get(seal);
		}
		else
		{
			return _signsDawnSealTotals.get(seal);
		}
	}
	
	public final int getTotalMembers(int cabal)
	{
		var cabalMembers = 0;
		final var cabalName = getCabalShortName(cabal);
		
		for (final StatsSet sevenDat : _signsPlayerData.values())
		{
			if (sevenDat != null && sevenDat.getString("cabal").equals(cabalName))
			{
				cabalMembers++;
			}
		}
		return cabalMembers;
	}
	
	public int getPlayerStoneContrib(int objectId)
	{
		final var currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		var stoneCount = 0;
		stoneCount += currPlayer.getInteger("red_stones");
		stoneCount += currPlayer.getInteger("green_stones");
		stoneCount += currPlayer.getInteger("blue_stones");
		
		return stoneCount;
	}
	
	public int getPlayerContribScore(int objectId)
	{
		final var currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		return currPlayer.getInteger("contribution_score");
	}
	
	public int getPlayerAdenaCollect(int objectId)
	{
		final var currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		return currPlayer.getInteger("ancient_adena_amount");
	}
	
	public int getPlayerSeal(int objectId)
	{
		final var currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return SEAL_NULL;
		}
		
		return currPlayer.getInteger("seal");
	}
	
	public int getPlayerCabal(int objectId)
	{
		final var currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return CABAL_NULL;
		}
		
		final var playerCabal = currPlayer.getString("cabal");
		if (playerCabal.equalsIgnoreCase("dawn"))
		{
			return CABAL_DAWN;
		}
		else if (playerCabal.equalsIgnoreCase("dusk"))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_NULL;
		}
	}
	
	protected void restoreSevenSignsData()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			try (
			    Statement s = con.createStatement(); ResultSet rs = s.executeQuery(LOAD_DATA))
			{
				StatsSet sevenDat = null;
				int charObjId;
				while (rs.next())
				{
					charObjId = rs.getInt("charId");
					
					sevenDat = new StatsSet();
					sevenDat.set("charId", charObjId);
					sevenDat.set("cabal", rs.getString("cabal"));
					sevenDat.set("seal", rs.getInt("seal"));
					sevenDat.set("red_stones", rs.getInt("red_stones"));
					sevenDat.set("green_stones", rs.getInt("green_stones"));
					sevenDat.set("blue_stones", rs.getInt("blue_stones"));
					sevenDat.set("ancient_adena_amount", rs.getDouble("ancient_adena_amount"));
					sevenDat.set("contribution_score", rs.getDouble("contribution_score"));
					
					_signsPlayerData.put(charObjId, sevenDat);
				}
			}
			
			try (
			    Statement s = con.createStatement(); ResultSet rs = s.executeQuery(LOAD_STATUS))
			{
				
				while (rs.next())
				{
					_currentCycle = rs.getInt("current_cycle");
					_activePeriod = rs.getInt("active_period");
					_previousWinner = rs.getInt("previous_winner");
					
					_dawnStoneScore = rs.getDouble("dawn_stone_score");
					_dawnFestivalScore = rs.getInt("dawn_festival_score");
					_duskStoneScore = rs.getDouble("dusk_stone_score");
					_duskFestivalScore = rs.getInt("dusk_festival_score");
					
					_signsSealOwners.put(SEAL_AVARICE, rs.getInt("avarice_owner"));
					_signsSealOwners.put(SEAL_GNOSIS, rs.getInt("gnosis_owner"));
					_signsSealOwners.put(SEAL_STRIFE, rs.getInt("strife_owner"));
					
					_signsDawnSealTotals.put(SEAL_AVARICE, rs.getInt("avarice_dawn_score"));
					_signsDawnSealTotals.put(SEAL_GNOSIS, rs.getInt("gnosis_dawn_score"));
					_signsDawnSealTotals.put(SEAL_STRIFE, rs.getInt("strife_dawn_score"));
					_signsDuskSealTotals.put(SEAL_AVARICE, rs.getInt("avarice_dusk_score"));
					_signsDuskSealTotals.put(SEAL_GNOSIS, rs.getInt("gnosis_dusk_score"));
					_signsDuskSealTotals.put(SEAL_STRIFE, rs.getInt("strife_dusk_score"));
					
					_lastSave.setTimeInMillis(rs.getLong("date"));
				}
			}
		}
		catch (final SQLException e)
		{
			warn("Unable to load Seven Signs data from database: " + e.getMessage(), e);
		}
	}
	
	public void saveSevenSignsData()
	{
		Connection con = null;
		PreparedStatement ps = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement(UPDATE_PLAYER);
			for (final var sevenDat : _signsPlayerData.values())
			{
				ps.setString(1, sevenDat.getString("cabal"));
				ps.setInt(2, sevenDat.getInteger("seal"));
				ps.setInt(3, sevenDat.getInteger("red_stones"));
				ps.setInt(4, sevenDat.getInteger("green_stones"));
				ps.setInt(5, sevenDat.getInteger("blue_stones"));
				ps.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
				ps.setDouble(7, sevenDat.getDouble("contribution_score"));
				ps.setInt(8, sevenDat.getInteger("charId"));
				ps.execute();
				ps.clearParameters();
			}
		}
		catch (final SQLException e)
		{
			warn("Unable to save data to database: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, ps);
		}
	}
	
	public final void saveSevenSignsData(int objectId)
	{
		final var sevenDat = _signsPlayerData.get(objectId);
		if (sevenDat == null)
		{
			return;
		}
		
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); PreparedStatement ps = con.prepareStatement(UPDATE_PLAYER))
		{
			ps.setString(1, sevenDat.getString("cabal"));
			ps.setInt(2, sevenDat.getInteger("seal"));
			ps.setInt(3, sevenDat.getInteger("red_stones"));
			ps.setInt(4, sevenDat.getInteger("green_stones"));
			ps.setInt(5, sevenDat.getInteger("blue_stones"));
			ps.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
			ps.setDouble(7, sevenDat.getDouble("contribution_score"));
			ps.setInt(8, sevenDat.getInteger("charId"));
			ps.execute();
		}
		catch (final SQLException e)
		{
			warn("Unable to save data to database: " + e.getMessage(), e);
		}
	}
	
	public final void saveSevenSignsStatus()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); PreparedStatement ps = con.prepareStatement(UPDATE_STATUS))
		{
			ps.setInt(1, _currentCycle);
			ps.setInt(2, _activePeriod);
			ps.setInt(3, _previousWinner);
			ps.setDouble(4, _dawnStoneScore);
			ps.setInt(5, _dawnFestivalScore);
			ps.setDouble(6, _duskStoneScore);
			ps.setInt(7, _duskFestivalScore);
			ps.setInt(8, _signsSealOwners.get(SEAL_AVARICE));
			ps.setInt(9, _signsSealOwners.get(SEAL_GNOSIS));
			ps.setInt(10, _signsSealOwners.get(SEAL_STRIFE));
			ps.setInt(11, _signsDawnSealTotals.get(SEAL_AVARICE));
			ps.setInt(12, _signsDawnSealTotals.get(SEAL_GNOSIS));
			ps.setInt(13, _signsDawnSealTotals.get(SEAL_STRIFE));
			ps.setInt(14, _signsDuskSealTotals.get(SEAL_AVARICE));
			ps.setInt(15, _signsDuskSealTotals.get(SEAL_GNOSIS));
			ps.setInt(16, _signsDuskSealTotals.get(SEAL_STRIFE));
			ps.setInt(17, SevenSignsFestival.getInstance().getCurrentFestivalCycle());
			
			for (int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
			{
				ps.setInt(18 + i, SevenSignsFestival.getInstance().getAccumulatedBonus(i));
			}
			_lastSave = Calendar.getInstance();
			ps.setLong(18 + SevenSignsFestival.FESTIVAL_COUNT, _lastSave.getTimeInMillis());
			ps.execute();
		}
		catch (final SQLException e)
		{
			warn("Unable to save data to database: " + e.getMessage(), e);
		}
	}
	
	protected void resetPlayerData()
	{
		int charObjId;
		
		for (final var sevenDat : _signsPlayerData.values())
		{
			charObjId = sevenDat.getInteger("charId");
			
			sevenDat.set("cabal", "");
			sevenDat.set("seal", SEAL_NULL);
			sevenDat.set("contribution_score", 0);
			
			_signsPlayerData.put(charObjId, sevenDat);
		}
	}
	
	public int setPlayerInfo(int objectId, int chosenCabal, int chosenSeal)
	{
		var currPlayerData = _signsPlayerData.get(objectId);
		
		if (currPlayerData != null)
		{
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			
			_signsPlayerData.put(objectId, currPlayerData);
		}
		else
		{
			currPlayerData = new StatsSet();
			currPlayerData.set("charId", objectId);
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			currPlayerData.set("red_stones", 0);
			currPlayerData.set("green_stones", 0);
			currPlayerData.set("blue_stones", 0);
			currPlayerData.set("ancient_adena_amount", 0);
			currPlayerData.set("contribution_score", 0);
			
			_signsPlayerData.put(objectId, currPlayerData);
			
			try (
			    var con = DatabaseFactory.getInstance().getConnection(); PreparedStatement ps = con.prepareStatement(INSERT_PLAYER))
			{
				ps.setInt(1, objectId);
				ps.setString(2, getCabalShortName(chosenCabal));
				ps.setInt(3, chosenSeal);
				ps.execute();
			}
			catch (final SQLException e)
			{
				warn("Failed to save data: " + e.getMessage(), e);
			}
		}
		
		if ("dawn".equals(currPlayerData.getString("cabal")))
		{
			_signsDawnSealTotals.put(chosenSeal, _signsDawnSealTotals.get(chosenSeal) + 1);
		}
		else
		{
			_signsDuskSealTotals.put(chosenSeal, _signsDuskSealTotals.get(chosenSeal) + 1);
		}
		
		if (!Config.ALT_SEVENSIGNS_LAZY_UPDATE)
		{
			saveSevenSignsStatus();
		}
		
		return chosenCabal;
	}
	
	public int getAncientAdenaReward(int objectId, boolean removeReward)
	{
		final var currPlayer = _signsPlayerData.get(objectId);
		final var rewardAmount = currPlayer.getInteger("ancient_adena_amount");
		
		currPlayer.set("red_stones", 0);
		currPlayer.set("green_stones", 0);
		currPlayer.set("blue_stones", 0);
		currPlayer.set("ancient_adena_amount", 0);
		
		if (removeReward)
		{
			_signsPlayerData.put(objectId, currPlayer);
			if (!Config.ALT_SEVENSIGNS_LAZY_UPDATE)
			{
				saveSevenSignsData(objectId);
				saveSevenSignsStatus();
			}
		}
		
		return rewardAmount;
	}
	
	public long addPlayerStoneContrib(int objectId, long blueCount, long greenCount, long redCount)
	{
		final var currPlayer = _signsPlayerData.get(objectId);
		
		final var contribScore = calcContributionScore(blueCount, greenCount, redCount);
		final var totalAncientAdena = currPlayer.getLong("ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
		final var totalContribScore = currPlayer.getLong("contribution_score") + contribScore;
		
		if (totalContribScore > Config.ALT_MAXIMUM_PLAYER_CONTRIB)
		{
			return -1;
		}
		
		currPlayer.set("red_stones", currPlayer.getInteger("red_stones") + redCount);
		currPlayer.set("green_stones", currPlayer.getInteger("green_stones") + greenCount);
		currPlayer.set("blue_stones", currPlayer.getInteger("blue_stones") + blueCount);
		currPlayer.set("ancient_adena_amount", totalAncientAdena);
		currPlayer.set("contribution_score", totalContribScore);
		_signsPlayerData.put(objectId, currPlayer);
		
		switch (getPlayerCabal(objectId))
		{
			case CABAL_DAWN :
				_dawnStoneScore += contribScore;
				break;
			case CABAL_DUSK :
				_duskStoneScore += contribScore;
				break;
		}
		
		if (!Config.ALT_SEVENSIGNS_LAZY_UPDATE)
		{
			saveSevenSignsData(objectId);
			saveSevenSignsStatus();
		}
		
		return contribScore;
	}
	
	public void addFestivalScore(int cabal, int amount)
	{
		if (cabal == CABAL_DUSK)
		{
			_duskFestivalScore += amount;
		}
		else
		{
			_dawnFestivalScore += amount;
		}
	}
	
	public void delFestivalScore(int cabal, int amount)
	{
		if (cabal == CABAL_DUSK)
		{
			_duskFestivalScore -= amount;
		}
		else
		{
			_dawnFestivalScore -= amount;
		}
	}
	
	public void sendCurrentPeriodMsg(Player player)
	{
		SystemMessage sm = null;
		
		switch (getCurrentPeriod())
		{
			case PERIOD_COMP_RECRUITING :
				sm = SystemMessage.getSystemMessage(SystemMessageId.PREPARATIONS_PERIOD_BEGUN);
				break;
			case PERIOD_COMPETITION :
				sm = SystemMessage.getSystemMessage(SystemMessageId.COMPETITION_PERIOD_BEGUN);
				break;
			case PERIOD_COMP_RESULTS :
				sm = SystemMessage.getSystemMessage(SystemMessageId.RESULTS_PERIOD_BEGUN);
				break;
			case PERIOD_SEAL_VALIDATION :
				sm = SystemMessage.getSystemMessage(SystemMessageId.VALIDATION_PERIOD_BEGUN);
				break;
		}
		
		player.sendPacket(sm);
	}
	
	public void sendMessageToAll(SystemMessageId sysMsgId)
	{
		final var sm = SystemMessage.getSystemMessage(sysMsgId);
		GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(sm));
	}
	
	protected void initializeSeals()
	{
		for (final Entry<Integer, Integer> e : _signsSealOwners.entrySet())
		{
			if (e.getValue() != CABAL_NULL)
			{
				if (isSealValidationPeriod())
				{
					info("The " + getCabalName(e.getValue()) + " have won the " + getSealName(e.getKey(), false) + ".");
				}
				else
				{
					info("The " + getSealName(e.getKey(), false) + " is currently owned by " + getCabalName(e.getValue()) + ".");
				}
			}
			else
			{
				info("The " + getSealName(e.getKey(), false) + " remains unclaimed.");
			}
		}
	}
	
	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}
	
	protected void calcNewSealOwners()
	{
		for (final var currSeal : _signsDawnSealTotals.keySet())
		{
			final var prevSealOwner = _signsSealOwners.get(currSeal);
			var newSealOwner = CABAL_NULL;
			final var dawnProportion = getSealProportion(currSeal, CABAL_DAWN);
			final var totalDawnMembers = getTotalMembers(CABAL_DAWN) == 0 ? 1 : getTotalMembers(CABAL_DAWN);
			final var dawnPercent = Math.round(((float) dawnProportion / (float) totalDawnMembers) * 100);
			final var duskProportion = getSealProportion(currSeal, CABAL_DUSK);
			final var totalDuskMembers = getTotalMembers(CABAL_DUSK) == 0 ? 1 : getTotalMembers(CABAL_DUSK);
			final var duskPercent = Math.round(((float) duskProportion / (float) totalDuskMembers) * 100);
			
			switch (prevSealOwner)
			{
				case CABAL_NULL :
					switch (getCabalHighestScore())
					{
						case CABAL_NULL :
							newSealOwner = CABAL_NULL;
							break;
						case CABAL_DAWN :
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK :
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DAWN :
					switch (getCabalHighestScore())
					{
						case CABAL_NULL :
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN :
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK :
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DUSK :
					switch (getCabalHighestScore())
					{
						case CABAL_NULL :
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN :
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK :
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
			}
			
			_signsSealOwners.put(currSeal, newSealOwner);
			
			switch (currSeal)
			{
				case SEAL_AVARICE :
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_AVARICE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_AVARICE);
					}
					break;
				case SEAL_GNOSIS :
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_GNOSIS);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_GNOSIS);
					}
					break;
				case SEAL_STRIFE :
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_STRIFE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_STRIFE);
					}
					
					CastleManager.getInstance().validateTaxes(newSealOwner);
					break;
			}
		}
	}
	
	protected void teleLosingCabalFromDungeons(String compWinner)
	{
		for (final var player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				final var currPlayer = _signsPlayerData.get(player.getObjectId());
				
				if (isSealValidationPeriod() || isCompResultsPeriod())
				{
					if (!player.isGM() && player.isIn7sDungeon() && ((currPlayer == null) || !currPlayer.getString("cabal").equals(compWinner)))
					{
						player.teleToLocation(TeleportWhereType.TOWN, true, player.getReflection());
						player.setIsIn7sDungeon(false);
						player.sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
					}
				}
				else
				{
					if (!player.isGM() && player.isIn7sDungeon() && ((currPlayer == null) || !currPlayer.getString("cabal").isEmpty()))
					{
						player.teleToLocation(TeleportWhereType.TOWN, true, player.getReflection());
						player.setIsIn7sDungeon(false);
						player.sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
					}
				}
			}
		}
	}
	
	protected class SevenSignsPeriodChange implements Runnable
	{
		@Override
		public void run()
		{
			final var periodEnded = getCurrentPeriod();
			_activePeriod++;
			
			switch (periodEnded)
			{
				case PERIOD_COMP_RECRUITING :
					SevenSignsFestival.getInstance().startFestivalManager();
					sendMessageToAll(SystemMessageId.QUEST_EVENT_PERIOD_BEGUN);
					RaidBossSpawnManager.getInstance().distributeRewards();
					break;
				case PERIOD_COMPETITION :
					sendMessageToAll(SystemMessageId.QUEST_EVENT_PERIOD_ENDED);
					final var compWinner = getCabalHighestScore();
					SevenSignsFestival.getInstance().getFestivalManagerSchedule().cancel(false);
					SevenSignsFestival.getInstance().rewardHighestRanked();
					
					calcNewSealOwners();
					
					switch (compWinner)
					{
						case CABAL_DAWN :
							sendMessageToAll(SystemMessageId.DAWN_WON);
							break;
						case CABAL_DUSK :
							sendMessageToAll(SystemMessageId.DUSK_WON);
							break;
					}
					
					_previousWinner = compWinner;
					final List<Castle> castles = CastleManager.getInstance().getCastles();
					for (final Castle castle : castles)
					{
						castle.setTicketBuyCount(0);
					}
					break;
				case PERIOD_COMP_RESULTS :
					initializeSeals();
					giveCPMult(getSealOwner(SEAL_STRIFE));
					sendMessageToAll(SystemMessageId.SEAL_VALIDATION_PERIOD_BEGUN);
					RaidBossSpawnManager.getInstance().distributeRewards();
					final Quest twQuest = QuestManager.getInstance().getQuest(TerritoryWarManager.qn);
					if (twQuest != null)
					{
						twQuest.startQuestTimer("setNextTWDate", 30000, null, null);
					}
					
					info("The " + getCabalName(_previousWinner) + " have won the competition with " + getCurrentScore(_previousWinner) + " points!");
					break;
				case PERIOD_SEAL_VALIDATION :
					_activePeriod = PERIOD_COMP_RECRUITING;
					sendMessageToAll(SystemMessageId.SEAL_VALIDATION_PERIOD_ENDED);
					removeCPMult();
					resetPlayerData();
					resetSeals();
					
					_currentCycle++;
					SevenSignsFestival.getInstance().resetFestivalData(false);
					
					_dawnStoneScore = 0;
					_duskStoneScore = 0;
					
					_dawnFestivalScore = 0;
					_duskFestivalScore = 0;
					break;
			}
			
			saveSevenSignsData();
			saveSevenSignsStatus();

			info("Change Catacomb spawn...");
			changePeriodSpawn();
			
			teleLosingCabalFromDungeons(getCabalShortName(getCabalHighestScore()));
			
			final var ss = new SSQInfo();
			GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(ss));
			spawnSevenSignsNPC();
			
			info("The " + getCurrentPeriodName() + " period has begun!");
			
			setCalendarForNextPeriodChange();
			
			final List<Castle> castles = CastleManager.getInstance().getCastles();
			for (final var castle : castles)
			{
				castle.getSiege().correctSiegeDateTime();
			}
			
			final var sspc = new SevenSignsPeriodChange();
			ThreadPoolManager.getInstance().schedule(sspc, getMilliToPeriodChange());
		}
	}
	
	public boolean checkIsDawnPostingTicket(int itemId)
	{
		if ((itemId > 6114) && (itemId < 6175))
		{
			return true;
		}
		if ((itemId > 6801) && (itemId < 6812))
		{
			return true;
		}
		if ((itemId > 7997) && (itemId < 8008))
		{
			return true;
		}
		if ((itemId > 7940) && (itemId < 7951))
		{
			return true;
		}
		if ((itemId > 6294) && (itemId < 6307))
		{
			return true;
		}
		if ((itemId > 6831) && (itemId < 6834))
		{
			return true;
		}
		if ((itemId > 8027) && (itemId < 8030))
		{
			return true;
		}
		if ((itemId > 7970) && (itemId < 7973))
		{
			return true;
		}
		return false;
	}
	
	public boolean checkIsRookiePostingTicket(int itemId)
	{
		if ((itemId > 6174) && (itemId < 6295))
		{
			return true;
		}
		if ((itemId > 6811) && (itemId < 6832))
		{
			return true;
		}
		if ((itemId > 7950) && (itemId < 7971))
		{
			return true;
		}
		if ((itemId > 8007) && (itemId < 8028))
		{
			return true;
		}
		return false;
	}
	
	public void giveCPMult(int strifeOwner)
	{
		for (final var player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				final var cabal = getPlayerCabal(player.getObjectId());
				if (cabal != SevenSigns.CABAL_NULL)
				{
					if (cabal == strifeOwner)
					{
						player.addSkill(SkillsParser.FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
					}
					else
					{
						player.addSkill(SkillsParser.FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill());
					}
				}
			}
		}
	}
	
	public void removeCPMult()
	{
		for (final var player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				player.removeSkill(SkillsParser.FrequentSkill.THE_VICTOR_OF_WAR.getSkill());
				player.removeSkill(SkillsParser.FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill());
			}
		}
	}
	
	public boolean checkSummonConditions(Player activeChar)
	{
		if (activeChar == null)
		{
			return true;
		}

		if (isSealValidationPeriod())
		{
			if (getSealOwner(SEAL_STRIFE) == CABAL_DAWN)
			{
				if (getPlayerCabal(activeChar.getObjectId()) == CABAL_DUSK)
				{
					activeChar.sendMessage("You cannot summon Siege Golem or Cannon while Seal of Strife posessed by Lords of Dawn.");
					return true;
				}
			}
		}
		return false;
	}
	
	private static class SingletonHolder
	{
		protected static final SevenSigns _instance = new SevenSigns();
	}
}