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
import l2e.gameserver.model.actor.templates.DressShieldTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public final class DressShieldParser extends DocumentParser
{
	private final List<DressShieldTemplate> _shield = new ArrayList<>();
	private final List<DressShieldTemplate> _activeList = new ArrayList<>();

	private DressShieldParser()
	{
		_shield.clear();
		_activeList.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/dress/dressShield.xml");
		info("Loaded " + _shield.size() + " dress shield templates.");
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
					if ("shield".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap shield = d.getAttributes();
						
						int itemId = 0;
						long itemCount = 0;
						double removeModifier = 1;
						
						final StatsSet set = new StatsSet();
						
						set.set("id", Integer.parseInt(shield.getNamedItem("number").getNodeValue()));
						set.set("shield", Integer.parseInt(shield.getNamedItem("id").getNodeValue()));
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								set.set(name, shield.getNamedItem(name) != null ? shield.getNamedItem(name).getNodeValue() : shield.getNamedItem("nameEn") != null ? shield.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						final var isForSell = shield.getNamedItem("isForSell") != null ? Boolean.parseBoolean(shield.getNamedItem("isForSell").getNodeValue()) : true;
						set.set("isForSell", isForSell);
						
						final List<Skill> skills = new ArrayList<>();
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							shield = cd.getAttributes();

							if ("price".equalsIgnoreCase(cd.getNodeName()))
							{
								itemId = Integer.parseInt(shield.getNamedItem("id").getNodeValue());
								itemCount = Long.parseLong(shield.getNamedItem("count").getNodeValue());
							}
							else if ("removed".equalsIgnoreCase(cd.getNodeName()))
							{
								removeModifier = Double.parseDouble(shield.getNamedItem("modifier").getNodeValue());
							}
							else if ("skill".equalsIgnoreCase(cd.getNodeName()))
							{
								final int skillId = Integer.parseInt(shield.getNamedItem("id").getNodeValue());
								final int level = Integer.parseInt(shield.getNamedItem("level").getNodeValue());
								final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
								if (data != null)
								{
									skills.add(data);
								}
							}
						}
						final var tpl = new DressShieldTemplate(set, itemId, itemCount, skills, removeModifier);
						addShield(tpl);
						if (isForSell)
						{
							_activeList.add(tpl);
						}
					}
				}
			}
		}
	}

	public void addShield(DressShieldTemplate shield)
	{
		_shield.add(shield);
	}
	
	public List<DressShieldTemplate> getAllShields()
	{
		return _shield;
	}
	
	public List<DressShieldTemplate> getAllActiveShields()
	{
		return _activeList;
	}
	
	public DressShieldTemplate getShield(int id)
	{
		for (final DressShieldTemplate shield : _shield)
		{
			if (shield.getId() == id)
			{
				return shield;
			}
		}
		return null;
	}
	
	public DressShieldTemplate getActiveShield(int id)
	{
		for (final DressShieldTemplate shield : _activeList)
		{
			if (shield.getId() == id)
			{
				return shield;
			}
		}
		return null;
	}
	
	public int getShieldId(int id)
	{
		for (final DressShieldTemplate shield : _shield)
		{
			if (shield.getShieldId() == id)
			{
				return shield.getId();
			}
		}
		return -1;
	}
	
	public int size()
	{
		return _shield.size();
	}

	public static DressShieldParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DressShieldParser _instance = new DressShieldParser();
	}
}