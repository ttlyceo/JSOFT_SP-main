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

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.templates.ArmorSetTemplate;
import l2e.gameserver.model.holders.SkillHolder;

public final class ArmorSetsParser extends DocumentParser
{
	private final Map<Integer, ArmorSetTemplate> _armorSets = new HashMap<>();

	protected ArmorSetsParser()
	{
		load();
	}

	@Override
	public void load()
	{
		_armorSets.clear();
		parseDirectory("data/stats/items/armorsets", false);
		info("Loaded " + _armorSets.size() + " armor sets.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		ArmorSetTemplate set;
		for (var n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("set".equalsIgnoreCase(d.getNodeName()))
					{
						set = new ArmorSetTemplate();
						final List<Integer> chests = new ArrayList<>();
						for (var a = d.getFirstChild(); a != null; a = a.getNextSibling())
						{
							attrs = a.getAttributes();
							switch (a.getNodeName())
							{
								case "chest" :
								{
									if (set.getChestId() > 0)
									{
										chests.add(parseInt(attrs, "id"));
									}
									else
									{
										set.addChest(parseInt(attrs, "id"));
									}
									break;
								}
								case "feet" :
								{
									set.addFeet(parseInt(attrs, "id"));
									break;
								}
								case "gloves" :
								{
									set.addGloves(parseInt(attrs, "id"));
									break;
								}
								case "head" :
								{
									set.addHead(parseInt(attrs, "id"));
									break;
								}
								case "legs" :
								{
									set.addLegs(parseInt(attrs, "id"));
									break;
								}
								case "shield" :
								{
									set.addShield(parseInt(attrs, "id"));
									break;
								}
								case "skill" :
								{
									final var skillId = parseInt(attrs, "id");
									final var skillLevel = parseInt(attrs, "level");
									set.addSkill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "shield_skill" :
								{
									final var skillId = parseInt(attrs, "id");
									final var skillLevel = parseInt(attrs, "level");
									set.addShieldSkill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "enchant6skill" :
								{
									final var skillId = parseInt(attrs, "id");
									final var skillLevel = parseInt(attrs, "level");
									set.addEnchant6Skill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "enchantBy" :
								{
									final var enchLvl = parseInteger(attrs, "level");
									final var skillId = parseInteger(attrs, "skillId");
									final var skillLevel = parseInteger(attrs, "skillLvl");
									set.addEnchantByLevel(enchLvl, new SkillHolder(skillId, skillLevel));
									break;
								}
								case "con" :
								{
									set.addCon(parseInt(attrs, "val"));
									break;
								}
								case "dex" :
								{
									set.addDex(parseInt(attrs, "val"));
									break;
								}
								case "str" :
								{
									set.addStr(parseInt(attrs, "val"));
									break;
								}
								case "men" :
								{
									set.addMen(parseInt(attrs, "val"));
									break;
								}
								case "wit" :
								{
									set.addWit(parseInt(attrs, "val"));
									break;
								}
								case "int" :
								{
									set.addInt(parseInt(attrs, "val"));
									break;
								}
							}
						}
						_armorSets.put(set.getChestId(), set);
						if (chests.size() > 0)
						{
							for (final var chestId : chests)
							{
								final ArmorSetTemplate newSet = new ArmorSetTemplate(set);
								newSet.addChest(chestId);
								_armorSets.put(chestId, newSet);
							}
						}
					}
				}
			}
		}
	}

	public boolean isArmorSet(int chestId)
	{
		return _armorSets.containsKey(chestId);
	}

	public ArmorSetTemplate getSet(int chestId)
	{
		return _armorSets.get(chestId);
	}

	public static ArmorSetsParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final ArmorSetsParser _instance = new ArmorSetsParser();
	}
}