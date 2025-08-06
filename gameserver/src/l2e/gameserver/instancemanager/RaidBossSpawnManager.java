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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.holder.SpawnHolder;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.database.DatabaseUtils;
import l2e.gameserver.database.SqlBatch;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.listener.player.impl.AskToTeleport;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.actor.templates.npc.AnnounceTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ConfirmDlg;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.RadarControl;
import l2e.gameserver.network.serverpackets.ShowMiniMap;
import l2e.gameserver.taskmanager.RaidBossTaskManager;

public class RaidBossSpawnManager extends LoggerObject
{
	private final Map<Integer, RaidBossInstance> _bosses = new ConcurrentHashMap<>();
	private final Map<Integer, Spawner> _spawns = new ConcurrentHashMap<>();
	private final Map<Integer, StatsSet> _storedInfo = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, Integer>> _points = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _clanPoints = new ConcurrentHashMap<>();
	
	private static final Integer KEY_RANK = new Integer(-1);
	private static final Integer KEY_TOTAL_POINTS = new Integer(0);
	private final Lock _pointsLock = new ReentrantLock();
	
	public static enum StatusEnum
	{
		ALIVE, DEAD, UNDEFINED
	}
	
	protected RaidBossSpawnManager()
	{
		load();
		restorePoints();
		calculateRanking();
	}
	
	public void load()
	{
		_bosses.clear();
		_spawns.clear();
		_storedInfo.clear();
		_points.clear();
		_clanPoints.clear();
		
		Connection con = null;
		final Statement statement = null;
		ResultSet rset = null;
		
		NpcTemplate template;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			rset = con.createStatement().executeQuery("SELECT * FROM raidboss_status ORDER BY boss_id");
			while (rset.next())
			{
				template = getValidTemplate(rset.getInt("boss_id"));
				if (template != null)
				{
					final int id = rset.getInt("boss_id");
					final StatsSet info = new StatsSet();
					info.set("currentHp", rset.getDouble("currentHp"));
					info.set("currentMp", rset.getDouble("currentMp"));
					info.set("respawnTime", rset.getLong("respawn_time"));
					_storedInfo.put(id, info);
				}
				else
				{
					warn("Could not load raidboss #" + rset.getInt("boss_id") + " from DataBase");
				}
			}
			info("Loaded " + _storedInfo.size() + " statuses.");
		}
		catch (final SQLException e)
		{
			warn("Couldnt load raidboss_status table");
		}
		catch (final Exception e)
		{
			warn("Error while initializing RaidBossSpawnManager: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void updateStatus(RaidBossInstance boss, boolean isBossDead)
	{
		if (!_storedInfo.containsKey(boss.getId()))
		{
			return;
		}
		
		final var info = _storedInfo.get(boss.getId());
		
		if (isBossDead)
		{
			boss.setRaidStatus(StatusEnum.DEAD);
			
			long respawnDelay = 0;
			long respawnTime = 0L;
			
			final int respawnMinDelay = (int) (boss.getSpawn().getRespawnMinDelay() * Config.RAID_MIN_RESPAWN_MULTIPLIER);
			final int respawnMaxDelay = (int) (boss.getSpawn().getRespawnMaxDelay() * Config.RAID_MAX_RESPAWN_MULTIPLIER);
			
			if (boss.getSpawn().getRespawnPattern() != null)
			{
				respawnTime = boss.getSpawn().getRespawnPattern().next(System.currentTimeMillis());
				respawnDelay = respawnTime - System.currentTimeMillis();
			}
			else
			{
				respawnDelay = Rnd.get(respawnMinDelay, respawnMaxDelay);
				respawnTime = System.currentTimeMillis() + respawnDelay;
			}
			info.set("currentHP", boss.getMaxHp());
			info.set("currentMP", boss.getMaxMp());
			info.set("respawnTime", respawnTime);
			final var instance = RaidBossTaskManager.getInstance();
			if (!instance.isInRaidList(boss.getId()) && respawnTime > System.currentTimeMillis())
			{
				final Calendar time = Calendar.getInstance();
				time.setTimeInMillis(respawnTime);
				if (Arrays.binarySearch(Config.RAIDBOSS_DEAD_ANNOUNCE_LIST, boss.getId()) >= 0)
				{
					for (final Player player : GameObjectsStorage.getPlayers())
					{
						if (player.isOnline())
						{
							final ServerMessage msg = new ServerMessage("Announce.RAID_DEATH_ANNOUNCE", player.getLang());
							msg.add(boss.getTemplate().getName(player.getLang()));
							player.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, "", msg.toString()));
						}
					}
				}
				if (Config.RAIDBOSS_PRE_ANNOUNCE_LIST.containsKey(boss.getId()))
				{
					final long delay = (Config.RAIDBOSS_PRE_ANNOUNCE_LIST.get(boss.getId()) * 60000L);
					final long announceTime = respawnTime - delay;
					if (announceTime > 0)
					{
						instance.addToAnnounceList(new AnnounceTemplate(boss.getTemplate(), delay), announceTime, true);
					}
				}
				info("Updated " + boss.getName(null) + " respawn time to " + time.getTime());
				instance.addToRaidList(boss.getId(), respawnTime, true);
				
				Connection con = null;
				PreparedStatement statement = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement("REPLACE INTO `raidboss_status` (boss_id, currentHp, currentMp, respawn_time) VALUES (?,?,?,?)");
					statement.setInt(1, boss.getId());
					statement.setDouble(2, info.getDouble("currentHP"));
					statement.setDouble(3, info.getDouble("currentMP"));
					statement.setLong(4, info.getLong("respawnTime", 0));
					statement.execute();
				}
				catch (final Exception e)
				{
					warn("Couldnt update raidboss_status table!");
				}
				finally
				{
					DbUtils.closeQuietly(con, statement);
				}
			}
		}
		else
		{
			boss.setRaidStatus(StatusEnum.ALIVE);
			
			info.set("currentHP", boss.getCurrentHp());
			info.set("currentMP", boss.getCurrentMp());
			info.set("respawnTime", 0L);
		}
		_storedInfo.put(boss.getId(), info);
	}
	
	private void restorePoints()
	{
		_pointsLock.lock();
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery("SELECT charId, boss_id, points FROM `raidboss_points` ORDER BY charId ASC");
			int currentOwner = 0;
			Map<Integer, Integer> score = null;
			while (rset.next())
			{
				if (currentOwner != rset.getInt("charId"))
				{
					currentOwner = rset.getInt("charId");
					score = new HashMap<>();
					_points.put(currentOwner, score);
				}
				
				assert score != null;
				final int bossId = rset.getInt("boss_id");
				final NpcTemplate template = NpcsParser.getInstance().getTemplate(bossId);
				if (bossId != KEY_RANK && bossId != KEY_TOTAL_POINTS && template != null && template.getRewardRp() > 0)
				{
					score.put(bossId, rset.getInt("points"));
				}
			}
		}
		catch (final SQLException e)
		{
			warn("Couldnt load raidboss points", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
			
		// wtf?
		for (final Map.Entry<Integer, Map<Integer, Integer>> charPoints : _points.entrySet())
		{
			final Map<Integer, Integer> tmpPoint = charPoints.getValue();
			
			int totalPoints = 0;
			for (final Entry<Integer, Integer> e : tmpPoint.entrySet())
			{
				totalPoints += e.getValue();
			}
			
			if (totalPoints != 0)
			{
				final int clanId = ClanHolder.getInstance().getClanId(charPoints.getKey());
				if (clanId != 0)
				{
					if (_clanPoints.containsKey(clanId))
					{
						final int clanPoints = _clanPoints.get(clanId);
						_clanPoints.put(clanId, (clanPoints + totalPoints));
					}
					else
					{
						_clanPoints.put(clanId, totalPoints);
					}
				}
			}
		}
		_pointsLock.unlock();
	}
	
	private void updatePointsDb()
	{
		_pointsLock.lock();
		if (!DatabaseUtils.set("TRUNCATE `raidboss_points`"))
		{
			warn("Couldnt empty raidboss_points table!");
		}
		
		if (_points.isEmpty())
		{
			_pointsLock.unlock();
			return;
		}
		
		Connection con = null;
		Statement statement = null;
		StringBuilder sb;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			final SqlBatch b = new SqlBatch("INSERT INTO `raidboss_points` (charId, boss_id, points) VALUES");
			
			for (final Map.Entry<Integer, Map<Integer, Integer>> pointEntry : _points.entrySet())
			{
				final Map<Integer, Integer> tmpPoint = pointEntry.getValue();
				if (tmpPoint == null || tmpPoint.isEmpty())
				{
					continue;
				}
				
				for (final Map.Entry<Integer, Integer> pointListEntry : tmpPoint.entrySet())
				{
					if (KEY_RANK.equals(pointListEntry.getKey()) || KEY_TOTAL_POINTS.equals(pointListEntry.getKey()) || pointListEntry.getValue() == null || pointListEntry.getValue() == 0)
					{
						continue;
					}
					
					sb = new StringBuilder("(");
					sb.append(pointEntry.getKey()).append(",");
					sb.append(pointListEntry.getKey()).append(",");
					sb.append(pointListEntry.getValue()).append(")");
					b.write(sb.toString());
				}
			}
			
			if (!b.isEmpty())
			{
				statement.executeUpdate(b.close());
			}
		}
		catch (final SQLException e)
		{
			warn("Couldnt update raidboss_points table!");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		_pointsLock.unlock();
	}
	
	public void addPoints(Player player, int bossId, int points)
	{
		if (points <= 0 || player.getObjectId() <= 0 || bossId <= 0)
		{
			return;
		}
		player.getListeners().onRaidPoints(points);
		
		_pointsLock.lock();
		try
		{
			Map<Integer, Integer> pointsTable = _points.get(player.getObjectId());
			
			if (pointsTable == null)
			{
				pointsTable = new HashMap<>();
				_points.put(player.getObjectId(), pointsTable);
			}
			
			if (player.getClan() != null)
			{
				if (_clanPoints.containsKey(player.getClan().getId()))
				{
					final int clanPoints = _clanPoints.get(player.getClan().getId());
					_clanPoints.put(player.getClan().getId(), (clanPoints + points));
				}
				else
				{
					_clanPoints.put(player.getClan().getId(), points);
				}
			}
			
			if (pointsTable.isEmpty())
			{
				pointsTable.put(bossId, points);
			}
			else
			{
				final Integer currentPoins = pointsTable.get(bossId);
				pointsTable.put(bossId, currentPoins == null ? points : currentPoins + points);
			}
		}
		finally
		{
			_pointsLock.unlock();
		}
	}
	
	public TreeMap<Integer, Integer> calculateRanking()
	{
		final TreeMap<Integer, Integer> tmpRanking = new TreeMap<>();
		
		_pointsLock.lock();
		try
		{
			for (final Map.Entry<Integer, Map<Integer, Integer>> point : _points.entrySet())
			{
				final Map<Integer, Integer> tmpPoint = point.getValue();
				
				tmpPoint.remove(KEY_RANK);
				tmpPoint.remove(KEY_TOTAL_POINTS);
				int totalPoints = 0;
				
				for (final Entry<Integer, Integer> e : tmpPoint.entrySet())
				{
					totalPoints += e.getValue();
				}
				
				if (totalPoints != 0)
				{
					tmpPoint.put(KEY_TOTAL_POINTS, totalPoints);
					tmpRanking.put(totalPoints, point.getKey());
				}
			}
			
			int ranking = 1;
			for (final Entry<Integer, Integer> entry : tmpRanking.descendingMap().entrySet())
			{
				final Map<Integer, Integer> tmpPoint = _points.get(entry.getValue());
				tmpPoint.put(KEY_RANK, ranking);
				ranking++;
			}
		}
		finally
		{
			_pointsLock.unlock();
		}
		return tmpRanking;
	}
	
	public void distributeRewards()
	{
		_pointsLock.lock();
		try
		{
			final TreeMap<Integer, Integer> ranking = calculateRanking();
			final Iterator<Integer> e = ranking.descendingMap().values().iterator();
			int counter = 1;
			while (e.hasNext() && counter <= 100)
			{
				int reward = 0;
				final int playerId = e.next();
				
				switch (counter)
				{
					case 1 :
						reward = Config.RAID_RANKING_1ST;
						break;
					case 2 :
						reward = Config.RAID_RANKING_2ND;
						break;
					case 3 :
						reward = Config.RAID_RANKING_3RD;
						break;
					case 4 :
						reward = Config.RAID_RANKING_4TH;
						break;
					case 5 :
						reward = Config.RAID_RANKING_5TH;
						break;
					case 6 :
						reward = Config.RAID_RANKING_6TH;
						break;
					case 7 :
						reward = Config.RAID_RANKING_7TH;
						break;
					case 8 :
						reward = Config.RAID_RANKING_8TH;
						break;
					case 9 :
						reward = Config.RAID_RANKING_9TH;
						break;
					case 10 :
						reward = Config.RAID_RANKING_10TH;
						break;
					default :
						if (counter <= 50)
						{
							reward = Config.RAID_RANKING_UP_TO_50TH;
						}
						else
						{
							reward = Config.RAID_RANKING_UP_TO_100TH;
						}
						break;
				}
				Clan clan = null;
				final Player player = GameObjectsStorage.getPlayer(playerId);
				if (player != null)
				{
					clan = player.getClan();
				}
				else
				{
					final int res = ClanHolder.getInstance().getClanId(playerId);
					if (res != 0)
					{
						clan = ClanHolder.getInstance().getClan(res);
					}
				}
				if (clan != null)
				{
					clan.addReputationScore(reward, true);
				}
				counter++;
			}
			_points.clear();
			updatePointsDb();
		}
		finally
		{
			_pointsLock.unlock();
		}
	}
	
	public void addNewSpawn(Spawner spawnDat, boolean storeInDb)
	{
		if (spawnDat == null)
		{
			return;
		}
		if (_spawns.containsKey(spawnDat.getId()))
		{
			return;
		}
		
		final var info = _storedInfo.get(spawnDat.getId());
		if (info != null)
		{
			final int bossId = spawnDat.getId();
			final long time = System.currentTimeMillis();
			final var instance = RaidBossTaskManager.getInstance();
			final long respawnTime = info.getLong("respawnTime");
			final int currentHP = info.getInteger("currentHp");
			final int currentMP = info.getInteger("currentMp");

			if ((respawnTime == 0L) || (time > respawnTime))
			{
				RaidBossInstance raidboss = null;

				if (spawnDat.getSpawnTemplate().getPeriodOfDay().equals("night"))
				{
					raidboss = DayNightSpawnManager.getInstance().handleBoss(spawnDat);
				}
				else
				{
					raidboss = (RaidBossInstance) spawnDat.doSpawn();
				}

				if (raidboss != null)
				{
					if (currentHP == 0)
					{
						raidboss.setCurrentHp(raidboss.getMaxHp());
					}
					else
					{
						raidboss.setCurrentHp(currentHP);
					}
					if (currentMP == 0)
					{
						raidboss.setCurrentMp(raidboss.getMaxMp());
					}
					else
					{
						raidboss.setCurrentMp(currentMP);
					}
					raidboss.setRaidStatus(StatusEnum.ALIVE);

					_bosses.put(bossId, raidboss);

					info.set("currentHP", raidboss.getCurrentHp());
					info.set("currentMP", raidboss.getCurrentMp());
					info.set("respawnTime", 0L);
				}
			}
			else
			{
				if (Config.RAIDBOSS_PRE_ANNOUNCE_LIST.containsKey(bossId))
				{
					final long delay = (Config.RAIDBOSS_PRE_ANNOUNCE_LIST.get(bossId) * 60000L);
					final long announceTime = respawnTime - delay;
					if (announceTime > 0)
					{
						instance.addToAnnounceList(new AnnounceTemplate(NpcsParser.getInstance().getTemplate(bossId), delay), announceTime, false);
					}
				}
				instance.addToRaidList(bossId, respawnTime, false);
			}
			_spawns.put(bossId, spawnDat);
		}
		else
		{
			if (!spawnDat.getTemplate().getParameter("isDestructionBoss", false) && spawnDat.getId() != 25665 && spawnDat.getId() != 25666)
			{
				info("Could not load raidboss #" + spawnDat.getId() + " status in database.");
			}
		}
		
		if (storeInDb)
		{
			final RaidBossInstance raidboss = (RaidBossInstance) spawnDat.doSpawn();
			raidboss.setCurrentHp(raidboss.getMaxHp());
			raidboss.setCurrentMp(raidboss.getMaxMp());
			
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO raidboss_status (boss_id,respawn_time,currentHp,currentMp) VALUES(?,?,?,?)");
				statement.setInt(1, spawnDat.getId());
				statement.setLong(2, 0);
				statement.setDouble(3, raidboss.getMaxHp());
				statement.setDouble(4, raidboss.getMaxMp());
				statement.execute();
				
				final StatsSet inf = new StatsSet();
				inf.set("currentHP", raidboss.getMaxHp());
				inf.set("currentMP", raidboss.getMaxMp());
				inf.set("respawnTime", 0L);
				_storedInfo.put(spawnDat.getId(), inf);
			}
			catch (final Exception e)
			{
				warn("Could not store raidboss #" + spawnDat.getId() + " in the DB:" + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public void deleteSpawn(Spawner spawnDat, boolean updateDb)
	{
		if (spawnDat == null)
		{
			return;
		}
		
		final int bossId = spawnDat.getId();
		if (!_spawns.containsKey(bossId))
		{
			return;
		}
		
		SpawnHolder.getInstance().deleteSpawn(spawnDat, false);
		_spawns.remove(bossId);
		
		if (_bosses.containsKey(bossId))
		{
			_bosses.remove(bossId);
		}
		
		RaidBossTaskManager.getInstance().removeFromList(bossId);
		
		if (_storedInfo.containsKey(bossId))
		{
			_storedInfo.remove(bossId);
		}
		
		if (updateDb)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM raidboss_status WHERE boss_id=?");
				statement.setInt(1, bossId);
				statement.execute();
			}
			catch (final Exception e)
			{
				warn("Could not remove raidboss #" + bossId + " from DB: " + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	private void updateAllStatusDb()
	{
		for (final int id : _storedInfo.keySet())
		{
			updateStatusDb(id, false);
		}
	}
	
	private void updateStatusDb(int id, boolean isDead)
	{
		final Spawner spawner = _spawns.get(id);
		if (spawner == null)
		{
			return;
		}
		
		StatsSet info = _storedInfo.get(id);
		if (info == null)
		{
			_storedInfo.put(id, info = new StatsSet());
		}
		
		final RaidBossInstance raidboss = _bosses.get(id);
		if (raidboss != null && raidboss.getRaidStatus().equals(StatusEnum.ALIVE))
		{
			info.set("currentHP", raidboss.getCurrentHp());
			info.set("currentMP", raidboss.getCurrentMp());
			info.set("respawnTime", 0);
		}
		else
		{
			info.set("currentHP", 0);
			info.set("currentMP", 0);
			info.set("respawnTime", info.getLong("respawnTime"));
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO `raidboss_status` (boss_id, currentHP, currentMP, respawn_time) VALUES (?,?,?,?)");
			statement.setInt(1, id);
			statement.setDouble(2, info.getDouble("currentHP"));
			statement.setDouble(3, info.getDouble("currentMP"));
			statement.setLong(4, info.getLong("respawnTime"));
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Couldnt update raidboss_status table!");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public String[] getAllRaidBossStatus()
	{
		final String[] msg = new String[(_bosses == null) ? 0 : _bosses.size()];
		
		if (_bosses == null)
		{
			msg[0] = "None";
			return msg;
		}
		
		int index = 0;
		
		for (final int i : _bosses.keySet())
		{
			final RaidBossInstance boss = _bosses.get(i);
			
			msg[index++] = boss.getName(null) + ": " + boss.getRaidStatus().name();
		}
		
		return msg;
	}
	
	public String getRaidBossStatus(int bossId)
	{
		String msg = "RaidBoss Status..." + Config.EOL;
		
		if (_bosses == null)
		{
			msg += "None";
			return msg;
		}
		
		if (_bosses.containsKey(bossId))
		{
			final RaidBossInstance boss = _bosses.get(bossId);
			
			msg += boss.getName(null) + ": " + boss.getRaidStatus().name();
		}
		
		return msg;
	}
	
	public StatusEnum getRaidBossStatusId(int bossId)
	{
		if (_bosses.containsKey(bossId))
		{
			return _bosses.get(bossId).getRaidStatus();
		}
		else if (RaidBossTaskManager.getInstance().isInRaidList(bossId))
		{
			return StatusEnum.DEAD;
		}
		else
		{
			return StatusEnum.UNDEFINED;
		}
	}

	public NpcTemplate getValidTemplate(int bossId)
	{
		final var template = NpcsParser.getInstance().getTemplate(bossId);
		if (template == null)
		{
			return null;
		}
		if (template.isType("RaidBoss") || template.isType("FlyRaidBoss"))
		{
			return template;
		}
		return null;
	}
	
	public void notifySpawnNightBoss(RaidBossInstance raidboss)
	{
		final var info = new StatsSet();
		info.set("currentHP", raidboss.getCurrentHp());
		info.set("currentMP", raidboss.getCurrentMp());
		info.set("respawnTime", 0L);
		
		raidboss.setRaidStatus(StatusEnum.ALIVE);
		
		_storedInfo.put(raidboss.getId(), info);
		
		info("Spawning Night Raid Boss " + raidboss.getName(null));
		
		_bosses.put(raidboss.getId(), raidboss);
	}
	
	public boolean isDefined(int bossId)
	{
		return _spawns.containsKey(bossId);
	}
	
	public Map<Integer, RaidBossInstance> getBosses()
	{
		return _bosses;
	}
	
	public void addRaidBoss(int bossId, RaidBossInstance raid)
	{
		_bosses.put(bossId, raid);
	}
	
	public Map<Integer, Spawner> getSpawns()
	{
		return _spawns;
	}
	
	public Map<Integer, StatsSet> getStoredInfo()
	{
		return _storedInfo;
	}
	
	public void addStoreInfo(int bossId, StatsSet info)
	{
		_storedInfo.put(bossId, info);
	}
	
	public void cleanUp()
	{
		updateAllStatusDb();
		_bosses.clear();
		RaidBossTaskManager.getInstance().cleanUp();
		_storedInfo.clear();
		_spawns.clear();
		updatePointsDb();
	}

	public void showBossLocation(Player player, int bossId)
	{
		for (final int id : Config.BLOCKED_RAID_LIST)
		{
			if (id == bossId)
			{
				return;
			}
		}
		
		switch (getInstance().getRaidBossStatusId(bossId))
		{
			case ALIVE :
			case DEAD :
				final Spawner spawn = getInstance().getSpawns().get(bossId);
				final Location loc = spawn.calcSpawnRangeLoc(spawn.getTemplate());
				
				final Player _player = player;
				final Location loc1 = loc;
				new Timer().schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						_player.sendPacket(new RadarControl(2, 2, loc1.getX(), loc1.getY(), loc1.getZ()));
						_player.sendPacket(new RadarControl(0, 1, loc1.getX(), loc1.getY(), loc1.getZ()));
					}
				}, 500);
				player.sendPacket(new ShowMiniMap(0));
				break;
			case UNDEFINED :
				final ServerMessage msg = new ServerMessage("BossesBBS.BOSS_NOT_INGAME", player.getLang());
				msg.add(bossId);
				player.sendMessage(msg.toString());
				break;
		}
	}

	public RaidBossInstance getBossStatus(int bossId)
	{
		if (_bosses.containsKey(bossId))
		{
			if (_bosses.get(bossId).getRaidStatus() == StatusEnum.ALIVE)
			{
				return _bosses.get(bossId);
			}
		}
		return null;
	}
	
	public Map<Integer, Map<Integer, Integer>> getPoints()
	{
		return _points;
	}
	
	public Map<Integer, Integer> getClanPoints()
	{
		return _clanPoints;
	}
	
	public Map<Integer, Integer> getPointsForOwnerId(int ownerId)
	{
		return _points.get(ownerId);
	}
	
	public int getPlayerRaidPoints(int ownerId)
	{
		final Map<Integer, Integer> points = _points.get(ownerId);
		if (points != null && !points.isEmpty())
		{
			for (final Map.Entry<Integer, Integer> e : points.entrySet())
			{
				switch (e.getKey())
				{
					case 0 :
						return e.getValue();
				}
			}
		}
		return 0;
	}
	
	public Map<Integer, Integer> getList(Player player)
	{
		return _points.get(player.getObjectId());
	}

	public void onBossSpawned(RaidBossInstance raidboss, RaidBossSpawnManager instance)
	{
		if (raidboss != null)
		{
			final int bossId = raidboss.getId();
			if (Arrays.binarySearch(Config.RAIDBOSS_ANNOUNCE_LIST, raidboss.getId()) >= 0)
			{
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					if (player.isOnline())
					{
						final ServerMessage msg = new ServerMessage("Announce.RAID_RESPAWN", player.getLang());
						msg.add(raidboss.getTemplate().getName(player.getLang()));
						player.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, "", msg.toString()));
					}
				}
			}
			raidboss.setRaidStatus(StatusEnum.ALIVE);
			final StatsSet info = new StatsSet();
			info.set("currentHP", raidboss.getCurrentHp());
			info.set("currentMP", raidboss.getCurrentMp());
			info.set("respawnTime", 0L);

			instance.addStoreInfo(bossId, info);

			info("Spawning Raid Boss " + raidboss.getName(null));

			instance.addRaidBoss(bossId, raidboss);
		}
	}

	public void askOnSummon(final RaidBossInstance raidboss, RaidBossSpawnManager instance) {
		if (raidboss != null)
		{
			final int bossId = raidboss.getId();

			try
			{
				if ((Config.SUMMON_TO_RB_ENABLE && Config.SUMMON_TO_RB_IDS.size() > 0 && Config.SUMMON_TO_RB_IDS.contains(bossId)) || Config.SUMMON_TO_RB_IDS.contains(-1))
				{
					for (Player player : GameObjectsStorage.getPlayers())
					{
						if (player == null)
							continue;
						if (player.getLevel() < Config.SUMMON_TO_RB_MIN_LVL || player.isInFightEvent() || player.isInOlympiadMode())
							continue;

						player.sendConfirmDlg(new AskToTeleport(player, raidboss), (int) TimeUnit.SECONDS.toMillis(10L), "Teleport to RaidBoss " + raidboss.getName(null) + "?");
					}
					ThreadPoolManager.getInstance().schedule(() -> onBossSpawned(raidboss, instance), TimeUnit.SECONDS.toMillis(Config.SUMMON_TO_RB_HOWDOES));
				}
				else
					onBossSpawned(raidboss, instance);
			} catch (Exception e) {
				_log.warn("askOnSummon <-----");
				onBossSpawned(raidboss, instance);
			}
		}

	}

	public void askOnSummon(final RaidBossInstance raidboss) {
		int bossId = raidboss.getId();

		try {
			if ((Config.SUMMON_TO_RB_ENABLE && Config.SUMMON_TO_RB_IDS.size() > 0 && Config.SUMMON_TO_RB_IDS.contains(bossId)) || Config.SUMMON_TO_RB_IDS.contains(-1)) {

				for (Player player : GameObjectsStorage.getPlayers()) {
					if (player == null)
						continue;
					if (player.getLevel() < Config.SUMMON_TO_RB_MIN_LVL || player.isInFightEvent() || player.isInOlympiadMode())
						continue;


					player.sendConfirmDlg(new AskToTeleport(player, raidboss), (int) TimeUnit.SECONDS.toMillis(10L), "Teleport to RaidBoss " + raidboss.getName(null) + "?");
				}
			}
		} catch (Exception e) {
			_log.warn("askOnSummon <-----");
		}
	}

	public static RaidBossSpawnManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidBossSpawnManager _instance = new RaidBossSpawnManager();
	}
}