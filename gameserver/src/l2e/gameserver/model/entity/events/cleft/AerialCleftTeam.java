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
package l2e.gameserver.model.entity.events.cleft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import l2e.commons.util.Rnd;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter 03.12.2018
 */
public class AerialCleftTeam
{
	private final String _name;
	private final int _teamId;
	private Location[] _coordinates = new Location[3];
	private Location[] _exitLoc = new Location[3];
	private short _points;
	private final Map<Integer, Player> _participatedPlayers = new ConcurrentHashMap<>();
	private final Map<Integer, Long> _participatedTimes = new ConcurrentHashMap<>();
	List<Player> _playersList = new ArrayList<>();
	private Player _teamCat;

	public AerialCleftTeam(String name, int teamId, Location[] coordinates, Location[] exitLoc)
	{
		_name = name;
		_teamId = teamId;
		_coordinates = coordinates;
		_exitLoc = exitLoc;
		_points = 0;
	}

	public void addPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}

		_playersList.add(player);
		
		synchronized (_participatedPlayers)
		{
			_participatedPlayers.put(player.getObjectId(), player);
		}
	}

	public void startEventTime(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		synchronized (_participatedTimes)
		{
			_participatedTimes.put(player.getObjectId(), System.currentTimeMillis());
		}
	}
	
	public void removePlayer(int objectId)
	{
		synchronized (_participatedPlayers)
		{
			_participatedPlayers.remove(objectId);
		}
	}
	
	public void removePlayerFromList(Player player)
	{
		_playersList.remove(player);
	}
	
	public void removePlayerTime(int objectId)
	{
		synchronized (_participatedTimes)
		{
			_participatedTimes.remove(objectId);
		}
	}
	
	public void addPoints(int count)
	{
		_points += count;
	}
	
	public void cleanMe()
	{
		_participatedPlayers.clear();
		_participatedTimes.clear();
		_points = 0;
		_teamCat = null;
		_playersList.clear();
	}
	
	public boolean containsPlayer(int playerObjectId)
	{
		final boolean containsPlayer;
		
		synchronized (_participatedPlayers)
		{
			containsPlayer = _participatedPlayers.containsKey(playerObjectId);
		}
		return containsPlayer;
	}

	public boolean containsTime(int playerObjectId)
	{
		final boolean containsPlayer;
		
		synchronized (_participatedTimes)
		{
			containsPlayer = _participatedTimes.containsKey(playerObjectId);
		}
		return containsPlayer;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getId()
	{
		return _teamId;
	}
	
	public Location[] getLocations()
	{
		return _coordinates;
	}

	public Location[] getExitLocations()
	{
		return _exitLoc;
	}
	
	public short getPoints()
	{
		return _points;
	}

	public Player getTeamCat()
	{
		return _teamCat;
	}
	
	public Map<Integer, Player> getParticipatedPlayers()
	{
		Map<Integer, Player> participatedPlayers = null;
		synchronized (_participatedPlayers)
		{
			participatedPlayers = _participatedPlayers;
		}
		return participatedPlayers;
	}
	
	public Map<Integer, Long> getParticipatedTimes()
	{
		Map<Integer, Long> participatedPlayers = null;
		synchronized (_participatedTimes)
		{
			participatedPlayers = _participatedTimes;
		}
		return participatedPlayers;
	}
	
	public int getParticipatedPlayerCount()
	{
		final int participatedPlayerCount;
		synchronized (_participatedPlayers)
		{
			participatedPlayerCount = _participatedPlayers.size();
		}
		return participatedPlayerCount;
	}

	public void selectTeamCat()
	{
		if (!_playersList.isEmpty())
		{
			final Player targetPlayer = _playersList.get(Rnd.get(_playersList.size()));
			if (targetPlayer != null)
			{
				_teamCat = targetPlayer;
				targetPlayer.setCleftCat(true);
			}
		}
	}
}