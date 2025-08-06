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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.instance.FortBallistaInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.spawn.Spawner;

public final class FortSiegeGuardManager extends LoggerObject
{
	private final Fort _fort;
	private final Map<Integer, List<Spawner>> _siegeGuards = new HashMap<>();
	
	public FortSiegeGuardManager(Fort fort)
	{
		_fort = fort;
	}
	
	public void spawnSiegeGuard()
	{
		try
		{
			final List<Spawner> monsterList = _siegeGuards.get(getFort().getId());
			if (monsterList != null)
			{
				for (final Spawner spawnDat : monsterList)
				{
					spawnDat.doSpawn();
					if (spawnDat.getLastSpawn() instanceof FortBallistaInstance)
					{
						spawnDat.stopRespawn();
					}
					else
					{
						spawnDat.startRespawn();
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error spawning siege guards for fort " + getFort().getName() + ":" + e.getMessage(), e);
		}
	}

	public void unspawnSiegeGuard()
	{
		try
		{
			final List<Spawner> monsterList = _siegeGuards.get(getFort().getId());
			if (monsterList != null)
			{
				for (final Spawner spawnDat : monsterList)
				{
					spawnDat.stopRespawn();
					if (spawnDat.getLastSpawn() != null)
					{
						spawnDat.getLastSpawn().doDie(spawnDat.getLastSpawn());
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error unspawning siege guards for fort " + getFort().getName() + ":" + e.getMessage(), e);
		}
	}

	void loadSiegeGuard()
	{
		_siegeGuards.clear();
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); PreparedStatement ps = con.prepareStatement("SELECT npcId, x, y, z, heading, respawnDelay FROM fort_siege_guards WHERE fortId = ?"))
		{
			final int fortId = getFort().getId();
			ps.setInt(1, fortId);
			try (
			    ResultSet rs = ps.executeQuery())
			{
				final List<Spawner> siegeGuardSpawns = new ArrayList<>();
				while (rs.next())
				{
					final NpcTemplate template = NpcsParser.getInstance().getTemplate(rs.getInt("npcId"));
					if (template != null)
					{
						final Spawner spawn = new Spawner(template);
						spawn.setAmount(1);
						spawn.setX(rs.getInt("x"));
						spawn.setY(rs.getInt("y"));
						spawn.setZ(rs.getInt("z"));
						spawn.setHeading(rs.getInt("heading"));
						spawn.setRespawnDelay(rs.getInt("respawnDelay"));
						spawn.setLocationId(0);
						siegeGuardSpawns.add(spawn);
					}
				}
				_siegeGuards.put(fortId, siegeGuardSpawns);
			}
		}
		catch (final Exception e)
		{
			warn("Error loading siege guard for fort " + getFort().getName() + ": " + e.getMessage(), e);
		}
	}

	public final Fort getFort()
	{
		return _fort;
	}

	public final Map<Integer, List<Spawner>> getSiegeGuardSpawn()
	{
		return _siegeGuards;
	}
}