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
package l2e.gameserver.model.actor.instance;

import l2e.gameserver.ai.character.BoatAI;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Vehicle;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.VehicleDeparture;
import l2e.gameserver.network.serverpackets.VehicleInfo;
import l2e.gameserver.network.serverpackets.VehicleStarted;

public class BoatInstance extends Vehicle
{
	public BoatInstance(int objectId, CharTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.BoatInstance);
		setAI(new BoatAI(this));
	}

	@Override
	public boolean isBoat()
	{
		return true;
	}

	@Override
	public int getId()
	{
		return 0;
	}

	@Override
	public boolean moveToNextRoutePoint()
	{
		final boolean result = super.moveToNextRoutePoint();
		if (result)
		{
			broadcastPacket(new VehicleDeparture(this));
		}
		return result;
	}
	
	@Override
	public void oustPlayer(Player player)
	{
		super.oustPlayer(player);
		
		final Location loc = getOustLoc();
		if (player.isOnline())
		{
			player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, ReflectionManager.DEFAULT);
		}
		else
		{
			player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
		}
	}
	
	@Override
	public void stopMove(Location loc)
	{
		super.stopMove(loc);
		
		broadcastPacket(new VehicleStarted(this, 0));
		broadcastPacket(new VehicleInfo(this));
	}
	
	@Override
	public void sendInfo(Player activeChar)
	{
		activeChar.sendPacket(new VehicleInfo(this));
	}
	
	@Override
	public GameServerPacket infoPacket()
	{
		return new VehicleInfo(this);
	}
}