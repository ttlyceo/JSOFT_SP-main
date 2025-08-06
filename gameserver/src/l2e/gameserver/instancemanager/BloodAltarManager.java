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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;

/**
 * Created by LordWinter 14.02.2019
 */
public class BloodAltarManager extends LoggerObject
{
	private final Map<Integer, StatsSet> _bosses = new ConcurrentHashMap<>();
	private final Map<Integer, RaidBossInstance> _spawns = new ConcurrentHashMap<>();
	private final Map<String, StatsSet> _altars = new ConcurrentHashMap<>();
	
	protected BloodAltarManager()
	{
		_bosses.clear();
		_altars.clear();
		_spawns.clear();
		
		loadAltars();
		loadBosses();
		
		info("Loaded " + _altars.size() + " blood altars and " + _bosses.size() + " destruction bosses.");
	}
	
	private void loadAltars()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM blood_altars ORDER BY altar_name");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final String altar_name = rset.getString("altar_name");

				final StatsSet info = new StatsSet();
				info.set("status", rset.getInt("status"));
				info.set("progress", rset.getInt("progress"));
				info.set("changeTime", rset.getLong("changeTime"));
				_altars.put(altar_name, info);
			}
		}
		catch (final SQLException e)
		{
			warn("Couldnt load blood_altars table");
		}
		catch (final Exception e)
		{
			warn("Error while initializing BloodAltarManager: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void loadBosses()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM destruction_bosses ORDER BY bossId");
			rs = statement.executeQuery();
			while (rs.next())
			{
				final int bossId = rs.getInt("bossId");

				final StatsSet info = new StatsSet();
				info.set("altar_name", rs.getString("altar_name"));
				info.set("status", rs.getInt("status"));
				info.set("currentHp", rs.getDouble("currentHp"));
				info.set("currentMp", rs.getDouble("currentMp"));
				_bosses.put(bossId, info);
			}
		}
		catch (final SQLException e)
		{
			warn("Couldnt load destruction_bosses table");
		}
		catch (final Exception e)
		{
			warn("Error while initializing BloodAltarManager: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	public void addBossSpawn(Spawner spawnDat)
	{
		if (spawnDat == null)
		{
			return;
		}
		
		if (_spawns.containsKey(spawnDat.getId()))
		{
			return;
		}
		
		final StatsSet info = _bosses.get(spawnDat.getId());
		if (info != null)
		{
			final int bossId = spawnDat.getId();
			final double currentHP = info.getDouble("currentHp");
			final double currentMP = info.getDouble("currentMp");

			spawnDat.stopRespawn();
			
			final RaidBossInstance raidboss = (RaidBossInstance) spawnDat.doSpawn();
			
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
				
				info.set("currentHp", raidboss.getCurrentHp());
				info.set("currentMp", raidboss.getCurrentMp());
				
				_spawns.put(bossId, raidboss);
			}
		}
		else
		{
			info("Could not load destruction boss #" + spawnDat.getId() + " status in database.");
		}
	}

	public void removeBossSpawn(Spawner spawnDat)
	{
		if (spawnDat == null || !_spawns.containsKey(spawnDat.getId()))
		{
			return;
		}
		_spawns.remove(spawnDat.getId());
	}
	
	public StatsSet getAltarInfo(String altar)
	{
		if (!_altars.containsKey(altar))
		{
			return null;
		}
		return _altars.get(altar);
	}
	
	public List<Integer> getDeadBossList(String altar)
	{
		final List<Integer> bossList = new ArrayList<>();
		for (final int i : _bosses.keySet())
		{
			final StatsSet info = _bosses.get(i);
			if (info.getString("altar_name").equalsIgnoreCase(altar))
			{
				if (info.getInteger("status") == 1)
				{
					bossList.add(i);
				}
			}
		}
		return bossList;
	}
	
	public List<Integer> getBossList(String altar)
	{
		final List<Integer> altar_bosses = new ArrayList<>();
		for (final int i : _bosses.keySet())
		{
			final StatsSet info = _bosses.get(i);
			if (info.getString("altar_name").equalsIgnoreCase(altar))
			{
				altar_bosses.add(i);
			}
		}
		return altar_bosses;
	}
	
	public void cleanBossStatus(String altar)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			for (final int i : _bosses.keySet())
			{
				final StatsSet info = _bosses.get(i);
				if (info.getString("altar_name").equalsIgnoreCase(altar))
				{
					info.set("status", 0);
				}
				
				statement = con.prepareStatement("UPDATE destruction_bosses SET status = ? WHERE bossId = ?");
				statement.setInt(1, info.getInteger("status"));
				statement.setInt(2, i);
				statement.execute();
				statement.close();
			}
		}
		catch (final Exception e)
		{
			warn("could not clean status for destruction bosses in database!");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void updateBossStatus(String altar, RaidBossInstance boss, int status)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			for (final int i : _bosses.keySet())
			{
				final StatsSet info = _bosses.get(i);
				if (info.getString("altar_name").equalsIgnoreCase(altar) && i == boss.getId())
				{
					double currentHP = 0;
					double currentMP = 0;
					if (status == 1)
					{
						currentHP = boss.getMaxHp();
						currentMP = boss.getMaxMp();
					}
					else
					{
						currentHP = boss.getCurrentHp();
						currentMP = boss.getCurrentMp();
						info.set("status", 0);
					}
					info.set("currentHP", currentHP);
					info.set("currentMP", currentMP);
					info.set("status", status);
					
					statement = con.prepareStatement("UPDATE destruction_bosses SET currentHP = ?, currentMP = ?, status = ? WHERE bossId = ?");
					statement.setDouble(1, currentHP);
					statement.setDouble(2, currentMP);
					statement.setInt(3, status);
					statement.setInt(4, boss.getId());
					statement.execute();
					statement.close();
				}
			}
		}
		catch (final Exception e)
		{
			warn("could not update destruction bossId: " + boss.getId() + " in database!");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void updateStatusTime(String altar, long time)
	{
		if (_altars.containsKey(altar))
		{
			final StatsSet info = _altars.get(altar);
			info.set("changeTime", time);
			
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE blood_altars SET changeTime = ? WHERE altar_name = ?");
				statement.setLong(1, time);
				statement.setString(2, altar);
				statement.execute();
			}
			catch (final Exception e)
			{
				warn("could not update: " + altar + " time in database!");
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public void updateProgress(String altar, int progress)
	{
		if (_altars.containsKey(altar))
		{
			final StatsSet info = _altars.get(altar);
			info.set("progress", progress);

			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE blood_altars SET progress = ? WHERE altar_name = ?");
				statement.setInt(1, progress);
				statement.setString(2, altar);
				statement.execute();
			}
			catch (final Exception e)
			{
				warn("could not update: " + altar + " in database!");
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	public void updateStatus(String altar, int status)
	{
		if (_altars.containsKey(altar))
		{
			final StatsSet info = _altars.get(altar);
			info.set("status", status);
			
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE blood_altars SET status = ? WHERE altar_name = ?");
				statement.setInt(1, status);
				statement.setString(2, altar);
				statement.execute();
			}
			catch (final Exception e)
			{
				warn("could not update: " + altar + " in database!");
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public void saveDb()
	{
		for (final int i : _bosses.keySet())
		{
			final RaidBossInstance boss = _spawns.get(i);
			if (boss == null)
			{
				continue;
			}
			
			final StatsSet info = _bosses.get(i);
			if (info == null)
			{
				continue;
			}
			
			updateBossStatus(info.getString("altar_name"), boss, 0);
		}
		_spawns.clear();
	}

	public static BloodAltarManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final BloodAltarManager _instance = new BloodAltarManager();
	}
}