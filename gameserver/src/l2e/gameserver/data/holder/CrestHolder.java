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
package l2e.gameserver.data.holder;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.file.filter.BMPFilter;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Crest;
import l2e.gameserver.model.CrestType;

public final class CrestHolder extends LoggerObject
{
	private final Map<Integer, Crest> _crests = new ConcurrentHashMap<>();
	private final AtomicInteger _nextId = new AtomicInteger(1);
	
	protected CrestHolder()
	{
		load();
	}
	
	public synchronized void load()
	{
		_crests.clear();
		final Set<Integer> crestsInUse = new HashSet<>();
		for (final var clan : ClanHolder.getInstance().getClans())
		{
			if (clan.getCrestId() != 0)
			{
				crestsInUse.add(clan.getCrestId());
			}
			
			if (clan.getCrestLargeId() != 0)
			{
				crestsInUse.add(clan.getCrestLargeId());
			}
			
			if (clan.getAllyCrestId() != 0)
			{
				crestsInUse.add(clan.getAllyCrestId());
			}
		}
		
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			rset = statement.executeQuery("SELECT `crest_id`, `data`, `type` FROM `crests` ORDER BY `crest_id` DESC");
			while (rset.next())
			{
				final var id = rset.getInt("crest_id");
				
				if (_nextId.get() <= id)
				{
					_nextId.set(id + 1);
				}
				
				if (!crestsInUse.contains(id) && (id != (_nextId.get() - 1)))
				{
					rset.deleteRow();
					continue;
				}
				
				final byte[] data = rset.getBytes("data");
				final var crestType = CrestType.getById(rset.getInt("type"));
				if (crestType != null)
				{
					_crests.put(id, new Crest(id, data, crestType));
				}
				else
				{
					warn("Unknown crest type found in database. Type:" + rset.getInt("type"));
				}
			}
			
		}
		catch (final SQLException e)
		{
			warn("There was an error while loading crests from database:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		moveOldCrestsToDb(crestsInUse);
		
		info("Loaded " + _crests.size() + " Crests.");
		
		for (final var clan : ClanHolder.getInstance().getClans())
		{
			if (clan.getCrestId() != 0)
			{
				if (getCrest(clan.getCrestId()) == null)
				{
					info("Removing non-existent crest for clan " + clan.getName() + " [" + clan.getId() + "], crestId:" + clan.getCrestId());
					clan.setCrestId(0);
					clan.changeClanCrest(0);
				}
			}
			
			if (clan.getCrestLargeId() != 0)
			{
				if (getCrest(clan.getCrestLargeId()) == null)
				{
					info("Removing non-existent large crest for clan " + clan.getName() + " [" + clan.getId() + "], crestLargeId:" + clan.getCrestLargeId());
					clan.setCrestLargeId(0);
					clan.changeLargeCrest(0);
				}
			}
			
			if (clan.getAllyCrestId() != 0)
			{
				if (getCrest(clan.getAllyCrestId()) == null)
				{
					info("Removing non-existent ally crest for clan " + clan.getName() + " [" + clan.getId() + "], allyCrestId:" + clan.getAllyCrestId());
					clan.setAllyCrestId(0);
					clan.changeAllyCrest(0, true);
				}
			}
		}
	}
	
	private void moveOldCrestsToDb(Set<Integer> crestsInUse)
	{
		final var crestDir = new File(Config.DATAPACK_ROOT, "data/crests/");
		if (crestDir.exists())
		{
			final File[] files = crestDir.listFiles(new BMPFilter());
			if (files == null)
			{
				return;
			}
			
			for (final var file : files)
			{
				try
				{
					final byte[] data = Files.readAllBytes(file.toPath());
					if (file.getName().startsWith("Crest_Large_"))
					{
						final var crestId = Integer.parseInt(file.getName().substring(12, file.getName().length() - 4));
						if (crestsInUse.contains(crestId))
						{
							final var crest = createCrest(data, CrestType.PLEDGE_LARGE);
							if (crest != null)
							{
								for (final var clan : ClanHolder.getInstance().getClans())
								{
									if (clan.getCrestLargeId() == crestId)
									{
										clan.setCrestLargeId(0);
										clan.changeLargeCrest(crest.getId());
									}
								}
							}
						}
					}
					else if (file.getName().startsWith("Crest_"))
					{
						final var crestId = Integer.parseInt(file.getName().substring(6, file.getName().length() - 4));
						if (crestsInUse.contains(crestId))
						{
							final var crest = createCrest(data, CrestType.PLEDGE);
							if (crest != null)
							{
								for (final var clan : ClanHolder.getInstance().getClans())
								{
									if (clan.getCrestId() == crestId)
									{
										clan.setCrestId(0);
										clan.changeClanCrest(crest.getId());
									}
								}
							}
						}
					}
					else if (file.getName().startsWith("AllyCrest_"))
					{
						final var crestId = Integer.parseInt(file.getName().substring(10, file.getName().length() - 4));
						if (crestsInUse.contains(crestId))
						{
							final var crest = createCrest(data, CrestType.ALLY);
							if (crest != null)
							{
								for (final var clan : ClanHolder.getInstance().getClans())
								{
									if (clan.getAllyCrestId() == crestId)
									{
										clan.setAllyCrestId(0);
										clan.changeAllyCrest(crest.getId(), false);
									}
								}
							}
						}
					}
					file.delete();
				}
				catch (final Exception e)
				{
					warn("There was an error while moving crest file " + file.getName() + " to database:", e);
				}
			}
			crestDir.delete();
		}
	}
	
	public Crest getCrest(int crestId)
	{
		return _crests.get(crestId);
	}
	
	public Crest createCrest(byte[] data, CrestType crestType)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO `crests`(`crest_id`, `data`, `type`) VALUES(?, ?, ?)");
			final var crest = new Crest(getNextId(), data, crestType);
			statement.setInt(1, crest.getId());
			statement.setBytes(2, crest.getData());
			statement.setInt(3, crest.getType().getId());
			statement.executeUpdate();
			_crests.put(crest.getId(), crest);
			return crest;
		}
		catch (final SQLException e)
		{
			warn("There was an error while saving crest in database:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return null;
	}
	
	public void removeCrest(int crestId)
	{
		_crests.remove(crestId);
		
		if (crestId == (_nextId.get() - 1))
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM `crests` WHERE `crest_id` = ?");
			statement.setInt(1, crestId);
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			warn("There was an error while deleting crest from database:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public int getNextId()
	{
		return _nextId.getAndIncrement();
	}
	
	public static CrestHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CrestHolder _instance = new CrestHolder();
	}
}