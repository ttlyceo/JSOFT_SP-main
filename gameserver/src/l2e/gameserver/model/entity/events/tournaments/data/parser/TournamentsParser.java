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
package l2e.gameserver.model.entity.events.tournaments.data.parser;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsEventsTemplate;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsMainTemplate;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.stats.StatsSet;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author psygrammator
 */
public class TournamentsParser extends DocumentParser
{
	public TournamentsMainTemplate tournamentsMainSettings;
	public final Map<Integer, TournamentsEventsTemplate> tournamentsEventsTemplates = new HashMap<>();

	public void reload()
	{
		tournamentsMainSettings = null;
		tournamentsEventsTemplates.clear();
		load();
	}

	protected TournamentsParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		tournamentsEventsTemplates.clear();
		parseDatapackFile("config/jsoft/events/tournaments.xml");
		if(tournamentsMainSettings.isEnable())
		{
			if(tournamentsMainSettings.isShedule())
				info("Shedule Time: " + tournamentsMainSettings.getSchedule());

			info("Loaded " + tournamentsEventsTemplates.size() + " Tournament Templates.");
		}
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (var n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("engine".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final StatsSet set = new StatsSet();
						set.set("isEnable", Boolean.parseBoolean(attrs.getNamedItem("isEnable").getNodeValue()));
						for (var s = d.getFirstChild(); s != null; s = s.getNextSibling())
						{
							if ("set".equalsIgnoreCase(s.getNodeName()))
							{
								set.set(s.getAttributes().getNamedItem("name").getNodeValue(), s.getAttributes().getNamedItem("val").getNodeValue());
							}
						}
						tournamentsMainSettings = new TournamentsMainTemplate(set);
					}
					else if ("tournament".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final StatsSet set = new StatsSet();
						set.set("id", Integer.parseInt(attrs.getNamedItem("id").getNodeValue()));
						final String type = attrs.getNamedItem("type").getNodeValue();
						final int typeById = TournamentUtil.getIntNameType(type);
						set.set("type", type);
						set.set("registerType", attrs.getNamedItem("registerType").getNodeValue());
						set.set("typeById", typeById);
						set.set("isEnable", Boolean.parseBoolean(attrs.getNamedItem("isEnable").getNodeValue()));
						for (var s = d.getFirstChild(); s != null; s = s.getNextSibling())
						{
							if ("set".equalsIgnoreCase(s.getNodeName()))
							{
								set.set(s.getAttributes().getNamedItem("name").getNodeValue(), s.getAttributes().getNamedItem("val").getNodeValue());
							}
							else if ("allows".equalsIgnoreCase(s.getNodeName()))
							{
								for (var s1 = s.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
								{
									if ("set".equalsIgnoreCase(s1.getNodeName()))
									{
										set.set(s1.getAttributes().getNamedItem("name").getNodeValue(), s1.getAttributes().getNamedItem("val").getNodeValue());
									}
								}
							}
							else if ("rewards".equalsIgnoreCase(s.getNodeName()))
							{
								for (var s1 = s.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
								{
									if ("set".equalsIgnoreCase(s1.getNodeName()))
									{
										set.set(s1.getAttributes().getNamedItem("name").getNodeValue(), set.getString(s1.getAttributes().getNamedItem("name").getNodeValue(), null) != null ? (set.getString(s1.getAttributes().getNamedItem("name").getNodeValue())  + ";" + s1.getAttributes().getNamedItem("val").getNodeValue()) : s1.getAttributes().getNamedItem("val").getNodeValue());
									}
								}
							}
							else if ("locations".equalsIgnoreCase(s.getNodeName()))
							{
								for (var s1 = s.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
								{
									if ("set".equalsIgnoreCase(s1.getNodeName()))
									{
										set.set(s1.getAttributes().getNamedItem("name").getNodeValue(), set.getString(s1.getAttributes().getNamedItem("name").getNodeValue(), null) != null ? (set.getString(s1.getAttributes().getNamedItem("name").getNodeValue())  + ";" + s1.getAttributes().getNamedItem("val").getNodeValue()) : s1.getAttributes().getNamedItem("val").getNodeValue());
									}
								}
							}
						}
						tournamentsEventsTemplates.put(typeById, new TournamentsEventsTemplate(set));
					}
				}
			}
		}
		TournamentUtil.TOURNAMENT_MAIN = tournamentsMainSettings;
		TournamentUtil.TOURNAMENT_EVENTS = tournamentsEventsTemplates;
		TournamentUtil.TOURNAMENT_TYPES = tournamentsEventsTemplates.keySet().stream().toList();
	}


	public static TournamentsParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final TournamentsParser _instance = new TournamentsParser();
	}
}