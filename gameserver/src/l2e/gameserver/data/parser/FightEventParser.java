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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.TreeIntObjectMap;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventItem;

public final class FightEventParser extends DocumentParser
{
	private final IntObjectMap<AbstractFightEvent> _events = new TreeIntObjectMap<>();
	private final List<Integer> _activeEvents = new CopyOnWriteArrayList<>();
	private final List<Integer> _disabledEvents = new CopyOnWriteArrayList<>();
	private final Map<Integer, Map<Integer, List<FightEventItem>>> _eventItems = new HashMap<>();

	protected FightEventParser()
	{
		_events.clear();
		_activeEvents.clear();
		_disabledEvents.clear();
		_eventItems.clear();
		load();
	}
	
	public void reload()
	{
		_events.clear();
		_activeEvents.clear();
		_disabledEvents.clear();
		load();
	}

	@Override
	public final void load()
	{
		parseDirectory("data/stats/events", false);
		info("Loaded " + _events.size() + " events.");
		info("Loaded " + _activeEvents.size() + " active and " + _disabledEvents.size() + " disable events.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@SuppressWarnings("unchecked")
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
						NamedNodeMap attrs = events.getAttributes();

						final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						final String impl = attrs.getNamedItem("impl").getNodeValue();

						Class<AbstractFightEvent> eventClass = null;
						try
						{
							eventClass = (Class<AbstractFightEvent>) Class.forName("l2e.gameserver.model.entity.events.model.impl." + impl + "Event");
						}
						catch (final ClassNotFoundException e)
						{
							warn("Not found impl class: " + impl + "; File: " + getCurrentFile().getName(), e);
							continue;
						}
						
						Constructor<AbstractFightEvent> constructor = null;
						try
						{
							constructor = eventClass.getConstructor(MultiValueSet.class);
						}
						catch (
						    IllegalArgumentException | NoSuchMethodException | SecurityException e)
						{
							warn("Unable to create eventClass!");
							e.printStackTrace();
						}
						
						final MultiValueSet<String> set = new MultiValueSet<>();
						final Map<Integer, List<FightEventItem>> eventItems = new HashMap<>();
						set.set("id", id);
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								set.set(name, attrs.getNamedItem(name) != null ? attrs.getNamedItem(name).getNodeValue() : attrs.getNamedItem("nameEn") != null ? attrs.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						set.set("eventClass", "l2e.gameserver.model.entity.events.model.impl." + impl + "Event");
						
						for (Node par = events.getFirstChild(); par != null; par = par.getNextSibling())
						{
							if ("parameter".equalsIgnoreCase(par.getNodeName()))
							{
								attrs = par.getAttributes();
								set.set(attrs.getNamedItem("name").getNodeValue(), attrs.getNamedItem("value").getNodeValue());
							}
							else if ("eventItems".equalsIgnoreCase(par.getNodeName()))
							{
								for (Node d1 = par.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
								{
									if ("class".equalsIgnoreCase(d1.getNodeName()))
									{
										final int classId = Integer.parseInt(d1.getAttributes().getNamedItem("id").getNodeValue());
										final List<FightEventItem> items = new ArrayList<>();
										for (Node s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
										{
											if ("itemList".equalsIgnoreCase(s1.getNodeName()))
											{
												for (Node it = s1.getFirstChild(); it != null; it = it.getNextSibling())
												{
													if ("item".equalsIgnoreCase(it.getNodeName()))
													{
														final int itemId = Integer.parseInt(it.getAttributes().getNamedItem("id").getNodeValue());
														final long itemAmount = Long.parseLong(it.getAttributes().getNamedItem("amount").getNodeValue());
														final int enchant = it.getAttributes().getNamedItem("enchant") != null ? Integer.parseInt(it.getAttributes().getNamedItem("enchant").getNodeValue()) : 0;
														final String augmentation = it.getAttributes().getNamedItem("augmentation") != null ? it.getAttributes().getNamedItem("augmentation").getNodeValue() : null;
														final String elementals = it.getAttributes().getNamedItem("elementals") != null ? it.getAttributes().getNamedItem("elementals").getNodeValue() : null;
														items.add(new FightEventItem(itemId, itemAmount, enchant, augmentation, elementals));
													}
												}
											}
										}
										eventItems.put(classId, items);
									}
								}
							}
						}

						AbstractFightEvent event = null;
						try
						{
							event = constructor.newInstance(set);
						}
						catch (
						    IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException e)
						{
							warn("Unable to create event!");
							e.printStackTrace();
						}
						
						if (Arrays.binarySearch(Config.DISALLOW_FIGHT_EVENTS, id) >= 0)
						{
							_disabledEvents.add(event.getId());
						}
						else
						{
							_activeEvents.add(event.getId());
						}
						_events.put(event.getId(), event);
						_eventItems.put(event.getId(), eventItems);
					}
				}
			}
		}
	}
	
	public Map<Integer, List<FightEventItem>> getEventItems(final int eventId)
	{
		return _eventItems.get(eventId);
	}
	
	public AbstractFightEvent getEvent(int id)
	{
		return _events.get(id);
	}
	
	public IntObjectMap<AbstractFightEvent> getEvents()
	{
		return _events;
	}
	
	public List<Integer> getAcviteEvents()
	{
		return _activeEvents;
	}

	public List<Integer> getDisabledEvents()
	{
		return _disabledEvents;
	}
	
	public static FightEventParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FightEventParser _instance = new FightEventParser();
	}
}
