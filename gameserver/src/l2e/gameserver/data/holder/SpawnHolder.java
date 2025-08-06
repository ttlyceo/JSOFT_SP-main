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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ClassMasterParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.DayNightSpawnManager;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.spawn.Spawner;

public class SpawnHolder extends LoggerObject
{
	protected SpawnHolder()
	{
		if (!Config.ALT_DEV_NO_SPAWNS)
		{
			fillSpawnTable();
		}
	}

	private void fillSpawnTable()
	{
		var amount = 0;
		if (!Config.SAVE_GMSPAWN_ON_CUSTOM)
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT count, npc_templateid, locx, locy, locz, heading, respawn_delay, respawn_random, loc_id, periodOfDay FROM spawnlist");
			rset = statement.executeQuery();

			Spawner spawnDat;
			NpcTemplate template1;

			while (rset.next())
			{
				template1 = NpcsParser.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					if (template1.isType("SiegeGuard"))
					{}
					else if (template1.isType("RaidBoss"))
					{}
					else if (!ClassMasterParser.getInstance().isAllowClassMaster() && template1.isType("ClassMaster"))
					{}
					else if (Config.ALT_CHEST_NO_SPAWNS && template1.isType("TreasureChest"))
					{}
					else
					{
						spawnDat = new Spawner(template1);
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setX(rset.getInt("locx"));
						spawnDat.setY(rset.getInt("locy"));
						spawnDat.setZ(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"), rset.getInt("respawn_random"));
						final var loc_id = rset.getInt("loc_id");
						spawnDat.setLocationId(loc_id);
						if (spawnDat.getRespawnDelay() == 0)
						{
							spawnDat.stopRespawn();
						}
						else
						{
							spawnDat.startRespawn();
						}
						spawnDat.setFromDatabase(true);
						switch (rset.getInt("periodOfDay"))
						{
							case 0 :
								spawnDat.doSpawn();
								break;
							case 1 :
								DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
								break;
							case 2 :
								DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
								break;
						}
						SpawnParser.getInstance().addNewSpawn(spawnDat);
						amount++;
					}
				}
				else
				{
					warn("Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Spawn could not be initialized: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (amount > 0)
		{
			info("Loaded " + amount + " npc spawns from database.");
		}
	}

	public void addNewSpawn(Spawner spawn, boolean storeInDb)
	{
		SpawnParser.getInstance().addNewSpawn(spawn);
		spawn.setFromDatabase(true);
		if (storeInDb)
		{
			Connection con = null;
			PreparedStatement insert = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				insert = con.prepareStatement("INSERT INTO spawnlist (count,npc_templateid,locx,locy,locz,heading,respawn_delay,respawn_random,loc_id) values(?,?,?,?,?,?,?,?,?)");
				insert.setInt(1, spawn.getAmount());
				insert.setInt(2, spawn.getId());
				insert.setInt(3, spawn.getX());
				insert.setInt(4, spawn.getY());
				insert.setInt(5, spawn.getZ());
				insert.setInt(6, spawn.getHeading());
				insert.setInt(7, spawn.getRespawnDelay() / 1000);
				insert.setInt(8, spawn.getRespawnMaxDelay() - spawn.getRespawnMinDelay());
				insert.setInt(9, spawn.getLocationId());
				insert.execute();
			}
			catch (final Exception e)
			{
				warn("Could not store spawn in the DB:" + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, insert);
			}
		}
	}

	public void deleteSpawn(Spawner spawn, boolean updateDb)
	{
		if (!SpawnParser.getInstance().deleteSpawn(spawn))
		{
			return;
		}
		
		if (spawn.getLocation() != null && updateDb && spawn.isFromDatabase())
		{
			Connection con = null;
			PreparedStatement delete = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				delete = con.prepareStatement("DELETE FROM spawnlist WHERE locx=? AND locy=? AND heading=? AND npc_templateid=?");
				delete.setInt(1, spawn.getLocation().getX());
				delete.setInt(2, spawn.getLocation().getY());
				delete.setInt(3, spawn.getLocation().getHeading());
				delete.setInt(4, spawn.getId());
				delete.execute();
				info("Deleted npcId - " + spawn.getId() + " from spawnlist.");
			}
			catch (final Exception e)
			{
				warn("Spawn " + spawn + " could not be removed from DB: " + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, delete);
			}
		}
	}

	public void reloadAll()
	{
		fillSpawnTable();
	}

	public static SpawnHolder getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final SpawnHolder _instance = new SpawnHolder();
	}
}