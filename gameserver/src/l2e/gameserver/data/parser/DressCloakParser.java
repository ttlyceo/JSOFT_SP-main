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

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.templates.DressCloakTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public final class DressCloakParser extends DocumentParser
{
	private final List<DressCloakTemplate> _cloak = new ArrayList<>();
	private final List<DressCloakTemplate> _activeList = new ArrayList<>();
	
	private DressCloakParser()
	{
		_cloak.clear();
		_activeList.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/dress/dressCloak.xml");
		info("Loaded " + _cloak.size() + " dress cloak templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("cloak".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap cloak = d.getAttributes();
						
						int itemId = 0;
						long itemCount = 0L;
						double removeModifier = 1;
						
						final StatsSet set = new StatsSet();
						
						set.set("id", Integer.parseInt(cloak.getNamedItem("number").getNodeValue()));
						set.set("cloak", Integer.parseInt(cloak.getNamedItem("id").getNodeValue()));
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								set.set(name, cloak.getNamedItem(name) != null ? cloak.getNamedItem(name).getNodeValue() : cloak.getNamedItem("nameEn") != null ? cloak.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						final List<Skill> skills = new ArrayList<>();
						final var isForSell = cloak.getNamedItem("isForSell") != null ? Boolean.parseBoolean(cloak.getNamedItem("isForSell").getNodeValue()) : true;
						set.set("isForSell", isForSell);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							cloak = cd.getAttributes();
							if ("price".equalsIgnoreCase(cd.getNodeName()))
							{
								itemId = Integer.parseInt(cloak.getNamedItem("id").getNodeValue());
								itemCount = Long.parseLong(cloak.getNamedItem("count").getNodeValue());
							}
							else if ("removed".equalsIgnoreCase(cd.getNodeName()))
							{
								removeModifier = Double.parseDouble(cloak.getNamedItem("modifier").getNodeValue());
							}
							else if ("skill".equalsIgnoreCase(cd.getNodeName()))
							{
								final int skillId = Integer.parseInt(cloak.getNamedItem("id").getNodeValue());
								final int level = Integer.parseInt(cloak.getNamedItem("level").getNodeValue());
								final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
								if (data != null)
								{
									skills.add(data);
								}
							}
						}
						final var tpl = new DressCloakTemplate(set, itemId, itemCount, skills, removeModifier);
						addCloak(tpl);
						if (isForSell)
						{
							_activeList.add(tpl);
						}
					}
				}
			}
		}
	}
	
	public void addCloak(DressCloakTemplate cloak)
	{
		_cloak.add(cloak);
	}

	public List<DressCloakTemplate> getAllCloaks()
	{
		return _cloak;
	}
	
	public List<DressCloakTemplate> getAllActiveCloaks()
	{
		return _activeList;
	}

	public DressCloakTemplate getCloak(int id)
	{
		for (final DressCloakTemplate cloak : _cloak)
		{
			if (cloak.getId() == id)
			{
				return cloak;
			}
		}
		return null;
	}
	
	public DressCloakTemplate getActiveCloak(int id)
	{
		for (final DressCloakTemplate cloak : _activeList)
		{
			if (cloak.getId() == id)
			{
				return cloak;
			}
		}
		return null;
	}
	
	public int getCloakId(int id)
	{
		for (final DressCloakTemplate cloak : _cloak)
		{
			if (cloak.getCloakId() == id)
			{
				return cloak.getId();
			}
		}
		return -1;
	}

	public int size()
	{
		return _cloak.size();
	}

	public static DressCloakParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DressCloakParser _instance = new DressCloakParser();
	}
}