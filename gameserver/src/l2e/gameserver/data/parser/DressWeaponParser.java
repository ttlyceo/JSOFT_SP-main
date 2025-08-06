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
import l2e.gameserver.model.actor.templates.DressWeaponTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public final class DressWeaponParser extends DocumentParser
{
	private final List<DressWeaponTemplate> _weapons = new ArrayList<>();
	private final List<DressWeaponTemplate> _activeList = new ArrayList<>();
	
	private DressWeaponParser()
	{
		_weapons.clear();
		_activeList.clear();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/dress/dressWeapon.xml");
		info("Loaded " + _weapons.size() + " dress weapon templates.");
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
					if ("weapon".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap weapon = d.getAttributes();
						
						int itemId = 0;
						long itemCount = 0;
						double removeModifier = 1;
						
						final StatsSet set = new StatsSet();
						
						set.set("id", Integer.parseInt(weapon.getNamedItem("id").getNodeValue()));
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								set.set(name, weapon.getNamedItem(name) != null ? weapon.getNamedItem(name).getNodeValue() : weapon.getNamedItem("nameEn") != null ? weapon.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						
						set.set("type", weapon.getNamedItem("type").getNodeValue());
						set.set("isAllowEnchant", weapon.getNamedItem("allowEnchant") != null ? Boolean.parseBoolean(weapon.getNamedItem("allowEnchant").getNodeValue()) : true);
						set.set("isAllowAugment", weapon.getNamedItem("allowAugment") != null ? Boolean.parseBoolean(weapon.getNamedItem("allowAugment").getNodeValue()) : true);
						final var isForSell = weapon.getNamedItem("isForSell") != null ? Boolean.parseBoolean(weapon.getNamedItem("isForSell").getNodeValue()) : true;
						set.set("isForSell", isForSell);
						
						final List<Skill> skills = new ArrayList<>();
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							weapon = cd.getAttributes();
							
							if ("price".equalsIgnoreCase(cd.getNodeName()))
							{
								itemId = Integer.parseInt(weapon.getNamedItem("id").getNodeValue());
								itemCount = Long.parseLong(weapon.getNamedItem("count").getNodeValue());
							}
							else if ("removed".equalsIgnoreCase(cd.getNodeName()))
							{
								removeModifier = Double.parseDouble(weapon.getNamedItem("modifier").getNodeValue());
							}
							else if ("skill".equalsIgnoreCase(cd.getNodeName()))
							{
								final int skillId = Integer.parseInt(weapon.getNamedItem("id").getNodeValue());
								final int level = Integer.parseInt(weapon.getNamedItem("level").getNodeValue());
								final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
								if (data != null)
								{
									skills.add(data);
								}
							}
						}
						final var tpl = new DressWeaponTemplate(set, itemId, itemCount, skills, removeModifier);
						addWeapon(tpl);
						if (isForSell)
						{
							_activeList.add(tpl);
						}
					}
				}
			}
		}
	}

	public void addWeapon(DressWeaponTemplate weapon)
	{
		_weapons.add(weapon);
	}
	
	public List<DressWeaponTemplate> getAllWeapons()
	{
		return _weapons;
	}
	
	public List<DressWeaponTemplate> getAllActiveWeapons()
	{
		return _activeList;
	}
	
	public DressWeaponTemplate getWeapon(int id)
	{
		for (final DressWeaponTemplate weapon : _weapons)
		{
			if (weapon.getId() == id)
			{
				return weapon;
			}
		}
		return null;
	}
	
	public DressWeaponTemplate getActiveWeapon(int id)
	{
		for (final DressWeaponTemplate weapon : _activeList)
		{
			if (weapon.getId() == id)
			{
				return weapon;
			}
		}
		return null;
	}
	
	public int size()
	{
		return _weapons.size();
	}

	public static DressWeaponParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DressWeaponParser _instance = new DressWeaponParser();
	}
}