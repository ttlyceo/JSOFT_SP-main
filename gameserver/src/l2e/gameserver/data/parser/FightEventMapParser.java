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
package l2e.gameserver.data.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.entity.events.model.template.FightEventMap;
import l2e.gameserver.model.zone.ZoneType;

public final class FightEventMapParser extends DocumentParser
{
	private final List<FightEventMap> _maps = new ArrayList<>();

	protected FightEventMapParser()
	{
		load();
	}
	
	public void reload()
	{
		load();
	}

	@Override
	public final void load()
	{
		_maps.clear();
		parseDirectory("data/stats/events/maps", false);
		info("Loaded " + _maps.size() + " map templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node c = getCurrentDocument().getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("list".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node map = c.getFirstChild(); map != null; map = map.getNextSibling())
				{
					if ("map".equalsIgnoreCase(map.getNodeName()))
					{
						NamedNodeMap attrs = map.getAttributes();

						Map<Integer, Location[]> teamSpawns = null;
						Map<Integer, Map<String, ZoneType>> territories = null;
						Map<Integer, Map<Integer, Location[]>> npcWaypath = null;
						Location[] keyLocations = null;
						Location[] defLocations = null;

						final String name = attrs.getNamedItem("name").getNodeValue();
						final MultiValueSet<String> set = new MultiValueSet<>();
						set.set("name", name);
						for (Node par = map.getFirstChild(); par != null; par = par.getNextSibling())
						{
							if ("parameter".equalsIgnoreCase(par.getNodeName()))
							{
								attrs = par.getAttributes();
								set.set(attrs.getNamedItem("name").getNodeValue(), attrs.getNamedItem("value").getNodeValue());
							}
							else if ("objects".equalsIgnoreCase(par.getNodeName()))
							{
								attrs = par.getAttributes();
								final String objectsName = attrs.getNamedItem("name").getNodeValue();

								final int team = attrs.getNamedItem("team") != null ? Integer.parseInt(attrs.getNamedItem("team").getNodeValue()) : -1;
								final int index = attrs.getNamedItem("index") != null ? Integer.parseInt(attrs.getNamedItem("index").getNodeValue()) : -1;
								if (objectsName.equals("teamSpawns"))
								{
									if (teamSpawns == null)
									{
										teamSpawns = new HashMap<>();
									}
									teamSpawns.put(team, parseLocations(par));
								}
								else if (objectsName.equals("territory"))
								{
									if (territories == null)
									{
										territories = new HashMap<>();
									}
									territories.put(team, parseTerritory(par));
								}
								else if (objectsName.equals("npcWaypath"))
								{
									if (npcWaypath == null)
									{
										npcWaypath = new HashMap<>();
									}
									
									if (npcWaypath.get(team) == null)
									{
										npcWaypath.put(team, new HashMap<>());
									}
									npcWaypath.get(team).put(index, parseLocations(par));
								}
								else if (objectsName.equals("keyLocations"))
								{
									keyLocations = parseLocations(par);
								}
								else if (objectsName.equals("defLocations"))
								{
									defLocations = parseLocations(par);
								}
							}
						}
						addMap(new FightEventMap(set, teamSpawns, territories, npcWaypath, keyLocations, defLocations));
					}
				}
			}
		}
	}

	private Location[] parseLocations(Node node)
	{
		final List<Location> locs = new ArrayList<>();
		for (Node loc = node.getFirstChild(); loc != null; loc = loc.getNextSibling())
		{
			if ("point".equalsIgnoreCase(loc.getNodeName()))
			{
				final NamedNodeMap attrs = loc.getAttributes();

				final int x = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
				final int y = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
				final int z = Integer.parseInt(attrs.getNamedItem("z").getNodeValue());
				locs.add(new Location(x, y, z));
			}
		}
		
		final Location[] locArray = new Location[locs.size()];
		
		for (int i = 0; i < locs.size(); i++)
		{
			locArray[i] = locs.get(i);
		}
		return locArray;
	}

	private Map<String, ZoneType> parseTerritory(Node node)
	{
		final Map<String, ZoneType> territories = new HashMap<>();
		for (Node zone = node.getFirstChild(); zone != null; zone = zone.getNextSibling())
		{
			if ("zone".equalsIgnoreCase(zone.getNodeName()))
			{
				final NamedNodeMap attrs = zone.getAttributes();
				final int zoneId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
				final ZoneType type = ZoneManager.getInstance().getZoneById(zoneId);
				if (type != null)
				{
					territories.put(type.getName(), type);
				}
				else
				{
					warn("Unable to find zoneId: " + zoneId + "");
				}
			}
		}
		return territories;
	}

	public void addMap(FightEventMap map)
	{
		_maps.add(map);
	}

	public List<FightEventMap> getMapsForEvent(String eventName)
	{
		final List<FightEventMap> maps = new ArrayList<>();
		for (final FightEventMap map : _maps)
		{
			for (final String possibleName : map.getEvents())
			{
				if (possibleName.equalsIgnoreCase(eventName))
				{
					maps.add(map);
				}
			}
		}
		return maps;
	}

	public List<Integer> getTeamPossibilitiesForEvent(String eventName)
	{
		final List<FightEventMap> allMaps = getMapsForEvent(eventName);
		final List<Integer> teams = new ArrayList<>();

		for (final FightEventMap map : allMaps)
		{
			for (final int possibility : map.getTeamCount())
			{
				if (!teams.contains(possibility))
				{
					teams.add(possibility);
				}
			}
		}
		Collections.sort(teams);

		return teams;
	}

	public int getMinPlayersForEvent(String eventName)
	{
		final List<FightEventMap> allMaps = getMapsForEvent(eventName);
		int minPlayers = Integer.MAX_VALUE;
		for (final FightEventMap map : allMaps)
		{
			final int newMin = map.getMinAllPlayers();
			if (newMin < minPlayers)
			{
				minPlayers = newMin;
			}
		}
		return minPlayers;
	}

	public int getMaxPlayersForEvent(String eventName)
	{
		final List<FightEventMap> allMaps = getMapsForEvent(eventName);
		int maxPlayers = 0;

		for (final FightEventMap map : allMaps)
		{
			final int newMax = map.getMaxAllPlayers();
			if (newMax > maxPlayers)
			{
				maxPlayers = newMax;
			}
		}
		return maxPlayers;
	}
	
	public static FightEventMapParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FightEventMapParser _instance = new FightEventMapParser();
	}
}