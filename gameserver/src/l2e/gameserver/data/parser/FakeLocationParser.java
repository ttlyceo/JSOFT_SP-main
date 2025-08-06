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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.templates.player.FakeLocTemplate;

public class FakeLocationParser extends DocumentParser
{
	private final List<FakeLocTemplate> _locations = new ArrayList<>();
	private int _totalAmount = 0;
	
	protected FakeLocationParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_locations.clear();
		parseDatapackFile("config/mods/fakes/locations.xml");
		info("Loaded " + _locations.size() + " fake players locations.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node list = getCurrentDocument().getFirstChild().getFirstChild(); list != null; list = list.getNextSibling())
		{
			if (list.getNodeName().equalsIgnoreCase("location"))
			{
				final NamedNodeMap node = list.getAttributes();
				
				final int id = Integer.valueOf(node.getNamedItem("id").getNodeValue());
				final int amount = Integer.valueOf(node.getNamedItem("amount").getNodeValue());
				final int distance = Integer.valueOf(node.getNamedItem("distance").getNodeValue());
				Location loc = null;
				int minLvl = 1, maxLvl = 1;
				List<Integer> classes = null;
				for (Node d = list.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("coords".equalsIgnoreCase(d.getNodeName()))
					{
						final int x = Integer.valueOf(d.getAttributes().getNamedItem("x").getNodeValue());
						final int y = Integer.valueOf(d.getAttributes().getNamedItem("y").getNodeValue());
						final int z = Integer.valueOf(d.getAttributes().getNamedItem("z").getNodeValue());
						loc = new Location(x, y, z);
					}
					else if ("class".equalsIgnoreCase(d.getNodeName()))
					{
						classes = parseExcludedClasses(d.getAttributes().getNamedItem("id").getNodeValue());
					}
					else if ("level".equalsIgnoreCase(d.getNodeName()))
					{
						minLvl = Integer.valueOf(d.getAttributes().getNamedItem("min").getNodeValue());
						maxLvl = Integer.valueOf(d.getAttributes().getNamedItem("max").getNodeValue());
					}
				}
				_totalAmount += amount;
				_locations.add(new FakeLocTemplate(id, amount, loc, classes, minLvl, maxLvl, distance));
			}
		}
	}
	
	public FakeLocTemplate createRndLoc(Location location)
	{
		final int id = _locations.size() + 1;
		final int minLvl = Rnd.get(10, 85);
		final int distance = Rnd.get(2000, 5000);
		final FakeLocTemplate loc = new FakeLocTemplate(id, 1, location, null, minLvl, (minLvl + 5), distance);
		_locations.add(loc);
		return loc;
	}
	
	private List<Integer> parseExcludedClasses(String classes)
	{
		if (classes.equals(""))
		{
			return null;
		}
		
		final String[] classType = classes.split(";");
		
		final List<Integer> selected = new ArrayList<>(classType.length);
		for (final String classId : classType)
		{
			selected.add(Integer.parseInt(classId.trim()));
		}
		return selected;
	}
	
	public FakeLocTemplate getRandomSpawnLoc()
	{
		final List<FakeLocTemplate> locations = new ArrayList<>();
		for (final FakeLocTemplate template : _locations)
		{
			if (template != null)
			{
				if (template.getCurrentAmount() < template.getAmount())
				{
					locations.add(template);
				}
			}
		}
		return locations.get(Rnd.get(locations.size()));
	}
	
	public List<FakeLocTemplate> getSpawnLocations()
	{
		return _locations;
	}
	
	public int getTotalAmount()
	{
		return _totalAmount;
	}
	
	public static FakeLocationParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FakeLocationParser _instance = new FakeLocationParser();
	}
}