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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.entity.underground_coliseum.UCArena;
import l2e.gameserver.model.entity.underground_coliseum.UCBestTeam;
import l2e.gameserver.model.entity.underground_coliseum.UCPoint;
import l2e.gameserver.model.entity.underground_coliseum.UCReward;
import l2e.gameserver.model.entity.underground_coliseum.UCTeam;
import l2e.gameserver.model.strings.server.ServerMessage;

public class UndergroundColiseumManager extends LoggerObject
{
	private final Map<Integer, UCArena> _arenas = new HashMap<>(5);
	private boolean _isStarted = false;
	private long _periodStartTime;
	private long _periodEndTime;
	private ScheduledFuture<?> _regTask = null;
	private final Map<Integer, UCBestTeam> _bestTeams = new HashMap<>(5);
	
	public static UndergroundColiseumManager getInstance()
	{
		return SingletonHolder._instance;
	}

	protected UndergroundColiseumManager()
	{
		_periodStartTime = ServerVariables.getLong("UC_START_TIME", 0);
		_periodEndTime = ServerVariables.getLong("UC_STOP_TIME", 0);
		
		if ((_periodStartTime < System.currentTimeMillis()) && (_periodEndTime < System.currentTimeMillis()))
		{
			generateNewDate();
		}
		
		load();

		info("Loaded " + _arenas.size() + " coliseum arenas.");
		
		if ((_periodStartTime < System.currentTimeMillis()) && (_periodEndTime > System.currentTimeMillis()))
		{
			switchStatus(true);
		}
		else
		{
			final long nextTime = _periodStartTime - System.currentTimeMillis();
			_regTask = ThreadPoolManager.getInstance().schedule(new UCRegistrationTask(true), nextTime);
			info("Battles will begin at: " + new Date(_periodStartTime));
		}
		
		restoreBestTeams();
	}
	
	private void restoreBestTeams()
	{
		_bestTeams.clear();
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); var statement = con.prepareStatement("SELECT * FROM underground_colliseum_stats ORDER BY arenaId"); var rset = statement.executeQuery())
		{
			while (rset.next())
			{
				final int arenaId = rset.getInt("arenaId");
				final String leader = rset.getString("leader");
				final int wins = rset.getInt("wins");
				_bestTeams.put(arenaId, new UCBestTeam(arenaId, leader, wins));
			}
		}
		catch (final SQLException e)
		{
			warn("Couldnt load underground_colliseum_stats table");
		}
		catch (final Exception e)
		{
			warn("Error while initializing UndergroundColiseumManager: " + e.getMessage(), e);
		}
	}
	
	private void saveBestTeam(UCBestTeam team, boolean isNew)
	{
		if (isNew)
		{
			try (
			    var con = DatabaseFactory.getInstance().getConnection(); PreparedStatement ps = con.prepareStatement("INSERT INTO underground_colliseum_stats (`arenaId`, `leader`, `wins`) VALUES (?,?,?) "))
			{
				ps.setInt(1, team.getArenaId());
				ps.setString(2, team.getLeaderName());
				ps.setInt(3, team.getWins());
				ps.executeUpdate();
			}
			catch (final SQLException e)
			{
				warn("Could not save underground_colliseum_stats: " + e.getMessage());
			}
		}
		else
		{
			try (
			    var con = DatabaseFactory.getInstance().getConnection())
			{
				final PreparedStatement stmt = con.prepareStatement("UPDATE underground_colliseum_stats SET leader = ?, wins = ?  WHERE arenaId = ?");
				stmt.setInt(1, team.getArenaId());
				stmt.setInt(2, team.getWins());
				stmt.setInt(3, team.getArenaId());
				stmt.execute();
				stmt.close();
			}
			catch (final Exception e)
			{
				warn("could not clean status for underground_colliseum_stats areanaId: " + team.getArenaId() + " in database!");
			}
		}
	}
	
	public UCBestTeam getBestTeam(int arenaId)
	{
		return _bestTeams.get(arenaId);
	}
	
	public void updateBestTeam(int arenaId, String name, int wins)
	{
		if (_bestTeams.containsKey(arenaId))
		{
			final UCBestTeam team = getBestTeam(arenaId);
			if (team != null)
			{
				team.setLeader(name);
				team.setWins(wins);
				saveBestTeam(team, false);
			}
		}
		else
		{
			final UCBestTeam team = new UCBestTeam(arenaId, name, wins);
			_bestTeams.put(arenaId, team);
			saveBestTeam(team, true);
		}
	}
	
	private void generateNewDate()
	{
		final SchedulingPattern timePattern = new SchedulingPattern(Config.UC_START_TIME);
		_periodStartTime = timePattern.next(System.currentTimeMillis());
		_periodEndTime = _periodStartTime + (Config.UC_TIME_PERIOD * 3600000);
		ServerVariables.set("UC_START_TIME", _periodStartTime);
		ServerVariables.set("UC_STOP_TIME", _periodEndTime);
	}

	private void load()
	{
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/spawnZones/underground_coliseum.xml");
			if (!file.exists())
			{
				info("The underground_coliseum.xml file is missing.");
				return;
			}
			final Document doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap map;
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("arena".equalsIgnoreCase(d.getNodeName()))
						{
							map = d.getAttributes();
							final int id = Integer.parseInt(map.getNamedItem("id").getNodeValue());
							final int min_level = Integer.parseInt(map.getNamedItem("minLvl").getNodeValue());
							final int max_level = Integer.parseInt(map.getNamedItem("maxLvl").getNodeValue());
							final int curator = Integer.parseInt(map.getNamedItem("curator").getNodeValue());

							final UCArena arena = new UCArena(id, curator, min_level, max_level);
							int index = 0;
							int index2 = 0;

							for (Node und = d.getFirstChild(); und != null; und = und.getNextSibling())
							{
								if ("tower".equalsIgnoreCase(und.getNodeName()))
								{
									map = und.getAttributes();

									final int npcId = Integer.parseInt(map.getNamedItem("id").getNodeValue());
									final int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
									final int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
									final int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());

									final UCTeam team = new UCTeam(index, arena, x, y, z, npcId);

									arena.setUCTeam(index, team);

									index++;
								}
								else if ("spawn".equalsIgnoreCase(und.getNodeName()))
								{
									map = und.getAttributes();
									final List<DoorInstance> doors = new ArrayList<>();
									final String doorList = map.getNamedItem("doors") != null ? map.getNamedItem("doors").getNodeValue() : "";
									if (!doorList.isEmpty())
									{
										final String[] doorSplint = doorList.split(",");
										for (final String doorId : doorSplint)
										{
											final DoorInstance door = DoorParser.getInstance().getDoor(Integer.parseInt(doorId));
											if (door != null)
											{
												doors.add(door);
											}
										}
									}
									final int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
									final int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
									final int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());

									final UCPoint point = new UCPoint(doors, new Location(x, y, z));
									arena.setUCPoint(index2, point);

									index2++;
								}
								else if ("rewards".equalsIgnoreCase(und.getNodeName()))
								{
									for (Node c = und.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("item".equalsIgnoreCase(c.getNodeName()))
										{
											final int itemId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
											final long amount = Long.parseLong(c.getAttributes().getNamedItem("amount").getNodeValue());
											final boolean useModifier = c.getAttributes().getNamedItem("useModifers") != null ? Boolean.parseBoolean(c.getAttributes().getNamedItem("useModifers").getNodeValue()) : false;
											arena.setReward(new UCReward(itemId, amount, useModifier));
										}
									}
								}
							}
							_arenas.put(arena.getId(), arena);
						}
					}
				}
			}
		}
		catch (
		    NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("underground_coliseum.xml could not be initialized.", e);
		}
		catch (
		    IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}

	public UCArena getArena(int id)
	{
		return _arenas.get(id);
	}

	public void setStarted(boolean started)
	{
		_isStarted = started;
		for (final UCArena arena : getAllArenas())
		{
			arena.switchStatus(started);
		}
		
		if (Config.UC_ANNOUNCE_BATTLES)
		{
			if (_isStarted)
			{
				final ServerMessage msg = new ServerMessage("UC.STARTED", true);
				Announcements.getInstance().announceToAll(msg);
			}
			else
			{
				final ServerMessage msg = new ServerMessage("UC.STOPED", true);
				Announcements.getInstance().announceToAll(msg);
			}
		}
	}

	public boolean isStarted()
	{
		return _isStarted;
	}

	public Collection<UCArena> getAllArenas()
	{
		return _arenas.values();
	}
	
	private void switchStatus(boolean isStart)
	{
		if (_regTask != null)
		{
			_regTask.cancel(false);
			_regTask = null;
		}
		
		setStarted(isStart);
		if (isStart)
		{
			final long nextTime = _periodEndTime - System.currentTimeMillis();
			_regTask = ThreadPoolManager.getInstance().schedule(new UCRegistrationTask(false), nextTime);
			info("Battles will end at: " + new Date(_periodEndTime));
		}
		else
		{
			generateNewDate();
			final long nextTime = _periodStartTime - System.currentTimeMillis();
			_regTask = ThreadPoolManager.getInstance().schedule(new UCRegistrationTask(true), nextTime);
			info("Battles will begin at: " + new Date(_periodStartTime));
		}
	}

	public class UCRegistrationTask implements Runnable
	{
		private final boolean _status;

		public UCRegistrationTask(boolean status)
		{
			_status = status;
		}

		@Override
		public void run()
		{
			switchStatus(_status);
		}
	}

	private static class SingletonHolder
	{
		protected static final UndergroundColiseumManager _instance = new UndergroundColiseumManager();
	}
}