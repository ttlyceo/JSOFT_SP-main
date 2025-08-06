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
import java.util.List;

import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.Location;

public class HerbsDropParser extends DocumentParser
{
	private final List<Location> _herbs = new ArrayList<>();
	private int _chance;
	private int _rndAmount;
	private int _respawnDelay;
	
	protected HerbsDropParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_herbs.clear();
		parseDatapackFile("data/stats/npcs/spawnZones/herbSpawnZones.xml");
		info("Loaded " + _herbs.size() + " heine herb spawn zones.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node list = getCurrentDocument().getFirstChild(); list != null; list = list.getNextSibling())
		{
			if (list.getNodeName().equalsIgnoreCase("list"))
			{
				for (Node n = list.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("param".equalsIgnoreCase(n.getNodeName()))
					{
						_chance = Integer.parseInt(n.getAttributes().getNamedItem("chance").getNodeValue());
						_rndAmount = Integer.parseInt(n.getAttributes().getNamedItem("rndAmount").getNodeValue());
						_respawnDelay = Integer.parseInt(n.getAttributes().getNamedItem("respawnDelay").getNodeValue());
					}
					else if ("location".equalsIgnoreCase(n.getNodeName()))
					{
						_herbs.add(Location.parseLoc(n.getAttributes().getNamedItem("val").getNodeValue()));
					}
				}
			}
		}
	}
	
	public List<Location> getHerbSpawns()
	{
		return _herbs;
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public int getRndAmount()
	{
		return _rndAmount;
	}
	
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
	
	public static HerbsDropParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final HerbsDropParser _instance = new HerbsDropParser();
	}
}