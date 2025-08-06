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

import l2e.gameserver.ai.character.AirShipAI;
import l2e.gameserver.instancemanager.AirShipManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Vehicle;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.network.serverpackets.ExAirShipInfo;
import l2e.gameserver.network.serverpackets.ExGetOffAirShip;
import l2e.gameserver.network.serverpackets.ExGetOnAirShip;
import l2e.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import l2e.gameserver.network.serverpackets.ExStopMoveAirShip;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class AirShipInstance extends Vehicle
{
	public AirShipInstance(int objectId, CharTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.AirShipInstance);
		setAI(new AirShipAI(this));
	}
	
	@Override
	public boolean isAirShip()
	{
		return true;
	}
	
	public boolean isOwner(Player player)
	{
		return false;
	}
	
	public int getOwnerId()
	{
		return 0;
	}
	
	public boolean isCaptain(Player player)
	{
		return false;
	}
	
	public int getCaptainId()
	{
		return 0;
	}
	
	public int getHelmObjectId()
	{
		return 0;
	}
	
	public int getHelmItemId()
	{
		return 0;
	}
	
	public boolean setCaptain(Player player)
	{
		return false;
	}
	
	public int getFuel()
	{
		return 0;
	}
	
	public void setFuel(int f)
	{
		
	}
	
	public int getMaxFuel()
	{
		return 0;
	}
	
	public void setMaxFuel(int mf)
	{
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
			broadcastPacket(new ExMoveToLocationAirShip(this));
		}
		
		return result;
	}
	
	@Override
	public boolean addPassenger(Player player)
	{
		if (!super.addPassenger(player))
		{
			return false;
		}
		
		player.setVehicle(this);
		player.setInVehiclePosition(new Location());
		player.broadcastPacket(new ExGetOnAirShip(player, this));
		player.setXYZ(getX(), getY(), getZ());
		player.refreshInfos();
		player.revalidateZone(true);
		return true;
	}
	
	@Override
	public void oustPlayer(Player player)
	{
		super.oustPlayer(player);
		final Location loc = getOustLoc();
		if (player.isOnline())
		{
			player.broadcastPacket(new ExGetOffAirShip(player, this, loc.getX(), loc.getY(), loc.getZ()));
			player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, false, ReflectionManager.DEFAULT);
		}
		else
		{
			player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
		}
	}
	
	@Override
	protected void onDelete()
	{
		super.onDelete();
		AirShipManager.getInstance().removeAirShip(this);
	}
	
	@Override
	public void stopMove(Location loc)
	{
		super.stopMove(loc);
		broadcastPacket(new ExStopMoveAirShip(this));
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		broadcastPacket(new ExAirShipInfo(this));
	}
	
	@Override
	public void sendInfo(Player activeChar)
	{
		if (isVisibleFor(activeChar))
		{
			activeChar.sendPacket(new ExAirShipInfo(this));
		}
	}
	
	@Override
	public GameServerPacket infoPacket()
	{
		return new ExAirShipInfo(this);
	}
}