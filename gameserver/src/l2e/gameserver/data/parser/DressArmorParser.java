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
import l2e.gameserver.model.actor.templates.DressArmorTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.type.ArmorType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public final class DressArmorParser extends DocumentParser
{
	private final List<DressArmorTemplate> _dress = new ArrayList<>();
	private final List<DressArmorTemplate> _activeList = new ArrayList<>();
	
	protected DressArmorParser()
	{
		_dress.clear();
		_activeList.clear();
		load();
	}

	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/stats/services/dress/dressArmor.xml");
		info("Loaded " + _dress.size() + " dress armor templates.");
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
					if ("dress".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap dress = d.getAttributes();

						int itemId = 0;
						long itemCount = 0L;
						double removeModifier = 1;
						
						final StatsSet set = new StatsSet();

						set.set("id", Integer.parseInt(dress.getNamedItem("id").getNodeValue()));
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								set.set(name, dress.getNamedItem(name) != null ? dress.getNamedItem(name).getNodeValue() : dress.getNamedItem("nameEn") != null ? dress.getNamedItem("nameEn").getNodeValue() : "");
							}
						}
						set.set("checkEquip", dress.getNamedItem("checkEquip") != null ? Boolean.parseBoolean(dress.getNamedItem("checkEquip").getNodeValue()) : true);
						final var isForSell = dress.getNamedItem("isForSell") != null ? Boolean.parseBoolean(dress.getNamedItem("isForSell").getNodeValue()) : true;
						set.set("isForSell", isForSell);
						final List<Skill> skills = new ArrayList<>();
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							final NamedNodeMap dr = cd.getAttributes();

							if ("set".equalsIgnoreCase(cd.getNodeName()))
							{
								set.set("chest", Integer.parseInt(dr.getNamedItem("chest").getNodeValue()));
								set.set("legs", Integer.parseInt(dr.getNamedItem("legs").getNodeValue()));
								set.set("gloves", Integer.parseInt(dr.getNamedItem("gloves").getNodeValue()));
								set.set("feet", Integer.parseInt(dr.getNamedItem("feet").getNodeValue()));
								set.set("shield", dr.getNamedItem("shield") != null ? Integer.parseInt(dr.getNamedItem("shield").getNodeValue()) : -1);
								set.set("cloak", dr.getNamedItem("cloak") != null ? Integer.parseInt(dr.getNamedItem("cloak").getNodeValue()) : -1);
								set.set("hat", dr.getNamedItem("hat") != null ? Integer.parseInt(dr.getNamedItem("hat").getNodeValue()) : -1);
								set.set("slot", dr.getNamedItem("cloak") != null ? Integer.parseInt(dr.getNamedItem("slot").getNodeValue()) : -1);
							}
							else if ("price".equalsIgnoreCase(cd.getNodeName()))
							{
								itemId = Integer.parseInt(dr.getNamedItem("id").getNodeValue());
								itemCount = Long.parseLong(dr.getNamedItem("count").getNodeValue());
							}
							else if ("removed".equalsIgnoreCase(cd.getNodeName()))
							{
								removeModifier = Double.parseDouble(dr.getNamedItem("modifier").getNodeValue());
							}
							else if ("skill".equalsIgnoreCase(cd.getNodeName()))
							{
								final int skillId = Integer.parseInt(dr.getNamedItem("id").getNodeValue());
								final int level = Integer.parseInt(dr.getNamedItem("level").getNodeValue());
								final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
								if (data != null)
								{
									skills.add(data);
								}
							}
						}
						final var isForKamael = dress.getNamedItem("isForKamael") != null ? Boolean.parseBoolean(dress.getNamedItem("isForKamael").getNodeValue()) : isForKamael(set.getInteger("chest"));
						set.set("isForKamael", isForKamael);
						
						final var tpl = new DressArmorTemplate(set, itemId, itemCount, skills, removeModifier);
						addDress(tpl);
						if (isForSell)
						{
							_activeList.add(tpl);
						}
					}
				}
			}
		}
	}

	public void addDress(DressArmorTemplate armorset)
	{
		_dress.add(armorset);
	}
	
	public List<DressArmorTemplate> getAllDress()
	{
		return _dress;
	}
	
	public List<DressArmorTemplate> getAllActiveDress()
	{
		return _activeList;
	}
	
	public DressArmorTemplate getArmor(int id)
	{
		for (final DressArmorTemplate dress : _dress)
		{
			if (dress.getId() == id)
			{
				return dress;
			}
		}
		return null;
	}
	
	public DressArmorTemplate getActiveArmor(int id)
	{
		for (final DressArmorTemplate dress : _activeList)
		{
			if (dress.getId() == id)
			{
				return dress;
			}
		}
		return null;
	}
	
	public DressArmorTemplate getArmorByPartId(int partId)
	{
		for (final DressArmorTemplate dress : _dress)
		{
			if (dress.getChest() == partId || dress.getLegs() == partId || dress.getGloves() == partId || dress.getFeet() == partId)
			{
				return dress;
			}
		}
		return null;
	}
	
	public int size()
	{
		return _dress.size();
	}
	
	private boolean isForKamael(int itemId)
	{
		final Item item = ItemsParser.getInstance().getTemplate(itemId);
		if (item != null)
		{
			if ((item.getItemType() == ArmorType.HEAVY) || (item.getItemType() == ArmorType.MAGIC))
			{
				if (item.getBodyPart() == Item.SLOT_ALLDRESS)
				{
					return true;
				}
				return false;
			}
			return true;
		}
		return false;
	}

	public static DressArmorParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final DressArmorParser _instance = new DressArmorParser();
	}
}