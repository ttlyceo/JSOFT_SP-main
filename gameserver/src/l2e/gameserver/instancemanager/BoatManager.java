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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.BoatInstance;
import l2e.gameserver.model.actor.templates.VehicleTemplate;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class BoatManager extends LoggerObject
{
	private final Map<Integer, BoatInstance> _boats = new ConcurrentHashMap<>();
	private final boolean[] _docksBusy = new boolean[3];

	public static final int TALKING_ISLAND = 1;
	public static final int GLUDIN_HARBOR = 2;
	public static final int RUNE_HARBOR = 3;
	
	public static final BoatManager getInstance()
	{
		return SingletonHolder._instance;
	}

	protected BoatManager()
	{
		for (int i = 0; i < _docksBusy.length; i++)
		{
			_docksBusy[i] = false;
		}
		if (Config.ALLOW_BOAT)
		{
			info("Loaded all ship functions.");
		}
	}

	public BoatInstance getNewBoat(int boatId, int x, int y, int z, int heading)
	{
		if (!Config.ALLOW_BOAT)
		{
			return null;
		}

		final StatsSet npcDat = new StatsSet();
		npcDat.set("npcId", boatId);
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
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseHpReg", 3.0);
		npcDat.set("baseMpReg", 3.0);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);
		final CharTemplate template = new CharTemplate(npcDat);
		final BoatInstance boat = new BoatInstance(IdFactory.getInstance().getNextId(), template);
		_boats.put(boat.getObjectId(), boat);
		boat.setHeading(heading);
		boat.setXYZInvisible(x, y, z);
		boat.spawnMe();
		return boat;
	}

	public BoatInstance getBoat(int boatId)
	{
		return _boats.get(boatId);
	}

	public void dockShip(int h, boolean value)
	{
		try
		{
			_docksBusy[h] = value;
		}
		catch (final ArrayIndexOutOfBoundsException e)
		{}
	}

	public boolean dockBusy(int h)
	{
		try
		{
			return _docksBusy[h];
		}
		catch (final ArrayIndexOutOfBoundsException e)
		{
			return false;
		}
	}

	public void broadcastPacket(VehicleTemplate point1, VehicleTemplate point2, GameServerPacket packet)
	{
		broadcastPacketsToPlayers(point1, point2, packet);
	}

	public void broadcastPackets(VehicleTemplate point1, VehicleTemplate point2, GameServerPacket... packets)
	{
		broadcastPacketsToPlayers(point1, point2, packets);
	}
	
	private void broadcastPacketsToPlayers(VehicleTemplate point1, VehicleTemplate point2, GameServerPacket... packets)
	{
		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				double dx = (double) player.getX() - point1.getX();
				double dy = (double) player.getY() - point1.getY();
				if (Math.sqrt((dx * dx) + (dy * dy)) < Config.BOAT_BROADCAST_RADIUS)
				{
					for (final GameServerPacket p : packets)
					{
						player.sendPacket(p);
					}
				}
				else
				{
					dx = (double) player.getX() - point2.getX();
					dy = (double) player.getY() - point2.getY();
					if (Math.sqrt((dx * dx) + (dy * dy)) < Config.BOAT_BROADCAST_RADIUS)
					{
						for (final GameServerPacket p : packets)
						{
							player.sendPacket(p);
						}
					}
				}
			}
		}
	}

	private static class SingletonHolder
	{
		protected static final BoatManager _instance = new BoatManager();
	}
}