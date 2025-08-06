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
package l2e.gameserver.model.entity.underground_coliseum;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;

public class UCPoint
{
	private final Location _loc;
	private final List<DoorInstance> _doors;
	private final List<Player> _players = new ArrayList<>();
	
	public UCPoint(List<DoorInstance> doors, Location loc)
	{
		_doors = doors;
		_loc = loc;
	}
	
	public void teleportPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		player.setSaveLoc(player.getLocation());

		if (player.isDead())
		{
			UCTeam.resPlayer(player);
		}
		
		final Location pos = Location.findPointToStay(_loc, 350, true);
		player.teleToLocation(pos, true, player.getReflection());
		_players.add(player);
	}

	public void actionDoors(boolean open)
	{
		if (_doors.isEmpty())
		{
			return;
		}
		
		for (final DoorInstance door : _doors)
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}

	public Location getLocation()
	{
		return _loc;
	}
	
	public List<Player> getPlayers()
	{
		return _players;
	}
	
	public boolean checkPlayer(Player player)
	{
		if (_players.contains(player))
		{
			actionDoors(true);
			for (final Player pl : _players)
			{
				if (pl != null)
				{
					pl.setUCState(Player.UC_STATE_ARENA);
				}
			}
			return true;
		}
		return false;
	}
}