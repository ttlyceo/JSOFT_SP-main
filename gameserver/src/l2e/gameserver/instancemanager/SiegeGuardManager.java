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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.spawn.Spawner;

public class SiegeGuardManager extends LoggerObject
{
	private final Castle _castle;
	private final List<Spawner> _siegeGuardSpawn = new ArrayList<>();

	public SiegeGuardManager(Castle castle)
	{
		_castle = castle;
	}
	
	public void addSiegeGuard(Player activeChar, int npcId)
	{
		if (activeChar == null)
		{
			return;
		}
		addSiegeGuard(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
	}

	public void addSiegeGuard(int x, int y, int z, int heading, int npcId)
	{
		saveSiegeGuard(x, y, z, heading, npcId, 0);
	}

	public void hireMerc(Player activeChar, int npcId)
	{
		if (activeChar == null)
		{
			return;
		}
		hireMerc(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
	}

	public void hireMerc(int x, int y, int z, int heading, int npcId)
	{
		saveSiegeGuard(x, y, z, heading, npcId, 1);
	}

	public void removeMerc(int npcId, int x, int y, int z)
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("Delete From castle_siege_guards Where npcId = ? And x = ? AND y = ? AND z = ? AND isHired = 1");
			statement.setInt(1, npcId);
			statement.setInt(2, x);
			statement.setInt(3, y);
			statement.setInt(4, z);
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Error deleting hired siege guard at " + x + ',' + y + ',' + z + ": " + e.getMessage(), e);
		}
	}

	public void removeMercs()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("Delete From castle_siege_guards Where castleId = ? And isHired = 1");
			statement.setInt(1, getCastle().getId());
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Error deleting hired siege guard for castle " + getCastle().getName(null) + ": " + e.getMessage(), e);
		}
	}

	public void spawnSiegeGuard()
	{
		try
		{
			int hiredCount = 0;
			final int hiredMax = MercTicketManager.getInstance().getMaxAllowedMerc(_castle.getId());
			final boolean isHired = (getCastle().getOwnerId() > 0) ? true : false;
			loadSiegeGuard();
			for (final Spawner spawn : getSiegeGuardSpawn())
			{
				if (spawn != null)
				{
					spawn.init();
					if (isHired)
					{
						spawn.stopRespawn();
						if (++hiredCount > hiredMax)
						{
							return;
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error spawning siege guards for castle " + getCastle().getName(null), e);
		}
	}

	public void unspawnSiegeGuard()
	{
		for (final Spawner spawn : getSiegeGuardSpawn())
		{
			if ((spawn != null) && (spawn.getLastSpawn() != null))
			{
				spawn.stopRespawn();
				spawn.getLastSpawn().doDie(spawn.getLastSpawn());
			}
		}
		getSiegeGuardSpawn().clear();
	}

	private void loadSiegeGuard()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT * FROM castle_siege_guards Where castleId = ? And isHired = ?");
			statement.setInt(1, getCastle().getId());
			if (getCastle().getOwnerId() > 0)
			{
				statement.setInt(2, 1);
			}
			else
			{
				statement.setInt(2, 0);
			}
			final ResultSet rs = statement.executeQuery();

			Spawner spawn1;
			NpcTemplate template1;

			while (rs.next())
			{
				template1 = NpcsParser.getInstance().getTemplate(rs.getInt("npcId"));
				if (template1 != null)
				{
					spawn1 = new Spawner(template1);
					spawn1.setAmount(1);
					spawn1.setX(rs.getInt("x"));
					spawn1.setY(rs.getInt("y"));
					spawn1.setZ(rs.getInt("z"));
					spawn1.setHeading(rs.getInt("heading"));
					spawn1.setRespawnDelay(rs.getInt("respawnDelay"));
					spawn1.setLocationId(0);

					_siegeGuardSpawn.add(spawn1);
				}
				else
				{
					warn("Missing npc data in npc table for id: " + rs.getInt("npcId"));
				}
			}
			rs.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Error loading siege guard for castle " + getCastle().getName(null) + ": " + e.getMessage(), e);
		}
	}

	private void saveSiegeGuard(int x, int y, int z, int heading, int npcId, int isHire)
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("Insert Into castle_siege_guards (castleId, npcId, x, y, z, heading, respawnDelay, isHired) Values (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, getCastle().getId());
			statement.setInt(2, npcId);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.setInt(6, heading);
			statement.setInt(7, (isHire == 1 ? 0 : 600));
			statement.setInt(8, isHire);
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Error adding siege guard for castle " + getCastle().getName(null) + ": " + e.getMessage(), e);
		}
	}

	public final Castle getCastle()
	{
		return _castle;
	}

	public final List<Spawner> getSiegeGuardSpawn()
	{
		return _siegeGuardSpawn;
	}
}