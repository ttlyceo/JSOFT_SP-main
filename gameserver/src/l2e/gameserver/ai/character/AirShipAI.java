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
package l2e.gameserver.ai.character;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.AirShipInstance;
import l2e.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import l2e.gameserver.network.serverpackets.ExStopMoveAirShip;

public class AirShipAI extends VehicleAI
{
	public AirShipAI(AirShipInstance accessor)
	{
		super(accessor);
	}
	
	@Override
	protected void moveTo(int x, int y, int z, int offset)
	{
		if (!_actor.isMovementDisabled())
		{
			_clientMoving = true;
			_actor.moveToLocation(x, y, z, offset);
			_actor.broadcastPacket(new ExMoveToLocationAirShip(getActor()));
		}
	}
	
	@Override
	protected void moveTo(Location loc, int offset)
	{
		if (!_actor.isMovementDisabled())
		{
			_clientMoving = true;
			_actor.moveToLocation(loc.getX(), loc.getY(), loc.getZ(), offset);
			_actor.broadcastPacket(new ExMoveToLocationAirShip(getActor()));
		}
	}

	@Override
	public void clientStopMoving(Location loc)
	{
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
		}

		if (_clientMoving || (loc != null))
		{
			_clientMoving = false;
			_actor.broadcastPacket(new ExStopMoveAirShip(getActor()));
		}
	}

	@Override
	public void describeStateToPlayer(Player player)
	{
		if (_clientMoving)
		{
			player.sendPacket(new ExMoveToLocationAirShip(getActor()));
		}
	}

	@Override
	public AirShipInstance getActor()
	{
		return (AirShipInstance) _actor;
	}
}