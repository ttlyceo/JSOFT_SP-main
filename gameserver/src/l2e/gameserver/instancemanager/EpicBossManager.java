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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.log.LoggerObject;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.templates.npc.AnnounceTemplate;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.type.BossZone;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.taskmanager.RaidBossTaskManager;

public class EpicBossManager extends LoggerObject
{
	private static final String DELETE_GRAND_BOSS_LIST = "DELETE FROM grandboss_list";
	private static final String INSERT_GRAND_BOSS_LIST = "INSERT INTO grandboss_list (player_id,zone) VALUES (?,?)";
	private static final String UPDATE_GRAND_BOSS_DATA = "UPDATE grandboss_data set loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, respawn_time = ?, currentHP = ?, currentMP = ?, status = ? where boss_id = ?";
	private static final String UPDATE_GRAND_BOSS_DATA2 = "UPDATE grandboss_data set status = ?, respawn_time = ? where boss_id = ?";
	
	private final Map<Integer, GrandBossInstance> _bosses = new ConcurrentHashMap<>();
	private final Map<Integer, StatsSet> _storedInfo = new HashMap<>();
	private final Map<Integer, Integer> _bossStatus = new ConcurrentHashMap<>();
	private final Map<Integer, BossZone> _zones = new ConcurrentHashMap<>();

	public static EpicBossManager getInstance()
	{
		return SingletonHolder._instance;
	}

	protected EpicBossManager()
	{
		init();
	}

	private void init()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); Statement s = con.createStatement(); var rset = s.executeQuery("SELECT * from grandboss_data ORDER BY boss_id"))
		{
			while (rset.next())
			{
				final StatsSet info = new StatsSet();
				final int bossId = rset.getInt("boss_id");
				info.set("loc_x", rset.getInt("loc_x"));
				info.set("loc_y", rset.getInt("loc_y"));
				info.set("loc_z", rset.getInt("loc_z"));
				info.set("heading", rset.getInt("heading"));
				info.set("respawnTime", rset.getLong("respawn_time"));
				final double HP = rset.getDouble("currentHP");
				final int true_HP = (int) HP;
				info.set("currentHP", true_HP);
				final double MP = rset.getDouble("currentMP");
				final int true_MP = (int) MP;
				info.set("currentMP", true_MP);
				final int status = rset.getInt("status");
				_bossStatus.put(bossId, status);
				_storedInfo.put(bossId, info);
				
				String checkStatus;
				switch (status)
				{
					case 1 :
						checkStatus = "Wait";
						break;
					case 2 :
						checkStatus = "Fight";
						break;
					case 3 :
						checkStatus = "Dead";
						break;
					default :
						checkStatus = "Alive";
						break;
				}
				
				if (status > 0)
				{
					info("" + NpcsParser.getInstance().getTemplate(bossId).getName(null) + "[" + bossId + "] respawn date [" + new Date(info.getLong("respawnTime")) + "].");
					if (Config.EPICBOSS_PRE_ANNOUNCE_LIST.containsKey(bossId))
					{
						final long delay = (Config.EPICBOSS_PRE_ANNOUNCE_LIST.get(bossId) * 60000L);
						final long announceTime = rset.getLong("respawn_time") - delay;
						if (announceTime > 0)
						{
							RaidBossTaskManager.getInstance().addToAnnounceList(new AnnounceTemplate(NpcsParser.getInstance().getTemplate(bossId), delay), announceTime, true);
						}
					}
				}
				else
				{
					info("" + NpcsParser.getInstance().getTemplate(bossId).getName(null) + "[" + bossId + "] status [" + checkStatus + "].");
				}
			}
		}
		catch (final SQLException e)
		{
			warn("Could not load grandboss_data table: " + e.getMessage(), e);
		}
		catch (final Exception e)
		{
			warn("Error while initializing EpicBossManager: " + e.getMessage(), e);
		}
	}

	public void initZones()
	{
		final Map<Integer, List<Integer>> zones = new HashMap<>();
		for (final Integer zoneId : _zones.keySet())
		{
			zones.put(zoneId, new ArrayList<>());
		}

		try (
		    var con = DatabaseFactory.getInstance().getConnection(); Statement s = con.createStatement(); var rset = s.executeQuery("SELECT * from grandboss_list ORDER BY player_id"))
		{
			while (rset.next())
			{
				final int id = rset.getInt("player_id");
				final int zone_id = rset.getInt("zone");
				zones.get(zone_id).add(id);
			}

			if (Config.DEBUG)
			{
				info("Initialized " + _zones.size() + " Grand Boss Zones");
			}
		}
		catch (final SQLException e)
		{
			warn("Could not load grandboss_list table: " + e.getMessage(), e);
		}
		catch (final Exception e)
		{
			warn("Error while initializing EpicBoss zones: " + e.getMessage(), e);
		}

		for (final Entry<Integer, BossZone> e : _zones.entrySet())
		{
			e.getValue().setAllowedPlayers(zones.get(e.getKey()));
		}
		zones.clear();
	}

	public void addZone(BossZone zone)
	{
		_zones.put(zone.getId(), zone);
	}

	public final BossZone getZone(int zoneId)
	{
		return _zones.get(zoneId);
	}

	public final BossZone getZone(Creature character)
	{
		return _zones.values().stream().filter(z -> z.isCharacterInZone(character)).findFirst().orElse(null);
	}

	public final BossZone getZone(Location loc)
	{
		return getZone(loc.getX(), loc.getY(), loc.getZ());
	}

	public final BossZone getZone(int x, int y, int z)
	{
		return _zones.values().stream().filter(zone -> zone.isInsideZone(x, y, z)).findFirst().orElse(null);
	}

	public boolean checkIfInZone(String zoneType, GameObject obj)
	{
		final BossZone temp = getZone(obj.getX(), obj.getY(), obj.getZ());
		return (temp != null) && temp.getName().equalsIgnoreCase(zoneType);
	}

	public boolean checkIfInZone(Player player)
	{
		return (player != null) && (getZone(player.getX(), player.getY(), player.getZ()) != null);
	}

	public int getBossStatus(int bossId)
	{
		return _bossStatus.get(bossId);
	}

	public void setBossStatus(int bossId, int status, boolean print)
	{
		if (status == 0)
		{
			if (Arrays.binarySearch(Config.GRANDBOSS_ANNOUNCE_LIST, bossId) >= 0)
			{
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					if (player.isOnline())
					{
						final ServerMessage msg = new ServerMessage("Announce.RAID_RESPAWN", player.getLang());
						msg.add(NpcsParser.getInstance().getTemplate(bossId).getName(player.getLang()));
						player.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, "", msg.toString()));
					}
				}
			}
			RaidBossTaskManager.getInstance().removeAnnounce(bossId);
		}
		
		_bossStatus.put(bossId, status);
		if (print)
		{
			info("Updated " + NpcsParser.getInstance().getTemplate(bossId).getName(null) + "(" + bossId + ") status to " + status);
		}
		updateDb(bossId, true);
	}

	public void addBoss(GrandBossInstance boss)
	{
		_bosses.put(boss.getId(), boss);
	}

	public GrandBossInstance getBoss(int bossId)
	{
		return _bosses.get(bossId);
	}

	public StatsSet getStatsSet(int bossId)
	{
		return _storedInfo.get(bossId);
	}

	public void setStatsSet(int bossId, StatsSet info)
	{
		_storedInfo.put(bossId, info);
		updateDb(bossId, false);
	}

	private void storeToDb()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); PreparedStatement delete = con.prepareStatement(DELETE_GRAND_BOSS_LIST))
		{
			delete.executeUpdate();

			try (
			    PreparedStatement insert = con.prepareStatement(INSERT_GRAND_BOSS_LIST))
			{
				for (final Entry<Integer, BossZone> e : _zones.entrySet())
				{
					final List<Integer> list = e.getValue().getAllowedPlayers();
					if ((list == null) || list.isEmpty())
					{
						continue;
					}
					for (final Integer player : list)
					{
						insert.setInt(1, player);
						insert.setInt(2, e.getKey());
						insert.executeUpdate();
						insert.clearParameters();
					}
				}
			}
			
			for (final Entry<Integer, StatsSet> e : _storedInfo.entrySet())
			{
				final GrandBossInstance boss = _bosses.get(e.getKey());
				final StatsSet info = e.getValue();
				if ((boss == null) || (info == null))
				{
					try (
					    PreparedStatement update = con.prepareStatement(UPDATE_GRAND_BOSS_DATA2))
					{
						update.setInt(1, _bossStatus.get(e.getKey()));
						update.setLong(2, info != null ? info.getLong("respawnTime", 0) : 0);
						update.setInt(3, e.getKey());
						update.executeUpdate();
						update.clearParameters();
					}
				}
				else
				{
					try (
					    PreparedStatement update = con.prepareStatement(UPDATE_GRAND_BOSS_DATA))
					{
						update.setInt(1, boss.getX());
						update.setInt(2, boss.getY());
						update.setInt(3, boss.getZ());
						update.setInt(4, boss.getHeading());
						update.setLong(5, info.getLong("respawnTime"));
						double hp = boss.getCurrentHp();
						double mp = boss.getCurrentMp();
						if (boss.isDead())
						{
							hp = boss.getMaxHp();
							mp = boss.getMaxMp();
						}
						update.setDouble(6, hp);
						update.setDouble(7, mp);
						update.setInt(8, _bossStatus.get(e.getKey()));
						update.setInt(9, e.getKey());
						update.executeUpdate();
						update.clearParameters();
					}
				}
			}
		}
		catch (final SQLException e)
		{
			warn("Couldn't store grandbosses to database:" + e.getMessage(), e);
		}
	}

	private void updateDb(int bossId, boolean statusOnly)
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final GrandBossInstance boss = _bosses.get(bossId);
			final StatsSet info = _storedInfo.get(bossId);

			if (statusOnly || (boss == null) || (info == null))
			{
				try (
				    var statement = con.prepareStatement(UPDATE_GRAND_BOSS_DATA2))
				{
					statement.setInt(1, _bossStatus.get(bossId));
					statement.setLong(2, info != null ? info.getLong("respawnTime", 0) : 0);
					statement.setInt(3, bossId);
					statement.executeUpdate();
				}
			}
			else
			{
				try (
				    var statement = con.prepareStatement(UPDATE_GRAND_BOSS_DATA))
				{
					statement.setInt(1, boss.getX());
					statement.setInt(2, boss.getY());
					statement.setInt(3, boss.getZ());
					statement.setInt(4, boss.getHeading());
					statement.setLong(5, info.getLong("respawnTime"));
					double hp = boss.getCurrentHp();
					double mp = boss.getCurrentMp();
					if (boss.isDead())
					{
						hp = boss.getMaxHp();
						mp = boss.getMaxMp();
					}
					statement.setDouble(6, hp);
					statement.setDouble(7, mp);
					statement.setInt(8, _bossStatus.get(bossId));
					statement.setInt(9, bossId);
					statement.executeUpdate();
				}
			}
		}
		catch (final SQLException e)
		{
			warn("Couldn't update grandbosses to database:" + e.getMessage(), e);
		}
	}

	public void cleanUp()
	{
		storeToDb();

		_bosses.clear();
		_storedInfo.clear();
		_bossStatus.clear();
		_zones.clear();
	}

	public Map<Integer, BossZone> getZones()
	{
		return _zones;
	}
	
	public Map<Integer, StatsSet> getStoredInfo()
	{
		return _storedInfo;
	}
	
	public static String respawnTimeFormat(StatsSet info)
	{
		return Util.dateFormat(info.getLong("respawnTime"));
	}
	
	public long setRespawnTime(int npcId, String time)
	{
		SchedulingPattern cronTime;
		try
		{
			cronTime = new SchedulingPattern(time);
		}
		catch (final InvalidPatternException e)
		{
			return 0L;
		}
		
		final long respawnTime = cronTime.next(System.currentTimeMillis());
		final Calendar date = Calendar.getInstance();
		date.setTimeInMillis(respawnTime);
		
		if (npcId != 29065)
		{
			getStatsSet(npcId).set("respawnTime", date.getTimeInMillis());
			setBossStatus(npcId, 3, false);
		}
		
		if (Arrays.binarySearch(Config.GRANDBOSS_DEAD_ANNOUNCE_LIST, npcId) >= 0)
		{
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (player.isOnline())
				{
					final ServerMessage msg = new ServerMessage("Announce.RAID_DEATH_ANNOUNCE", player.getLang());
					msg.add(NpcsParser.getInstance().getTemplate(npcId).getName(player.getLang()));
					player.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, "", msg.toString()));
				}
			}
		}
		
		if (Config.EPICBOSS_PRE_ANNOUNCE_LIST.containsKey(npcId))
		{
			final long delay = (Config.EPICBOSS_PRE_ANNOUNCE_LIST.get(npcId) * 60000L);
			final long announceTime = respawnTime - delay;
			if (announceTime > 0)
			{
				RaidBossTaskManager.getInstance().addToAnnounceList(new AnnounceTemplate(NpcsParser.getInstance().getTemplate(npcId), delay), announceTime, true);
			}
		}
		info("" + NpcsParser.getInstance().getTemplate(npcId).getName(null) + " Dead! Respawn date [" + new Date(date.getTimeInMillis()) + "].");
		return date.getTimeInMillis();
	}
	
	public void addAnounceTask(int npcId, long respawnTime, long delay)
	{
		final long announceTime = respawnTime - delay;
		if (announceTime > 0)
		{
			RaidBossTaskManager.getInstance().addToAnnounceList(new AnnounceTemplate(NpcsParser.getInstance().getTemplate(npcId), delay), announceTime, true);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final EpicBossManager _instance = new EpicBossManager();
	}
}