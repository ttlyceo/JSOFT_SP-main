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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.geometry.Polygon;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.entity.events.model.template.WorldEventDrop;
import l2e.gameserver.model.entity.events.model.template.WorldEventLocation;
import l2e.gameserver.model.entity.events.model.template.WorldEventReward;
import l2e.gameserver.model.entity.events.model.template.WorldEventSpawn;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.entity.events.model.template.WorldEventTerritory;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.model.stats.StatsSet;

/**
 * Created by LordWinter 14.12.2021
 */
public final class WorldEventParser extends DocumentParser
{
	private final Map<Integer, WorldEventTemplate> _events = new HashMap<>();

	protected WorldEventParser()
	{
		_events.clear();
		load();
	}
	
	public void reload()
	{
		parseDirectory("data/stats/events/worldEvents", true);
		info("Reloaded " + _events.size() + " world event templates.");
	}

	@Override
	public final void load()
	{
		parseDirectory("data/stats/events/worldEvents", false);
		info("Loaded " + _events.size() + " world event templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
		for (Node c = getCurrentDocument().getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("list".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node events = c.getFirstChild(); events != null; events = events.getNextSibling())
				{
					if ("event".equalsIgnoreCase(events.getNodeName()))
					{
						final int id = Integer.parseInt(events.getAttributes().getNamedItem("id").getNodeValue());
						
						String startTimePattern = null;
						String stopTimePattern = null;
						boolean isNonStop = false;
						final Map<Integer, List<WorldEventReward>> variantRequests = new HashMap<>();
						final Map<Integer, List<WorldEventReward>> variantRewards = new HashMap<>();
						final Map<Integer, List<WorldEventReward>> variantRandomRewards = new HashMap<>();
						final List<WorldEventDrop> dropList = new ArrayList<>();
						final List<WorldEventSpawn> spawnList = new ArrayList<>();
						final List<WorldEventLocation> locations = new ArrayList<>();
						final List<WorldEventTerritory> territories = new ArrayList<>();
						final StatsSet params = new StatsSet();
						
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								params.set(name, events.getAttributes().getNamedItem(name) != null ? events.getAttributes().getNamedItem(name).getNodeValue() : events.getAttributes().getNamedItem("nameEn") != null ? events.getAttributes().getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						final boolean activate = Boolean.parseBoolean(events.getAttributes().getNamedItem("activate").getNodeValue());
						
						for (Node n = events.getFirstChild(); n != null; n = n.getNextSibling())
						{
							if (n.getNodeName().equalsIgnoreCase("time"))
							{
								startTimePattern = n.getAttributes().getNamedItem("startPattern") == null ? null : n.getAttributes().getNamedItem("startPattern").getNodeValue();
								stopTimePattern = n.getAttributes().getNamedItem("stopPattern") == null ? null : n.getAttributes().getNamedItem("stopPattern").getNodeValue();
								isNonStop = n.getAttributes().getNamedItem("isNonStop") == null ? false : Boolean.parseBoolean(n.getAttributes().getNamedItem("isNonStop").getNodeValue());
							}
							else if (n.getNodeName().equalsIgnoreCase("spawnlist"))
							{
								for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
								{
									if (d.getNodeName().equalsIgnoreCase("npc"))
									{
										try
										{
											final int npcId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
											final int x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
											final int y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
											final int z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
											final int h = d.getAttributes().getNamedItem("heading").getNodeValue() != null ? Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue()) : 0;
											final int triggerId = d.getAttributes().getNamedItem("triggerId") != null ? Integer.parseInt(d.getAttributes().getNamedItem("triggerId").getNodeValue()) : 0;
											
											if (NpcsParser.getInstance().getTemplate(npcId) == null)
											{
												warn("NpcId " + npcId + " is wrong NPC id, NPC was not added in spawnlist");
												continue;
											}
											spawnList.add(new WorldEventSpawn(npcId, new Location(x, y, z, h), triggerId));
										}
										catch (final NumberFormatException nfe)
										{
											warn("Wrong number format in xml settings!", nfe);
										}
									}
									else if (d.getNodeName().equalsIgnoreCase("location"))
									{
										final String name = d.getAttributes().getNamedItem("name").getNodeValue();
										final int x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
										final int y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
										final int z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
										final int h = d.getAttributes().getNamedItem("h").getNodeValue() != null ? Integer.parseInt(d.getAttributes().getNamedItem("h").getNodeValue()) : 0;
										locations.add(new WorldEventLocation(name, new Location(x, y, z, h)));
									}
									else if (d.getNodeName().equalsIgnoreCase("territory"))
									{
										final String name = d.getAttributes().getNamedItem("name").getNodeValue();
										final SpawnTerritory t = new SpawnTerritory();
										t.add(parsePolygon0(name, d, d.getAttributes()));
										territories.add(new WorldEventTerritory(name, t));
									}
								}
							}
							else if (n.getNodeName().equalsIgnoreCase("droplist"))
							{
								for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
								{
									if (d.getNodeName().equalsIgnoreCase("item"))
									{
										final int itemId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
										final long min = Long.parseLong(d.getAttributes().getNamedItem("min").getNodeValue());
										final long max = d.getAttributes().getNamedItem("max") != null ? Long.parseLong(d.getAttributes().getNamedItem("max").getNodeValue()) : 0;
										final double chance = Double.parseDouble(d.getAttributes().getNamedItem("chance").getNodeValue());
										final int minLvl = d.getAttributes().getNamedItem("minLvl") != null ? Integer.parseInt(d.getAttributes().getNamedItem("minLvl").getNodeValue()) : 1;
										final int maxLvl = d.getAttributes().getNamedItem("maxLvl") != null ? Integer.parseInt(d.getAttributes().getNamedItem("maxLvl").getNodeValue()) : 85;
										dropList.add(new WorldEventDrop(itemId, min, max, chance, minLvl, maxLvl));
									}
								}
							}
							else if ("add_parameters".equalsIgnoreCase(n.getNodeName()))
							{
								for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
								{
									if ("set".equalsIgnoreCase(d.getNodeName()))
									{
										params.set(d.getAttributes().getNamedItem("name").getNodeValue(), d.getAttributes().getNamedItem("value").getNodeValue());
									}
								}
							}
							else if (n.getNodeName().equalsIgnoreCase("rewardlist"))
							{
								for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
								{
									if (d.getNodeName().equalsIgnoreCase("variant"))
									{
										final List<WorldEventReward> requestList = new ArrayList<>();
										final List<WorldEventReward> rewardList = new ArrayList<>();
										final List<WorldEventReward> randomRewardList = new ArrayList<>();
										final int variantId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
										for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
										{
											if (e.getNodeName().equalsIgnoreCase("reward"))
											{
												final int itemId = Integer.parseInt(e.getAttributes().getNamedItem("id").getNodeValue());
												final long min = Integer.parseInt(e.getAttributes().getNamedItem("min").getNodeValue());
												final long max = e.getAttributes().getNamedItem("max") != null ? Integer.parseInt(e.getAttributes().getNamedItem("max").getNodeValue()) : 0;
												final double chance = e.getAttributes().getNamedItem("chance") != null ? Double.parseDouble(e.getAttributes().getNamedItem("chance").getNodeValue()) : 0;
												rewardList.add(new WorldEventReward(itemId, min, max, chance));
											}
											else if (e.getNodeName().equalsIgnoreCase("request"))
											{
												final int itemId = Integer.parseInt(e.getAttributes().getNamedItem("id").getNodeValue());
												final long min = Integer.parseInt(e.getAttributes().getNamedItem("min").getNodeValue());
												final long max = e.getAttributes().getNamedItem("max") != null ? Integer.parseInt(e.getAttributes().getNamedItem("max").getNodeValue()) : 0;
												requestList.add(new WorldEventReward(itemId, min, max, 0));
											}
											else if (e.getNodeName().equalsIgnoreCase("random"))
											{
												for (Node g = e.getFirstChild(); g != null; g = g.getNextSibling())
												{
													if (g.getNodeName().equalsIgnoreCase("reward"))
													{
														final int itemId = Integer.parseInt(g.getAttributes().getNamedItem("id").getNodeValue());
														final long min = Integer.parseInt(g.getAttributes().getNamedItem("min").getNodeValue());
														final long max = g.getAttributes().getNamedItem("max") != null ? Integer.parseInt(g.getAttributes().getNamedItem("max").getNodeValue()) : 0;
														final double chance = g.getAttributes().getNamedItem("chance") != null ? Double.parseDouble(g.getAttributes().getNamedItem("chance").getNodeValue()) : 0;
														randomRewardList.add(new WorldEventReward(itemId, min, max, chance));
													}
												}
											}
										}
										variantRequests.put(variantId, requestList);
										variantRewards.put(variantId, rewardList);
										variantRandomRewards.put(variantId, randomRewardList);
									}
								}
							}
						}
						addWorldEvent(id, new WorldEventTemplate(id, activate, new SchedulingPattern(startTimePattern), new SchedulingPattern(stopTimePattern), isNonStop, dropList, variantRequests, variantRewards, variantRandomRewards, spawnList, locations, territories, params));
					}
				}
			}
		}
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node c = getCurrentDocument().getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("list".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node events = c.getFirstChild(); events != null; events = events.getNextSibling())
				{
					if ("event".equalsIgnoreCase(events.getNodeName()))
					{
						String startTimePattern = null;
						String stopTimePattern = null;
						boolean isNonStop = false;
						final Map<Integer, List<WorldEventReward>> variantRequests = new HashMap<>();
						final Map<Integer, List<WorldEventReward>> variantRewards = new HashMap<>();
						final Map<Integer, List<WorldEventReward>> variantRandomRewards = new HashMap<>();
						final List<WorldEventDrop> dropList = new ArrayList<>();
						final List<WorldEventSpawn> spawnList = new ArrayList<>();
						final List<WorldEventLocation> locations = new ArrayList<>();
						final List<WorldEventTerritory> territories = new ArrayList<>();
						final StatsSet params = new StatsSet();

						final int id = Integer.parseInt(events.getAttributes().getNamedItem("id").getNodeValue());
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								params.set(name, events.getAttributes().getNamedItem(name) != null ? events.getAttributes().getNamedItem(name).getNodeValue() : events.getAttributes().getNamedItem("nameEn") != null ? events.getAttributes().getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						final boolean activate = Boolean.parseBoolean(events.getAttributes().getNamedItem("activate").getNodeValue());
						
						for (Node n = events.getFirstChild(); n != null; n = n.getNextSibling())
						{
							if (n.getNodeName().equalsIgnoreCase("time"))
							{
								startTimePattern = n.getAttributes().getNamedItem("startPattern") == null ? null : n.getAttributes().getNamedItem("startPattern").getNodeValue();
								stopTimePattern = n.getAttributes().getNamedItem("stopPattern") == null ? null : n.getAttributes().getNamedItem("stopPattern").getNodeValue();
								isNonStop = n.getAttributes().getNamedItem("isNonStop") == null ? false : Boolean.parseBoolean(n.getAttributes().getNamedItem("isNonStop").getNodeValue());
							}
							else if (n.getNodeName().equalsIgnoreCase("spawnlist"))
							{
								for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
								{
									if (d.getNodeName().equalsIgnoreCase("npc"))
									{
										try
										{
											final int npcId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
											final int x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
											final int y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
											final int z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
											final int h = d.getAttributes().getNamedItem("heading").getNodeValue() != null ? Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue()) : 0;
											final int triggerId = d.getAttributes().getNamedItem("triggerId") != null ? Integer.parseInt(d.getAttributes().getNamedItem("triggerId").getNodeValue()) : 0;
											
											if (NpcsParser.getInstance().getTemplate(npcId) == null)
											{
												warn("NpcId " + npcId + " is wrong NPC id, NPC was not added in spawnlist");
												continue;
											}
											spawnList.add(new WorldEventSpawn(npcId, new Location(x, y, z, h), triggerId));
										}
										catch (final NumberFormatException nfe)
										{
											warn("Wrong number format in xml settings!", nfe);
										}
									}
									else if (d.getNodeName().equalsIgnoreCase("location"))
									{
										final String name = d.getAttributes().getNamedItem("name").getNodeValue();
										final int x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
										final int y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
										final int z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
										final int h = d.getAttributes().getNamedItem("h").getNodeValue() != null ? Integer.parseInt(d.getAttributes().getNamedItem("h").getNodeValue()) : 0;
										locations.add(new WorldEventLocation(name, new Location(x, y, z, h)));
									}
									else if (d.getNodeName().equalsIgnoreCase("territory"))
									{
										final String name = d.getAttributes().getNamedItem("name").getNodeValue();
										final SpawnTerritory t = new SpawnTerritory();
										t.add(parsePolygon0(name, d, d.getAttributes()));
										territories.add(new WorldEventTerritory(name, t));
									}
								}
							}
							else if (n.getNodeName().equalsIgnoreCase("droplist"))
							{
								for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
								{
									if (d.getNodeName().equalsIgnoreCase("item"))
									{
										final int itemId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
										final long min = Long.parseLong(d.getAttributes().getNamedItem("min").getNodeValue());
										final long max = d.getAttributes().getNamedItem("max") != null ? Long.parseLong(d.getAttributes().getNamedItem("max").getNodeValue()) : 0;
										final double chance = Double.parseDouble(d.getAttributes().getNamedItem("chance").getNodeValue());
										final int minLvl = d.getAttributes().getNamedItem("minLvl") != null ? Integer.parseInt(d.getAttributes().getNamedItem("minLvl").getNodeValue()) : 1;
										final int maxLvl = d.getAttributes().getNamedItem("maxLvl") != null ? Integer.parseInt(d.getAttributes().getNamedItem("maxLvl").getNodeValue()) : 85;
										dropList.add(new WorldEventDrop(itemId, min, max, chance, minLvl, maxLvl));
									}
								}
							}
							else if ("add_parameters".equalsIgnoreCase(n.getNodeName()))
							{
								for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
								{
									if ("set".equalsIgnoreCase(d.getNodeName()))
									{
										params.set(d.getAttributes().getNamedItem("name").getNodeValue(), d.getAttributes().getNamedItem("value").getNodeValue());
									}
								}
							}
							else if (n.getNodeName().equalsIgnoreCase("rewardlist"))
							{
								for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
								{
									if (d.getNodeName().equalsIgnoreCase("variant"))
									{
										final List<WorldEventReward> requestList = new ArrayList<>();
										final List<WorldEventReward> rewardList = new ArrayList<>();
										final List<WorldEventReward> randomRewardList = new ArrayList<>();
										final int variantId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
										for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
										{
											if (e.getNodeName().equalsIgnoreCase("reward"))
											{
												final int itemId = Integer.parseInt(e.getAttributes().getNamedItem("id").getNodeValue());
												final long min = Integer.parseInt(e.getAttributes().getNamedItem("min").getNodeValue());
												final long max = e.getAttributes().getNamedItem("max") != null ? Integer.parseInt(e.getAttributes().getNamedItem("max").getNodeValue()) : 0;
												final double chance = e.getAttributes().getNamedItem("chance") != null ? Double.parseDouble(e.getAttributes().getNamedItem("chance").getNodeValue()) : 0;
												rewardList.add(new WorldEventReward(itemId, min, max, chance));
											}
											else if (e.getNodeName().equalsIgnoreCase("request"))
											{
												final int itemId = Integer.parseInt(e.getAttributes().getNamedItem("id").getNodeValue());
												final long min = Integer.parseInt(e.getAttributes().getNamedItem("min").getNodeValue());
												final long max = e.getAttributes().getNamedItem("max") != null ? Integer.parseInt(e.getAttributes().getNamedItem("max").getNodeValue()) : 0;
												requestList.add(new WorldEventReward(itemId, min, max, 0));
											}
											else if (e.getNodeName().equalsIgnoreCase("random"))
											{
												for (Node g = e.getFirstChild(); g != null; g = g.getNextSibling())
												{
													if (g.getNodeName().equalsIgnoreCase("reward"))
													{
														final int itemId = Integer.parseInt(g.getAttributes().getNamedItem("id").getNodeValue());
														final long min = Integer.parseInt(g.getAttributes().getNamedItem("min").getNodeValue());
														final long max = g.getAttributes().getNamedItem("max") != null ? Integer.parseInt(g.getAttributes().getNamedItem("max").getNodeValue()) : 0;
														final double chance = g.getAttributes().getNamedItem("chance") != null ? Double.parseDouble(g.getAttributes().getNamedItem("chance").getNodeValue()) : 0;
														randomRewardList.add(new WorldEventReward(itemId, min, max, chance));
													}
												}
											}
										}
										variantRequests.put(variantId, requestList);
										variantRewards.put(variantId, rewardList);
										variantRandomRewards.put(variantId, randomRewardList);
									}
								}
							}
						}
						addWorldEvent(id, new WorldEventTemplate(id, activate, new SchedulingPattern(startTimePattern), new SchedulingPattern(stopTimePattern), isNonStop, dropList, variantRequests, variantRewards, variantRandomRewards, spawnList, locations, territories, params));
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
	
	public WorldEventTemplate getEvent(int id)
	{
		return _events.get(id);
	}
	
	public Map<Integer, WorldEventTemplate> getEvents()
	{
		return _events;
	}
	
	public void addWorldEvent(int id, WorldEventTemplate template)
	{
		_events.put(id, template);
	}
	
	public static WorldEventParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final WorldEventParser _instance = new WorldEventParser();
	}
}
