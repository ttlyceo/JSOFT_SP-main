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
import l2e.gameserver.model.actor.templates.DressHatTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public final class DressHatParser extends DocumentParser
{
	private final List<DressHatTemplate> _hat = new ArrayList<>();
	private final List<DressHatTemplate> _activeList = new ArrayList<>();

	private DressHatParser()
	{
		_hat.clear();
		_activeList.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/dress/dressHat.xml");
		info("Loaded " + _hat.size() + " dress hat templates.");
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
					if ("hat".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap hat = d.getAttributes();
						
						int itemId = 0;
						long itemCount = 0;
						double removeModifier = 1;
						
						final StatsSet set = new StatsSet();
						
						set.set("id", Integer.parseInt(hat.getNamedItem("number").getNodeValue()));
						set.set("hat", Integer.parseInt(hat.getNamedItem("id").getNodeValue()));
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								set.set(name, hat.getNamedItem(name) != null ? hat.getNamedItem(name).getNodeValue() : hat.getNamedItem("nameEn") != null ? hat.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						set.set("slot", Integer.parseInt(hat.getNamedItem("slot").getNodeValue()));
						final var isForSell = hat.getNamedItem("isForSell") != null ? Boolean.parseBoolean(hat.getNamedItem("isForSell").getNodeValue()) : true;
						set.set("isForSell", isForSell);
						
						final List<Skill> skills = new ArrayList<>();
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							hat = cd.getAttributes();
							if ("price".equalsIgnoreCase(cd.getNodeName()))
							{
								itemId = Integer.parseInt(hat.getNamedItem("id").getNodeValue());
								itemCount = Long.parseLong(hat.getNamedItem("count").getNodeValue());
							}
							else if ("removed".equalsIgnoreCase(cd.getNodeName()))
							{
								removeModifier = Double.parseDouble(hat.getNamedItem("modifier").getNodeValue());
							}
							else if ("skill".equalsIgnoreCase(cd.getNodeName()))
							{
								final int skillId = Integer.parseInt(hat.getNamedItem("id").getNodeValue());
								final int level = Integer.parseInt(hat.getNamedItem("level").getNodeValue());
								final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
								if (data != null)
								{
									skills.add(data);
								}
							}
						}
						final var tpl = new DressHatTemplate(set, itemId, itemCount, skills, removeModifier);
						addHat(tpl);
						if (isForSell)
						{
							_activeList.add(tpl);
						}
					}
				}
			}
		}
	}

	public void addHat(DressHatTemplate shield)
	{
		_hat.add(shield);
	}
	
	public List<DressHatTemplate> getAllHats()
	{
		return _hat;
	}
	
	public List<DressHatTemplate> getAllActiveHats()
	{
		return _activeList;
	}
	
	public DressHatTemplate getHat(int id)
	{
		for (final DressHatTemplate hat : _hat)
		{
			if (hat.getId() == id)
			{
				return hat;
			}
		}
		return null;
	}
	
	public DressHatTemplate getActiveHat(int id)
	{
		for (final DressHatTemplate hat : _activeList)
		{
			if (hat.getId() == id)
			{
				return hat;
			}
		}
		return null;
	}
	
	public int getHatId(int id)
	{
		for (final DressHatTemplate hat : _hat)
		{
			if (hat.getHatId() == id)
			{
				return hat.getId();
			}
		}
		return -1;
	}
	
	public int size()
	{
		return _hat.size();
	}
	
	public static DressHatParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DressHatParser _instance = new DressHatParser();
	}
}