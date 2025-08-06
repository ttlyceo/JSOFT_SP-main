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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.instancemanager.DropManager;
import l2e.gameserver.model.actor.templates.npc.AbsorbInfo;
import l2e.gameserver.model.actor.templates.npc.Faction;
import l2e.gameserver.model.actor.templates.npc.MinionData;
import l2e.gameserver.model.actor.templates.npc.MinionTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.reward.RewardList;
import l2e.gameserver.model.reward.RewardType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public class NpcsParser extends DocumentParser
{
	private final Map<Integer, NpcTemplate> _npcs = new HashMap<>();
	
	protected NpcsParser()
	{
		load();
	}
	
	@Override
	public final void load()
	{
		_npcs.clear();
		parseDirectory("data/stats/npcs/npcs", false);
		if (Config.CUSTOM_NPC)
		{
			parseDirectory("data/stats/npcs/npcs/custom", false);
		}
		info("Loaded " + _npcs.size() + " npc templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
		for (Node c = getCurrentDocument().getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("list".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node npc = c.getFirstChild(); npc != null; npc = npc.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(npc.getNodeName()))
					{
						NamedNodeMap attrs = npc.getAttributes();
						
						final int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						
						final NpcTemplate template = getTemplate(npcId);
						if (template != null)
						{
							for (Node cat = npc.getFirstChild(); cat != null; cat = cat.getNextSibling())
							{
								if ("rewardlist".equalsIgnoreCase(cat.getNodeName()))
								{
									final RewardType type = RewardType.valueOf(cat.getAttributes().getNamedItem("type").getNodeValue());
									template.putRewardList(type, RewardList.parseRewardList(_log, cat, cat.getAttributes(), type, template.isEpicRaid() || template.isRaid(), String.valueOf(npcId)));
								}
								else if ("skills".equalsIgnoreCase(cat.getNodeName()))
								{
									for (Node skillCat = cat.getFirstChild(); skillCat != null; skillCat = skillCat.getNextSibling())
									{
										if ("skill".equalsIgnoreCase(skillCat.getNodeName()))
										{
											attrs = skillCat.getAttributes();
											
											final int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
											final int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
											
											if (skillId == Skill.SKILL_NPC_RACE)
											{
												template.setRace(level);
												continue;
											}
											
											final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
											if (data != null)
											{
												template.addSkill(data);
											}
											else
											{
												warn("[" + getCurrentFile().getName() + "] skill not found. NPC ID: " + npcId + " Skill ID:" + skillId + " Skill Level: " + level);
											}
										}
									}
								}
							}
						}
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
				for (Node npc = c.getFirstChild(); npc != null; npc = npc.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(npc.getNodeName()))
					{
						NamedNodeMap attrs = npc.getAttributes();

						final int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						final int templateId = attrs.getNamedItem("templateId") == null ? npcId : Integer.parseInt(attrs.getNamedItem("templateId").getNodeValue());
						
						final StatsSet set = new StatsSet();
						set.set("npcId", npcId);
						set.set("displayId", templateId);
						set.set("baseCpReg", 0);
						set.set("baseCpMax", 0);
						for (final String lang : Config.MULTILANG_ALLOWED)
						{
							if (lang != null)
							{
								final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								final String title = "title" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
								set.set(name, attrs.getNamedItem(name) != null ? attrs.getNamedItem(name).getNodeValue() : attrs.getNamedItem("nameEn") != null ? attrs.getNamedItem("nameEn").getNodeValue() : "");
								set.set(title, attrs.getNamedItem(title) != null ? attrs.getNamedItem(title).getNodeValue() : attrs.getNamedItem("titleEn") != null ? attrs.getNamedItem("titleEn").getNodeValue() : "");
							}
						}

						for (Node cat = npc.getFirstChild(); cat != null; cat = cat.getNextSibling())
						{
							if ("set".equalsIgnoreCase(cat.getNodeName()))
							{
								attrs = cat.getAttributes();
								set.set(attrs.getNamedItem("name").getNodeValue(), attrs.getNamedItem("value").getNodeValue());
							}
							else if ("equip".equalsIgnoreCase(cat.getNodeName()))
							{
								for (Node equip = cat.getFirstChild(); equip != null; equip = equip.getNextSibling())
								{
									if ("rhand".equalsIgnoreCase(equip.getNodeName()))
									{
										set.set(equip.getNodeName(), Integer.parseInt(equip.getAttributes().getNamedItem("itemId").getNodeValue()));
									}
									if ("lhand".equalsIgnoreCase(equip.getNodeName()))
									{
										set.set(equip.getNodeName(), Integer.parseInt(equip.getAttributes().getNamedItem("itemId").getNodeValue()));
									}
								}
							}
						}

						final NpcTemplate template = new NpcTemplate(set);

						for (Node cat = npc.getFirstChild(); cat != null; cat = cat.getNextSibling())
						{
							if ("faction".equalsIgnoreCase(cat.getNodeName()))
							{
								final String factionId = cat.getAttributes().getNamedItem("name").getNodeValue();
								final Faction faction = new Faction(factionId);
								final int factionRange = Integer.parseInt(cat.getAttributes().getNamedItem("range").getNodeValue());
								faction.setRange(factionRange);
								for (Node ignore = cat.getFirstChild(); ignore != null; ignore = ignore.getNextSibling())
								{
									if ("ignore".equalsIgnoreCase(ignore.getNodeName()))
									{
										final int ignoreId = Integer.parseInt(ignore.getAttributes().getNamedItem("npcId").getNodeValue());
										faction.addIgnoreNpcId(ignoreId);
									}
								}
								template.setFaction(faction);
							}
							else if ("ai_params".equalsIgnoreCase(cat.getNodeName()))
							{
								final StatsSet ai = new StatsSet();
								for (Node params = cat.getFirstChild(); params != null; params = params.getNextSibling())
								{
									if ("set".equalsIgnoreCase(params.getNodeName()))
									{
										ai.set(params.getAttributes().getNamedItem("name").getNodeValue(), params.getAttributes().getNamedItem("value").getNodeValue());
									}
								}
								template.setParameters(ai);
							}
							else if ("attributes".equalsIgnoreCase(cat.getNodeName()))
							{
								for (Node attribute = cat.getFirstChild(); attribute != null; attribute = attribute.getNextSibling())
								{
									if ("attack".equalsIgnoreCase(attribute.getNodeName()))
									{
										attrs = attribute.getAttributes();
										switch (attrs.getNamedItem("attribute").getNodeValue())
										{
											case "fire" :
												template.setBaseFire(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "wind" :
												template.setBaseWind(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "water" :
												template.setBaseWater(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "earth" :
												template.setBaseEarth(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "holy" :
												template.setBaseHoly(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "unholy" :
												template.setBaseDark(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
										}
									}
									else if ("defence".equalsIgnoreCase(attribute.getNodeName()))
									{
										attrs = attribute.getAttributes();
										switch (attrs.getNamedItem("attribute").getNodeValue())
										{
											case "fire" :
												template.setBaseFireRes(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "wind" :
												template.setBaseWindRes(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "water" :
												template.setBaseWaterRes(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "earth" :
												template.setBaseEarthRes(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "holy" :
												template.setBaseHolyRes(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
											case "unholy" :
												template.setBaseDarkRes(Integer.parseInt(attrs.getNamedItem("value").getNodeValue()));
												break;
										}
									}
								}
							}
							else if ("minions".equalsIgnoreCase(cat.getNodeName()))
							{
								for (Node minion = cat.getFirstChild(); minion != null; minion = minion.getNextSibling())
								{
									if ("random".equalsIgnoreCase(minion.getNodeName()))
									{
										for (Node m = minion.getFirstChild(); m != null; m = m.getNextSibling())
										{
											if ("minion".equalsIgnoreCase(m.getNodeName()))
											{
												final List<MinionTemplate> minions = new ArrayList<>();
												attrs = m.getAttributes();
												final String[] minionsList = attrs.getNamedItem("list").getNodeValue().split(";");
												for (final String minionId : minionsList)
												{
													final String[] minionSplit = minionId.split(",");
													if (minionSplit.length == 2)
													{
														minions.add(new MinionTemplate(Integer.parseInt(minionSplit[0]), Integer.parseInt(minionSplit[1])));
													}
												}
												
												if (!minions.isEmpty())
												{
													template.addRaidData(new MinionData(minions), true);
												}
											}
										}
									}
									else if ("minion".equalsIgnoreCase(minion.getNodeName()))
									{
										attrs = minion.getAttributes();
										template.addRaidData(new MinionData(new MinionTemplate(Integer.parseInt(attrs.getNamedItem("npcId").getNodeValue()), Integer.parseInt(attrs.getNamedItem("count").getNodeValue()))), false);
									}
								}
							}
							else if ("skills".equalsIgnoreCase(cat.getNodeName()))
							{
								for (Node skillCat = cat.getFirstChild(); skillCat != null; skillCat = skillCat.getNextSibling())
								{
									if ("skill".equalsIgnoreCase(skillCat.getNodeName()))
									{
										attrs = skillCat.getAttributes();

										final int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
										final int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());

										if (skillId == Skill.SKILL_NPC_RACE)
										{
											template.setRace(level);
											continue;
										}

										final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
										if (data != null)
										{
											template.addSkill(data);
										}
										else
										{
											warn("[" + getCurrentFile().getName() + "] skill not found. NPC ID: " + npcId + " Skill ID:" + skillId + " Skill Level: " + level);
										}
									}
								}
							}
							else if ("teach_classes".equalsIgnoreCase(cat.getNodeName()))
							{
								final List<ClassId> teachInfo = new ArrayList<>();
								for (Node teach = cat.getFirstChild(); teach != null; teach = teach.getNextSibling())
								{
									if ("class".equalsIgnoreCase(teach.getNodeName()))
									{
										final int id = Integer.parseInt(teach.getAttributes().getNamedItem("id").getNodeValue());
										teachInfo.add(ClassId.values()[id]);
									}
								}
								template.addTeachInfo(teachInfo);
							}
							else if ("absorblist".equalsIgnoreCase(cat.getNodeName()))
							{
								for (Node absorb = cat.getFirstChild(); absorb != null; absorb = absorb.getNextSibling())
								{
									if ("absorb".equalsIgnoreCase(absorb.getNodeName()))
									{
										final int chance = Integer.parseInt(absorb.getAttributes().getNamedItem("chance").getNodeValue());
										final int cursedChance = absorb.getAttributes().getNamedItem("cursed_chance") == null ? 0 : Integer.parseInt(absorb.getAttributes().getNamedItem("cursed_chance").getNodeValue());
										final int minLevel = Integer.parseInt(absorb.getAttributes().getNamedItem("min_level").getNodeValue());
										final int maxLevel = Integer.parseInt(absorb.getAttributes().getNamedItem("max_level").getNodeValue());
										final boolean skill = absorb.getAttributes().getNamedItem("skill") != null && Boolean.parseBoolean(absorb.getAttributes().getNamedItem("skill").getNodeValue());
										final AbsorbInfo.AbsorbType absorbType = AbsorbInfo.AbsorbType.valueOf(absorb.getAttributes().getNamedItem("type").getNodeValue());
										
										template.addAbsorbInfo(new AbsorbInfo(skill, absorbType, chance, cursedChance, minLevel, maxLevel));
									}
								}
							}
							else if ("rewardlist".equalsIgnoreCase(cat.getNodeName()))
							{
								final RewardType type = RewardType.valueOf(cat.getAttributes().getNamedItem("type").getNodeValue());
								template.putRewardList(type, RewardList.parseRewardList(_log, cat, cat.getAttributes(), type, template.isEpicRaid() || template.isRaid(), String.valueOf(npcId)));
							}
						}
						_npcs.put(npcId, template);
					}
				}
			}
		}
	}
	
	public NpcTemplate getTemplate(int id)
	{
		return _npcs.get(id);
	}
	
	public NpcTemplate getTemplateByName(String name)
	{
		for (final NpcTemplate npcTemplate : _npcs.values())
		{
			for (final String lang : Config.MULTILANG_ALLOWED)
			{
				if (lang != null)
				{
					if (npcTemplate.getName(lang).equalsIgnoreCase(name))
					{
						return npcTemplate;
					}
				}
			}
		}
		return null;
	}
	
	public Collection<NpcTemplate> getAllNpcs()
	{
		return _npcs.values();
	}
	
	public List<NpcTemplate> getAllOfLevel(int... lvls)
	{
		final List<NpcTemplate> list = new ArrayList<>();
		for (final int lvl : lvls)
		{
			for (final NpcTemplate t : _npcs.values())
			{
				if (t.getLevel() == lvl)
				{
					list.add(t);
				}
			}
		}
		return list;
	}

	public List<NpcTemplate> getAllMonstersOfLevel(int... lvls)
	{
		final List<NpcTemplate> list = new ArrayList<>();
		for (final int lvl : lvls)
		{
			for (final NpcTemplate t : _npcs.values())
			{
				if ((t.getLevel() == lvl) && t.isType("Monster"))
				{
					list.add(t);
				}
			}
		}
		return list;
	}

	public List<NpcTemplate> getAllNpcStartingWith(String... letters)
	{
		final List<NpcTemplate> list = new ArrayList<>();
		for (final String letter : letters)
		{
			for (final NpcTemplate t : _npcs.values())
			{
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null)
					{
						if (t.getName(lang).startsWith(letter) && t.isType("Npc"))
						{
							list.add(t);
						}
					}
				}
			}
		}
		return list;
	}

	public List<NpcTemplate> getAllNpcOfClassType(String... classTypes)
	{
		final List<NpcTemplate> list = new ArrayList<>();
		for (final String classType : classTypes)
		{
			for (final NpcTemplate t : _npcs.values())
			{
				if (t.isType(classType))
				{
					list.add(t);
				}
			}
		}
		return list;
	}
	
	public void reloadAllDropAndSkills()
	{
		for (final NpcTemplate template : getAllNpcs())
		{
			if (template != null)
			{
				if (template.getRewards() != null)
				{
					template.getRewards().clear();
				}
				template.getSkills().clear();
			}
		}
		
		parseDirectory("data/stats/npcs/npcs", true);
		if (Config.CUSTOM_NPC)
		{
			parseDirectory("data/stats/npcs/npcs/custom", true);
		}
		info("Reloaded all npc drop templates.");
		DropManager.getInstance().reload();
	}
	
	public static NpcsParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcsParser _instance = new NpcsParser();
	}
}