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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;

public final class CanNotMoveAnymore extends GameClientPacket
{
	private final Location _loc = new Location();
	
	@Override
	protected void readImpl()
	{
		_loc.setX(readD());
		_loc.setY(readD());
		_loc.setZ(readD());
		_loc.setHeading(readD());
	}

	@Override
	protected void runImpl()
	{
		final var player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}

		if (Config.DEBUG)
		{
			_log.info("client: " + _loc.toString() + " server: " + player.getLocation().toString());
		}

		if (player.getAI() != null)
		{
			player.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED_BLOCKED, GeoEngine.getInstance().isNSWEAll(_loc, 60) ? _loc : player.getLocation());
		}
	}
}