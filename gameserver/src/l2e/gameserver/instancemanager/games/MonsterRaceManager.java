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
package l2e.gameserver.instancemanager.games;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.templates.HistoryInfoTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.DeleteObject;
import l2e.gameserver.network.serverpackets.MonRaceInfo;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class MonsterRaceManager extends LoggerObject
{
	public static enum RaceState
	{
		ACCEPTING_BETS,
		WAITING,
		STARTING_RACE,
		RACE_END
	}
	
	private static final PlaySound SOUND_1 = new PlaySound(1, "S_Race");
	private static final PlaySound SOUND_2 = new PlaySound("ItemSound2.race_start");
	
	private static final int[][] CODES =
	{
		{
			-1,
			0
		},
		{
			0,
			15322
		},
		{
			13765,
			-1
		}
	};
	
	private final List<Integer> _npcTemplates = new ArrayList<>();
	private final List<HistoryInfoTemplate> _history = new ArrayList<>();
	private final Map<Integer, Long> _betsPerLane = new ConcurrentHashMap<>();
	private final List<Double> _odds = new ArrayList<>();
	private Npc _manager = null;
	
	private int _raceNumber = 1;
	private int _finalCountdown = 0;
	private RaceState _state = RaceState.RACE_END;
	
	private MonRaceInfo _packet;
	
	private final Npc[] _monsters;
	private Constructor<?> _constructor;
	private int[][] _speeds;
	private final int[] _first, _second;
	
	public MonsterRaceManager()
	{
		loadHistory();
		loadBets();
		
		for (int i = 31003; i < 31027; i++)
		{
			_npcTemplates.add(i);
		}
		
		_monsters = new Npc[8];
		_speeds = new int[8][20];
		_first = new int[2];
		_second = new int[2];
		
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn != null && spawn.getId() == 30995)
			{
				_manager = spawn.getLastSpawn();
			}
		}
		
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new Announcement(), 0, 1000);
	}
	
	public Npc[] getMonsters()
	{
		return _monsters;
	}
	
	public int[][] getSpeeds()
	{
		return _speeds;
	}
	
	public int getFirstPlace()
	{
		return _first[0];
	}
	
	public int getSecondPlace()
	{
		return _second[0];
	}
	
	public MonRaceInfo getRacePacket()
	{
		return _packet;
	}
	
	public RaceState getCurrentRaceState()
	{
		return _state;
	}
	
	public int getRaceNumber()
	{
		return _raceNumber;
	}
	
	public List<HistoryInfoTemplate> getHistory()
	{
		return _history;
	}
	
	public List<Double> getOdds()
	{
		return _odds;
	}
	
	public void newRace()
	{
		_history.add(new HistoryInfoTemplate(_raceNumber, 0, 0, 0));
		Collections.shuffle(_npcTemplates);
		for (int i = 0; i < 8; i++)
		{
			try
			{
				final NpcTemplate template = NpcsParser.getInstance().getTemplate(_npcTemplates.get(i));
				_constructor = Class.forName("l2e.gameserver.model.actor.instance." + template.getType() + "Instance").getConstructors()[0];
				_monsters[i] = (Npc) _constructor.newInstance(IdFactory.getInstance().getNextId(), template);
			}
			catch (final Exception e)
			{
				warn("Failed generating MonsterRace monster.", e);
			}
		}
	}
	
	public void newSpeeds()
	{
		_speeds = new int[8][20];
		int total = 0;
		_first[1] = 0;
		_second[1] = 0;
		
		for (int i = 0; i < 8; i++)
		{
			total = 0;
			for (int j = 0; j < 20; j++)
			{
				if (j == 19)
				{
					_speeds[i][j] = 100;
				}
				else
				{
					_speeds[i][j] = Rnd.get(60) + 65;
				}
				total += _speeds[i][j];
			}
			
			if (total >= _first[1])
			{
				_second[0] = _first[0];
				_second[1] = _first[1];
				_first[0] = 8 - i;
				_first[1] = total;
			}
			else if (total >= _second[1])
			{
				_second[0] = 8 - i;
				_second[1] = total;
			}
		}
	}
	
	private void loadHistory()
	{
		Connection con = null;
		final PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			rs = con.prepareStatement("SELECT * FROM monster_race_history").executeQuery();
			while (rs.next())
			{
				_history.add(new HistoryInfoTemplate(rs.getInt("race_id"), rs.getInt("first"), rs.getInt("second"), rs.getDouble("odd_rate")));
				_raceNumber++;
			}
		}
		catch (final SQLException e)
		{
			warn("Can't load Monster Race history.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
		info("Loaded " + _history.size() + " Monster Race records, currently on race #" + _raceNumber);
	}
	
	private void saveHistory(HistoryInfoTemplate history)
	{
		Connection con = null;
		PreparedStatement ps = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement("REPLACE INTO monster_race_history (race_id, first, second, odd_rate) VALUES (?,?,?,?)");
			ps.setInt(1, history.getRaceId());
			ps.setInt(2, history.getFirst());
			ps.setInt(3, history.getSecond());
			ps.setDouble(4, history.getOddRate());
			ps.execute();
		}
		catch (final SQLException e)
		{
			warn("Can't save Monster Race history.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, ps);
		}
	}
	
	private void loadBets()
	{
		Connection con = null;
		final PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			rs = con.prepareStatement("SELECT * FROM monster_race_bets").executeQuery();
			while (rs.next())
			{
				setBetOnLane(rs.getInt("lane_id"), rs.getLong("bet"), false);
			}
		}
		catch (final SQLException e)
		{
			warn("Can't load Monster Race bets.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}
	
	private void saveBet(int lane, long sum)
	{
		Connection con = null;
		PreparedStatement ps = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement("REPLACE INTO monster_race_bets (lane_id, bet) VALUES (?,?)");
			ps.setInt(1, lane);
			ps.setLong(2, sum);
			ps.execute();
		}
		catch (final SQLException e)
		{
			warn("Can't save Monster Race bet.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, ps);
		}
	}
	
	private void clearBets()
	{
		for (final int key : _betsPerLane.keySet())
		{
			_betsPerLane.put(key, 0L);
		}
		
		Connection con = null;
		PreparedStatement ps = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement("UPDATE monster_race_bets SET bet = 0");
			ps.execute();
		}
		catch (final SQLException e)
		{
			warn("Can't clear Monster Race bets.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, ps);
		}
	}
	
	public void setBetOnLane(int lane, long amount, boolean saveOnDb)
	{
		final long sum = _betsPerLane.getOrDefault(lane, 0L) + amount;
		_betsPerLane.put(lane, sum);
		if (saveOnDb)
		{
			saveBet(lane, sum);
		}
	}
	
	private void calculateOdds()
	{
		_odds.clear();
		final Map<Integer, Long> sortedLanes = new TreeMap<>(_betsPerLane);
		
		long sumOfAllLanes = 0;
		for (final long amount : sortedLanes.values())
		{
			sumOfAllLanes += amount;
		}
		
		for (final long amount : sortedLanes.values())
		{
			_odds.add((amount == 0) ? 0D : Math.max(1.25, sumOfAllLanes * 0.7 / amount));
		}
	}
	
	private class Announcement implements Runnable
	{
		@Override
		public void run()
		{
			if (_manager == null)
			{
				return;
			}
			
			if (_finalCountdown > 1200)
			{
				_finalCountdown = 0;
			}
			
			switch (_finalCountdown)
			{
				case 0:
					newRace();
					newSpeeds();
					_state = RaceState.ACCEPTING_BETS;
					_packet = new MonRaceInfo(CODES[0][0], CODES[0][1], getMonsters(), getSpeeds());
					_manager.broadcastPacketToOthers(2000, _packet, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber));
					break;
				case 30 :
				case 60 :
				case 90 :
				case 120 :
				case 150 :
				case 180 :
				case 210 :
				case 240 :
				case 270 :
				case 330 :
				case 360 :
				case 390 :
				case 420 :
				case 450 :
				case 480 :
				case 510 :
				case 540 :
				case 570 :
				case 630 :
				case 660 :
				case 690 :
				case 720 :
				case 750 :
				case 780 :
				case 810 :
				case 870 :
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber));
					break;
				case 300 :
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_STOP_IN_S1_MINUTES).addNumber(10));
					break;
				case 600 :
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_STOP_IN_S1_MINUTES).addNumber(5));
					break;
				case 840 :
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_STOP_IN_S1_MINUTES).addNumber(1));
					break;
				case 900 :
					_state = RaceState.WAITING;
					calculateOdds();
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S1_TICKET_SALES_CLOSED));
					break;
				case 960 :
				case 1020 :
					final int minutes = (_finalCountdown == 960) ? 2 : 1;
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S2_BEGINS_IN_S1_MINUTES).addNumber(minutes));
					break;
				case 1050 :
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S1_BEGINS_IN_30_SECONDS));
					break;
				case 1070 :
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S1_COUNTDOWN_IN_FIVE_SECONDS));
					break;
				case 1075 :
				case 1076 :
				case 1077 :
				case 1078 :
				case 1079 :
					final int seconds = 1080 - _finalCountdown;
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_BEGINS_IN_S1_SECONDS).addNumber(seconds));
					break;
				case 1080 :
					_state = RaceState.STARTING_RACE;
					_packet = new MonRaceInfo(CODES[1][0], CODES[1][1], getMonsters(), getSpeeds());
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_RACE_START), SOUND_1, SOUND_2, _packet);
					break;
				case 1085 :
					_packet = new MonRaceInfo(CODES[2][0], CODES[2][1], getMonsters(), getSpeeds());
					_manager.broadcastPacketToOthers(2000, _packet);
					break;
				case 1115 :
					_state = RaceState.RACE_END;
					final HistoryInfoTemplate info = _history.get(_history.size() - 1);
					info.setFirst(getFirstPlace());
					info.setSecond(getSecondPlace());
					info.setOddRate(_odds.get(getFirstPlace() - 1));
					saveHistory(info);
					clearBets();
					_manager.broadcastPacketToOthers(2000, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_FIRST_PLACE_S1_SECOND_S2).addNumber(getFirstPlace()).addNumber(getSecondPlace()), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S1_RACE_END).addNumber(_raceNumber));
					_raceNumber++;
					break;
				case 1140 :
					_manager.broadcastPacketToOthers(4000, new DeleteObject(getMonsters()[0]), new DeleteObject(getMonsters()[1]), new DeleteObject(getMonsters()[2]), new DeleteObject(getMonsters()[3]), new DeleteObject(getMonsters()[4]), new DeleteObject(getMonsters()[5]), new DeleteObject(getMonsters()[6]), new DeleteObject(getMonsters()[7]));
					break;
			}
			_finalCountdown += 1;
		}
	}
	
	public static MonsterRaceManager getInstance()
	{
		return SingletonHolder.instance;
	}
	
	private static class SingletonHolder
	{
		protected static final MonsterRaceManager instance = new MonsterRaceManager();
	}
}