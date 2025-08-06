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

import l2e.commons.geometry.Polygon;
import l2e.commons.util.Rnd;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.templates.player.FakePassiveLocTemplate;
import l2e.gameserver.model.actor.templates.player.FakeTraderTemplate;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.spawn.SpawnTerritory;

public class FakePassiveLocationParser extends DocumentParser
{
	private final List<FakePassiveLocTemplate> _locations = new ArrayList<>();
	private final List<FakeTraderTemplate> _traders = new ArrayList<>();
	private int _totalAmount = 0;
	
	protected FakePassiveLocationParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_locations.clear();
		_traders.clear();
		parseDatapackFile("config/mods/fakes/passive_locations.xml");
		info("Loaded " + _locations.size() + " fake players passive locations.");
		if (_traders.size() > 0)
		{
			info("Loaded " + _traders.size() + " fake traders.");
		}
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
				SpawnTerritory territory = null;
				int minLvl = 1, maxLvl = 1;
				long minDelay = 1, maxDelay = 1, minRespawn = 1, maxRespawn = 1;
				List<Integer> classes = null;
				for (Node d = list.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if (d.getNodeName().equalsIgnoreCase("territory"))
					{
						final String name = d.getAttributes().getNamedItem("name").getNodeValue();
						territory = new SpawnTerritory();
						territory.add(parsePolygon0(name, d, d.getAttributes()));
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
					else if ("delay".equalsIgnoreCase(d.getNodeName()))
					{
						minDelay = Integer.valueOf(d.getAttributes().getNamedItem("min").getNodeValue());
						maxDelay = Integer.valueOf(d.getAttributes().getNamedItem("max").getNodeValue());
					}
					else if ("respawn".equalsIgnoreCase(d.getNodeName()))
					{
						minRespawn = Integer.valueOf(d.getAttributes().getNamedItem("min").getNodeValue());
						maxRespawn = Integer.valueOf(d.getAttributes().getNamedItem("max").getNodeValue());
					}
				}
				_totalAmount += amount;
				_locations.add(new FakePassiveLocTemplate(id, amount, territory, classes, minLvl, maxLvl, minDelay, maxDelay, minRespawn, maxRespawn));
			}
			else if (list.getNodeName().equalsIgnoreCase("traders"))
			{
				for (Node d = list.getFirstChild(); d != null; d = d.getNextSibling())
				{
					NamedNodeMap node = d.getAttributes();
					if (d.getNodeName().equalsIgnoreCase("trader"))
					{
						final int id = Integer.valueOf(node.getNamedItem("id").getNodeValue());
						final int classId = node.getNamedItem("classId") != null ? Integer.valueOf(node.getNamedItem("classId").getNodeValue()) : -1;
						final String type = node.getNamedItem("type") != null ? node.getNamedItem("type").getNodeValue() : null;
						final int minLvl = d.getAttributes().getNamedItem("minLvl") != null ? Integer.valueOf(d.getAttributes().getNamedItem("minLvl").getNodeValue()) : 1;
						final int maxLvl = d.getAttributes().getNamedItem("maxLvl") != null ? Integer.valueOf(d.getAttributes().getNamedItem("maxLvl").getNodeValue()) : 85;
						SpawnTerritory territory = null;
						Location loc = null;
						final List<ItemHolder> addItems = new ArrayList<>();
						final List<ItemHolder> tradeList = new ArrayList<>();
						String message = "";
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							node = c.getAttributes();
							if (c.getNodeName().equalsIgnoreCase("territory"))
							{
								final String name = node.getNamedItem("name").getNodeValue();
								territory = new SpawnTerritory();
								territory.add(parsePolygon0(name, c, c.getAttributes()));
							}
							else if ("point".equalsIgnoreCase(c.getNodeName()))
							{
								final int x = Integer.parseInt(node.getNamedItem("x").getNodeValue());
								final int y = Integer.parseInt(node.getNamedItem("y").getNodeValue());
								final int z = Integer.parseInt(node.getNamedItem("z").getNodeValue());
								final int h = node.getNamedItem("h") == null ? 0 : Integer.parseInt(node.getNamedItem("h").getNodeValue());
								loc = new Location(x, y, z, h);
							}
							else if ("items".equalsIgnoreCase(c.getNodeName()))
							{
								for (Node e = c.getFirstChild(); e != null; e = e.getNextSibling())
								{
									node = e.getAttributes();
									if (e.getNodeName().equalsIgnoreCase("add"))
									{
										final int itemId = Integer.parseInt(node.getNamedItem("itemId").getNodeValue());
										final long amount = Long.parseLong(node.getNamedItem("amount").getNodeValue());
										final int enchantLevel = node.getNamedItem("enchant") != null ? Integer.parseInt(node.getNamedItem("enchant").getNodeValue()) : 0;
										final var item = ItemsParser.getInstance().getTemplate(itemId);
										if (item != null)
										{
											var canAdd = true;
											if (!item.isStackable() && amount > 100)
											{
												canAdd = false;
											}
											
											if (canAdd)
											{
												addItems.add(new ItemHolder(itemId, amount, 100, enchantLevel));
											}
										}
									}
									else if (e.getNodeName().equalsIgnoreCase("trade"))
									{
										final int itemId = Integer.parseInt(node.getNamedItem("itemId").getNodeValue());
										final long amountMin = Long.parseLong(node.getNamedItem("amountMin").getNodeValue());
										final long amountMax = Long.parseLong(node.getNamedItem("amountMax").getNodeValue());
										final long priceMin = Long.parseLong(node.getNamedItem("priceMin").getNodeValue());
										final long priceMax = Long.parseLong(node.getNamedItem("priceMax").getNodeValue());
										final var item = ItemsParser.getInstance().getTemplate(itemId);
										if (item != null)
										{
											final var amount = Rnd.get(amountMin, amountMax);
											var canAdd = true;
											if (!item.isStackable() && amount > 100)
											{
												canAdd = false;
											}
											if (canAdd)
											{
												tradeList.add(new ItemHolder(itemId, amount, Rnd.get(priceMin, priceMax), 0));
											}
										}
									}
								}
							}
							else if ("message".equalsIgnoreCase(c.getNodeName()))
							{
								message = node.getNamedItem("val").getNodeValue();
							}
						}
						_traders.add(new FakeTraderTemplate(id, classId, type, minLvl, maxLvl, territory, loc, addItems, tradeList, message));
					}
				}
			}
		}
	}
	
	private Polygon parsePolygon0(String name, Node n, NamedNodeMap attrs)
	{
		final Polygon temp = new Polygon();
		for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling())
		{
			if ("add".equalsIgnoreCase(cd.getNodeName()))
			{
				attrs = cd.getAttributes();
				final int x = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
				final int y = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
				final int zmin = Integer.parseInt(attrs.getNamedItem("zmin").getNodeValue());
				final int zmax = Integer.parseInt(attrs.getNamedItem("zmax").getNodeValue());
				temp.add(x, y).setZmin(zmin).setZmax(zmax);
			}
		}
		
		if (!temp.validate())
		{
			warn("Invalid polygon: " + name + "{" + temp + "}. File: " + getClass().getSimpleName());
		}
		return temp;
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
	
	public FakePassiveLocTemplate getRandomSpawnLoc()
	{
		final List<FakePassiveLocTemplate> locations = new ArrayList<>();
		for (final FakePassiveLocTemplate template : _locations)
		{
			if (template != null)
			{
				if (template.getCurrentAmount() < template.getAmount())
				{
					locations.add(template);
				}
			}
		}
		return locations.size() > 0 ? locations.get(Rnd.get(locations.size())) : null;
	}
	
	public List<FakePassiveLocTemplate> getPassiveLocations()
	{
		return _locations;
	}
	
	public List<FakeTraderTemplate> getFakeTraders()
	{
		return _traders;
	}
	
	public int getTotalAmount()
	{
		return _totalAmount;
	}
	
	public static FakePassiveLocationParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePassiveLocationParser _instance = new FakePassiveLocationParser();
	}
}