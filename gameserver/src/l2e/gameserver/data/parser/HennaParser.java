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

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.templates.items.Henna;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public final class HennaParser extends DocumentParser
{
	private final Map<Integer, Henna> _hennaList = new HashMap<>();
	
	protected HennaParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_hennaList.clear();
		parseDatapackFile("data/stats/chars/hennaList.xml");
		info("Loaded " + _hennaList.size() + " henna data.");
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
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("henna".equals(d.getNodeName()))
					{
						parseHenna(d);
					}
				}
			}
		}
	}
	
	private void parseHenna(Node d)
	{
		final StatsSet set = new StatsSet();
		final List<ClassId> wearClassIds = new ArrayList<>();
		final List<Skill> skillList = new ArrayList<>();
		NamedNodeMap attrs = d.getAttributes();
		Node attr;
		String name;
		for (int i = 0; i < attrs.getLength(); i++)
		{
			attr = attrs.item(i);
			set.set(attr.getNodeName(), attr.getNodeValue());
		}
		
		for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
		{
			name = c.getNodeName();
			attrs = c.getAttributes();
			switch (name)
			{
				case "stats" :
				{
					for (int i = 0; i < attrs.getLength(); i++)
					{
						attr = attrs.item(i);
						set.set(attr.getNodeName(), attr.getNodeValue());
					}
					break;
				}
				case "wear" :
				{
					attr = attrs.getNamedItem("count");
					set.set("wear_count", attr.getNodeValue());
					attr = attrs.getNamedItem("fee");
					set.set("wear_fee", attr.getNodeValue());
					break;
				}
				case "cancel" :
				{
					attr = attrs.getNamedItem("count");
					set.set("cancel_count", attr.getNodeValue());
					attr = attrs.getNamedItem("fee");
					set.set("cancel_fee", attr.getNodeValue());
					break;
				}
				case "classId" :
				{
					wearClassIds.add(ClassId.getClassId(Integer.parseInt(c.getTextContent())));
					break;
				}
				case "skill" :
				{
					final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					final int lvl = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
					final Skill data = SkillsParser.getInstance().getInfo(id, lvl);
					if (data != null)
					{
						skillList.add(data);
					}
					break;
				}
			}
		}
		final Henna henna = new Henna(set);
		henna.setWearClassIds(wearClassIds);
		henna.setSkills(skillList);
		_hennaList.put(henna.getDyeId(), henna);
	}
	
	public Henna getHenna(int id)
	{
		return _hennaList.get(id);
	}

	public boolean isHenna(int itemId)
	{
		for (final Henna henna : _hennaList.values())
		{
			if (henna.getDyeId() == itemId)
			{
				return true;
			}
		}
		return false;
	}
	
	public List<Henna> getHennaList(ClassId classId)
	{
		final List<Henna> list = new ArrayList<>();
		for (final Henna henna : _hennaList.values())
		{
			if (henna.isAllowedClass(classId))
			{
				list.add(henna);
			}
		}
		return list;
	}
	
	public static HennaParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final HennaParser _instance = new HennaParser();
	}
}