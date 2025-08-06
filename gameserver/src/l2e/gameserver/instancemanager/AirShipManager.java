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
import java.util.HashMap;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.AirShipInstance;
import l2e.gameserver.model.actor.instance.ControllableAirShipInstance;
import l2e.gameserver.model.actor.templates.AirShipTeleportTemplate;
import l2e.gameserver.model.actor.templates.VehicleTemplate;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.serverpackets.ExAirShipTeleportList;

public class AirShipManager extends LoggerObject
{
	private static final String LOAD_DB = "SELECT * FROM airships";
	private static final String ADD_DB = "INSERT INTO airships (owner_id,fuel) VALUES (?,?)";
	private static final String UPDATE_DB = "UPDATE airships SET fuel=? WHERE owner_id=?";
	
	private CharTemplate _airShipTemplate = null;
	private final Map<Integer, StatsSet> _airShipsInfo = new HashMap<>();
	private final Map<Integer, AirShipInstance> _airShips = new HashMap<>();
	private final Map<Integer, AirShipTeleportTemplate> _teleports = new HashMap<>();
	
	protected AirShipManager()
	{
		final StatsSet npcDat = new StatsSet();
		npcDat.set("npcId", 9);
		npcDat.set("level", 0);
		npcDat.set("jClass", "boat");
		
		npcDat.set("baseSTR", 0);
		npcDat.set("baseCON", 0);
		npcDat.set("baseDEX", 0);
		npcDat.set("baseINT", 0);
		npcDat.set("baseWIT", 0);
		npcDat.set("baseMEN", 0);
		
		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseAccCombat", 38);
		npcDat.set("baseEvasRate", 38);
		npcDat.set("baseCritRate", 38);
		
		npcDat.set("collision_radius", 0);
		npcDat.set("collision_height", 0);
		npcDat.set("sex", "male");
		npcDat.set("type", "");
		npcDat.set("baseAtkRange", 0);
		npcDat.set("baseMpMax", 0);
		npcDat.set("baseCpMax", 0);
		npcDat.set("rewardExp", 0);
		npcDat.set("rewardSp", 0);
		npcDat.set("basePAtk", 0);
		npcDat.set("baseMAtk", 0);
		npcDat.set("basePAtkSpd", 0);
		npcDat.set("aggroRange", 0);
		npcDat.set("baseMAtkSpd", 0);
		npcDat.set("rhand", 0);
		npcDat.set("lhand", 0);
		npcDat.set("armor", 0);
		npcDat.set("baseWalkSpd", 0);
		npcDat.set("baseRunSpd", 0);
		npcDat.set("name", "AirShip");
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseHpReg", 3.0);
		npcDat.set("baseMpReg", 3.0);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);
		_airShipTemplate = new CharTemplate(npcDat);
		
		load();
	}
	
	public AirShipInstance getNewAirShip(int x, int y, int z, int heading)
	{
		final AirShipInstance airShip = new AirShipInstance(IdFactory.getInstance().getNextId(), _airShipTemplate);
		
		airShip.setHeading(heading);
		airShip.setXYZInvisible(x, y, z);
		airShip.spawnMe();
		airShip.getStat().setMoveSpeed(280);
		airShip.getStat().setRotationSpeed(2000);
		return airShip;
	}
	
	public AirShipInstance getNewAirShip(int x, int y, int z, int heading, int ownerId)
	{
		final StatsSet info = _airShipsInfo.get(ownerId);
		if (info == null)
		{
			return null;
		}
		
		final AirShipInstance airShip;
		if (_airShips.containsKey(ownerId))
		{
			airShip = _airShips.get(ownerId);
			airShip.refreshID();
		}
		else
		{
			airShip = new ControllableAirShipInstance(IdFactory.getInstance().getNextId(), _airShipTemplate, ownerId);
			_airShips.put(ownerId, airShip);
			
			airShip.setMaxFuel(600);
			airShip.setFuel(info.getInteger("fuel"));
			airShip.getStat().setMoveSpeed(280);
			airShip.getStat().setRotationSpeed(2000);
		}
		
		airShip.setHeading(heading);
		airShip.setXYZInvisible(x, y, z);
		airShip.spawnMe();
		return airShip;
	}
	
	public void removeAirShip(AirShipInstance ship)
	{
		if (ship.getOwnerId() != 0)
		{
			storeInDb(ship.getOwnerId());
			final StatsSet info = _airShipsInfo.get(ship.getOwnerId());
			if (info != null)
			{
				info.set("fuel", ship.getFuel());
			}
		}
	}
	
	public boolean hasAirShipLicense(int ownerId)
	{
		return _airShipsInfo.containsKey(ownerId);
	}
	
	public void registerLicense(int ownerId)
	{
		if (!_airShipsInfo.containsKey(ownerId))
		{
			final StatsSet info = new StatsSet();
			info.set("fuel", 600);
			
			_airShipsInfo.put(ownerId, info);
			
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(ADD_DB);
				statement.setInt(1, ownerId);
				statement.setInt(2, info.getInteger("fuel"));
				statement.executeUpdate();
			}
			catch (final SQLException e)
			{
				warn("Could not add new airship license: " + e.getMessage(), e);
			}
			catch (final Exception e)
			{
				warn("Error while initializing: " + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}
	
	public boolean hasAirShip(int ownerId)
	{
		final AirShipInstance ship = _airShips.get(ownerId);
		if ((ship == null) || !(ship.isVisible() || ship.isTeleporting()))
		{
			return false;
		}
		return true;
	}
	
	public void registerAirShipTeleportList(int dockId, int locationId, VehicleTemplate[][] tp, int[] fuelConsumption)
	{
		if (tp.length != fuelConsumption.length)
		{
			return;
		}
		_teleports.put(dockId, new AirShipTeleportTemplate(locationId, fuelConsumption, tp));
	}
	
	public void sendAirShipTeleportList(Player player)
	{
		if ((player == null) || !player.isInAirShip())
		{
			return;
		}
		
		final AirShipInstance ship = player.getAirShip();
		if (!ship.isCaptain(player) || !ship.isInDock() || ship.isMoving())
		{
			return;
		}
		
		final int dockId = ship.getDockId();
		if (!_teleports.containsKey(dockId))
		{
			return;
		}
		final AirShipTeleportTemplate all = _teleports.get(dockId);
		player.sendPacket(new ExAirShipTeleportList(all.getLocation(), all.getRoute(), all.getFuel()));
	}
	
	public VehicleTemplate[] getTeleportDestination(int dockId, int index)
	{
		final AirShipTeleportTemplate all = _teleports.get(dockId);
		if (all == null)
		{
			return null;
		}
		
		if ((index < -1) || (index >= all.getRoute().length))
		{
			return null;
		}
		return all.getRoute()[index + 1];
	}
	
	public int getFuelConsumption(int dockId, int index)
	{
		final AirShipTeleportTemplate all = _teleports.get(dockId);
		if (all == null)
		{
			return 0;
		}
		
		if ((index < -1) || (index >= all.getFuel().length))
		{
			return 0;
		}
		return all.getFuel()[index + 1];
	}
	
	private void load()
	{
		Connection con = null;
		Statement s = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			s = con.createStatement();
			rs = s.executeQuery(LOAD_DB);
			StatsSet info;
			while (rs.next())
			{
				info = new StatsSet();
				info.set("fuel", rs.getInt("fuel"));
				_airShipsInfo.put(rs.getInt("owner_id"), info);
			}
		}
		catch (final SQLException e)
		{
			warn("Could not load airships table: " + e.getMessage(), e);
		}
		catch (final Exception e)
		{
			warn("Error while initializing: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, s, rs);
		}
		info("Loaded " + _airShipsInfo.size() + " private airships.");
	}
	
	private void storeInDb(int ownerId)
	{
		final StatsSet info = _airShipsInfo.get(ownerId);
		if (info == null)
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement ps = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement(UPDATE_DB);
			ps.setInt(1, info.getInteger("fuel"));
			ps.setInt(2, ownerId);
			ps.executeUpdate();
		}
		catch (final SQLException e)
		{
			warn("Could not update airships table: " + e.getMessage(), e);
		}
		catch (final Exception e)
		{
			warn("Error while save: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, ps);
		}
	}
	
	public static final AirShipManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AirShipManager _instance = new AirShipManager();
	}
}