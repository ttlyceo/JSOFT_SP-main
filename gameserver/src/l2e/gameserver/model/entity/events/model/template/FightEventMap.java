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
package l2e.gameserver.model.entity.events.model.template;

import java.util.Map;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.zone.ZoneType;

/**
 * Created by LordWinter
 */
public class FightEventMap
{
	private final String _name;
	private final String[] _events;
	private final int[] _teamsCount;
	private final int _minAllPlayers;
	private final int _maxAllPlayers;
	private final Map<Integer, Location[]> _teamSpawns;
	private final Map<Integer, Map<String, ZoneType>> _territories;
	private final Map<Integer, Map<Integer, Location[]>> _npcWaypath;
	private final int[] _doors;
	private final Location[] _keyLocations;
	private final Location[] _defLocations;
	
	public FightEventMap(MultiValueSet<String> params, Map<Integer, Location[]> teamSpawns, Map<Integer, Map<String, ZoneType>> territories, Map<Integer, Map<Integer, Location[]>> npcWaypath, Location[] keyLocations, Location[] defLocations)
	{
		_name = params.getString("name");
		_events = params.getString("events").split(";");
		_minAllPlayers = Integer.parseInt(params.getString("minAllPlayers", "-1"));
		_maxAllPlayers = Integer.parseInt(params.getString("maxAllPlayers", "-1"));
		final String[] doorList = params.getString("doors", "0").split(";");
		_doors = new int[doorList.length];
		for (int i = 0; i < doorList.length; i++)
		{
			_doors[i] = Integer.parseInt(doorList[i]);
		}
		
		final String[] teamCounts = params.getString("teamsCount", "-1").split(";");
		_teamsCount = new int[teamCounts.length];
		for (int i = 0; i < teamCounts.length; i++)
		{
			_teamsCount[i] = Integer.parseInt(teamCounts[i]);
		}
		
		_teamSpawns = teamSpawns;
		_territories = territories;
		_npcWaypath = npcWaypath;
		_keyLocations = keyLocations;
		_defLocations = defLocations;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String[] getEvents()
	{
		return _events;
	}
	
	public int[] getTeamCount()
	{
		return _teamsCount;
	}
	
	public int getMinAllPlayers()
	{
		return _minAllPlayers;
	}
	
	public int getMaxAllPlayers()
	{
		return _maxAllPlayers;
	}
	
	public int[] getDoors()
	{
		return _doors;
	}
	
	public Map<Integer, Location[]> getTeamSpawns()
	{
		return _teamSpawns;
	}
	
	public Location[] getPlayerSpawns()
	{
		return _teamSpawns.get(-1);
	}
	
	public Map<Integer, Map<String, ZoneType>> getTerritories()
	{
		return _territories;
	}
	
	public Map<Integer, Map<Integer, Location[]>> getNpcWaypath()
	{
		return _npcWaypath;
	}
	
	public Location[] getKeyLocations()
	{
		return _keyLocations;
	}

	public Location[] getDefLocations()
	{
		return _defLocations;
	}
}