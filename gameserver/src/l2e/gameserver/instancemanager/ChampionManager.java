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
package l2e.gameserver.instancemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionRewardItem;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.stats.Stats;

/**
 * Created by LordWinter
 */
public class ChampionManager extends LoggerObject
{
	public boolean ENABLE_EXT_CHAMPION_MODE = false;
	public int EXT_CHAMPION_MODE_MAX_ROLL_VALUE = 0;
	private final List<ChampionTemplate> _championTemplates = new ArrayList<>();

	public ChampionManager()
	{
		_championTemplates.clear();
		championParser();
	}

	private void championParser()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/config/mods/championTemplate.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final Document doc = factory.newDocumentBuilder().parse(file);
			final Node first = doc.getFirstChild();
			if (first != null && "list".equalsIgnoreCase(first.getNodeName()))
			{
				NamedNodeMap attrs = first.getAttributes();
				Node att = attrs.getNamedItem("enabled");
				try
				{
					ENABLE_EXT_CHAMPION_MODE = Boolean.parseBoolean(att.getNodeValue());
					att = attrs.getNamedItem("maxRollValue");
					EXT_CHAMPION_MODE_MAX_ROLL_VALUE = Integer.parseInt(att.getNodeValue());
				}
				catch (final Exception e)
				{
					warn("Failed to load initial list params, mode skipped.");
					e.printStackTrace();
				}

				if (!ENABLE_EXT_CHAMPION_MODE)
				{
					return;
				}

				ChampionTemplate ct;

				for (Node n = first.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("champion".equalsIgnoreCase(n.getNodeName()))
					{
						ct = new ChampionTemplate();
						attrs = n.getAttributes();

						if ((att = attrs.getNamedItem("minChance")) == null)
						{
							warn("Missing minChance, skipping");
							continue;
						}
						ct.minChance = Integer.parseInt(att.getNodeValue());

						if ((att = attrs.getNamedItem("maxChance")) == null)
						{
							warn("Missing maxChance, skipping");
							continue;
						}
						ct.maxChance = Integer.parseInt(att.getNodeValue());

						if ((att = attrs.getNamedItem("minLevel")) == null)
						{
							warn("Missing minLevel, skipping");
							continue;
						}
						ct.minLevel = Integer.parseInt(att.getNodeValue());

						if ((att = attrs.getNamedItem("maxLevel")) == null)
						{
							warn("Missing maxLevel, skipping");
							continue;
						}
						ct.maxLevel = Integer.parseInt(att.getNodeValue());

						for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							try
							{
								switch (cd.getNodeName())
								{
									case "setByte" :
										ct.getClass().getField(cd.getAttributes().item(0).getNodeName()).setByte(ct, Byte.parseByte(cd.getAttributes().item(0).getNodeValue()));
										break;
									case "setBoolean" :
										ct.getClass().getField(cd.getAttributes().item(0).getNodeName()).setBoolean(ct, Boolean.parseBoolean(cd.getAttributes().item(0).getNodeValue()));
										break;
									case "setDouble" :
										ct.getClass().getField(cd.getAttributes().item(0).getNodeName()).setDouble(ct, Double.parseDouble(cd.getAttributes().item(0).getNodeValue()));
										break;
									case "setFloat" :
										ct.getClass().getField(cd.getAttributes().item(0).getNodeName()).setFloat(ct, Float.parseFloat(cd.getAttributes().item(0).getNodeValue()));
										break;
									case "setInt" :
										ct.getClass().getField(cd.getAttributes().item(0).getNodeName()).setInt(ct, Integer.parseInt(cd.getAttributes().item(0).getNodeValue()));
										break;
									case "setLong" :
										ct.getClass().getField(cd.getAttributes().item(0).getNodeName()).setLong(ct, Long.parseLong(cd.getAttributes().item(0).getNodeValue()));
										break;
									case "setString" :
										ct.getClass().getField(cd.getAttributes().item(0).getNodeName()).set(ct, cd.getAttributes().item(0).getNodeValue());
										break;
									case "item" :
										final int itemId = Integer.parseInt(cd.getAttributes().getNamedItem("itemId").getNodeValue());
										final long minCount = Long.parseLong(cd.getAttributes().getNamedItem("minCount").getNodeValue());
										final long maxCount = Long.parseLong(cd.getAttributes().getNamedItem("maxCount").getNodeValue());
										final double dropChance = Double.parseDouble(cd.getAttributes().getNamedItem("dropChance").getNodeValue());
										ct.rewards.add(new ChampionRewardItem(itemId, minCount, maxCount, dropChance));
										break;
									case "abnormalEffect" :
										final AbnormalEffect ae = AbnormalEffect.getByName(cd.getAttributes().getNamedItem("name").getNodeValue());
										if (ae != null)
										{
											ct.abnormalEffect.add(ae);
										}
										break;
									case "maxSpoilItemsFromOneGroup" :
										ct.maxSpoilItemsFromOneGroup = Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue());
										break;
									case "maxDropItemsFromOneGroup" :
										ct.maxDropItemsFromOneGroup = Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue());
										break;
									case "maxRaidDropItemsFromOneGroup" :
										ct.maxRaidDropItemsFromOneGroup = Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue());
										break;
									case "npcIdList" :
										final String line = cd.getAttributes().getNamedItem("val").getNodeValue();
										for (final String l : line.split(","))
										{
											if (l != null)
											{
												ct.npcIdList.add(Integer.parseInt(l));
											}
										}
										break;
									case "skills" :
										for (Node skillCat = cd.getFirstChild(); skillCat != null; skillCat = skillCat.getNextSibling())
										{
											if ("skill".equalsIgnoreCase(skillCat.getNodeName()))
											{
												attrs = skillCat.getAttributes();

												final int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
												final int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());

												final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
												if (data != null)
												{
													ct.skills.add(data);
												}
												else
												{
													warn("Skill not found. Skill ID:" + skillId + " Skill Level: " + level);
												}
											}
										}
										break;
									case "resists" :
										final Map<Stats, Double> resists = new HashMap<>();
										for (Node rs = cd.getFirstChild(); rs != null; rs = rs.getNextSibling())
										{
											if ("stat".equalsIgnoreCase(rs.getNodeName()))
											{
												final Stats stat = Stats.valueOf(rs.getAttributes().getNamedItem("type").getNodeValue());
												final double value = Double.parseDouble(rs.getAttributes().getNamedItem("val").getNodeValue());
												resists.put(stat, value);
											}
										}
										ct._resists = resists;
										break;
								}
							}
							catch (final NoSuchFieldException nsfe)
							{
								warn("The variable [" + att.getNodeName() + "] which is set in the XML config was not found in the java file.");
								nsfe.printStackTrace();
							}
							catch (final Exception e)
							{
								warn("A problem occured while setting a value to the variable [" + att.getNodeName() + "]");
								e.printStackTrace();
							}
						}
						_championTemplates.add(ct);
					}
				}
			}
		}
		catch (final Exception e)
		{
			ENABLE_EXT_CHAMPION_MODE = false;
			warn("Failed to parse xml: " + e.getMessage(), e);
		}
		info("Loaded " + _championTemplates.size() + " champion templates.");
	}

	public List<ChampionTemplate> getChampionTemplates()
	{
		return _championTemplates;
	}
	
	public static final ChampionManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ChampionManager _instance = new ChampionManager();
	}
}