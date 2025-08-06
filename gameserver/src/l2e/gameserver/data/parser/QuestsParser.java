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

import l2e.gameserver.Config;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.actor.templates.quest.QuestDropItem;
import l2e.gameserver.model.actor.templates.quest.QuestExperience;
import l2e.gameserver.model.actor.templates.quest.QuestRewardItem;
import l2e.gameserver.model.actor.templates.quest.QuestTemplate;
import l2e.gameserver.model.stats.StatsSet;

/**
 * Create by LordWinter 04.12.2019
 */
public class QuestsParser extends DocumentParser
{
	private final Map<Integer, QuestTemplate> _quests = new HashMap<>();
	
	protected QuestsParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_quests.clear();
		parseDirectory("data/stats/quests", false);
		info("Loaded " + _quests.size() + " quest templates.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node list = getCurrentDocument().getFirstChild().getFirstChild(); list != null; list = list.getNextSibling())
		{
			if (list.getNodeName().equalsIgnoreCase("quest"))
			{
				NamedNodeMap node = list.getAttributes();
				
				final int id = Integer.valueOf(node.getNamedItem("id").getNodeValue());
				final StatsSet params = new StatsSet();
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null)
					{
						final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
						params.set(name, node.getNamedItem(name) != null ? node.getNamedItem(name).getNodeValue() : node.getNamedItem("nameEn") != null ? node.getNamedItem("nameEn").getNodeValue() : "");
					}
				}
				long expReward = 0, spReward = 0;
				double expRate = 0, spRate = 0;
				final boolean rateable = false;
				boolean expRateable = false, spRateable = false;
				QuestExperience experience = null;
				List<QuestRewardItem> rewards = null;
				final Map<Integer, List<QuestRewardItem>> groupRewards = new HashMap<>();
				List<QuestRewardItem> rewardList = null;
				Map<Integer, List<QuestDropItem>> dropList = null;
				List<QuestDropItem> itemList = null;
				int minLvl = 0, maxLvl = 0;
				
				for (Node quest = list.getFirstChild(); quest != null; quest = quest.getNextSibling())
				{
					if (quest.getNodeName().equalsIgnoreCase("level"))
					{
						node = quest.getAttributes();
						minLvl = node.getNamedItem("min") != null ? Integer.parseInt(node.getNamedItem("min").getNodeValue()) : 1;
						maxLvl = node.getNamedItem("max") != null ? Integer.parseInt(node.getNamedItem("max").getNodeValue()) : 85;
					}
					else if (quest.getNodeName().equalsIgnoreCase("expirience"))
					{
						for (Node exp = quest.getFirstChild(); exp != null; exp = exp.getNextSibling())
						{
							if (exp.getNodeName().equalsIgnoreCase("rewardExp"))
							{
								node = exp.getAttributes();
								expRate = node.getNamedItem("rate") != null ? Double.valueOf(node.getNamedItem("rate").getNodeValue()) : 1;
								expReward = Long.valueOf(node.getNamedItem("val").getNodeValue());
								expRateable = node.getNamedItem("rateable") != null ? Boolean.parseBoolean(node.getNamedItem("rateable").getNodeValue()) : false;
							}
							else if (exp.getNodeName().equalsIgnoreCase("rewardSp"))
							{
								node = exp.getAttributes();
								spRate = node.getNamedItem("rate") != null ? Double.valueOf(node.getNamedItem("rate").getNodeValue()) : 1;
								spReward = Long.valueOf(node.getNamedItem("val").getNodeValue());
								spRateable = node.getNamedItem("rateable") != null ? Boolean.parseBoolean(node.getNamedItem("rateable").getNodeValue()) : false;
							}
						}
					}
					else if (quest.getNodeName().equalsIgnoreCase("droplist"))
					{
						dropList = new HashMap<>();
						for (Node drop = quest.getFirstChild(); drop != null; drop = drop.getNextSibling())
						{
							if (drop.getNodeName().equalsIgnoreCase("npc"))
							{
								itemList = new ArrayList<>();
								final int npcId = Integer.valueOf(drop.getAttributes().getNamedItem("id").getNodeValue());
								for (Node group = drop.getFirstChild(); group != null; group = group.getNextSibling())
								{
									if (group.getNodeName().equalsIgnoreCase("item"))
									{
										node = group.getAttributes();
										final int itemId = Integer.valueOf(node.getNamedItem("id").getNodeValue());
										final double rate = node.getNamedItem("rate") != null ? Double.valueOf(node.getNamedItem("rate").getNodeValue()) : 1;
										final long min = Long.valueOf(node.getNamedItem("min").getNodeValue());
										final long max = node.getNamedItem("max") != null ? Long.valueOf(node.getNamedItem("max").getNodeValue()) : 0;
										final double chance = node.getNamedItem("chance") != null ? Double.valueOf(node.getNamedItem("chance").getNodeValue()) : 1;
										final boolean isRateable = node.getNamedItem("rateable") != null ? Boolean.parseBoolean(node.getNamedItem("rateable").getNodeValue()) : false;
										itemList.add(new QuestDropItem(itemId, rate, min, max, chance, isRateable));
									}
								}
								dropList.put(npcId, itemList);
							}
						}
					}
					else if (quest.getNodeName().equalsIgnoreCase("add_parameters"))
					{
						for (Node sp = quest.getFirstChild(); sp != null; sp = sp.getNextSibling())
						{
							if ("set".equalsIgnoreCase(sp.getNodeName()))
							{
								params.set(sp.getAttributes().getNamedItem("name").getNodeValue(), sp.getAttributes().getNamedItem("value").getNodeValue());
							}
						}
					}
					else if (quest.getNodeName().equalsIgnoreCase("rewardlist"))
					{
						rewards = new ArrayList<>();
						for (Node reward = quest.getFirstChild(); reward != null; reward = reward.getNextSibling())
						{
							if (reward.getNodeName().equalsIgnoreCase("item"))
							{
								node = reward.getAttributes();
								final int itemId = Integer.valueOf(node.getNamedItem("id").getNodeValue());
								final double rate = node.getNamedItem("rate") != null ? Double.valueOf(node.getNamedItem("rate").getNodeValue()) : 1;
								final long min = Long.valueOf(node.getNamedItem("min").getNodeValue());
								final long max = node.getNamedItem("max") != null ? Long.valueOf(node.getNamedItem("max").getNodeValue()) : 0;
								final boolean isRateable = node.getNamedItem("rateable") != null ? Boolean.parseBoolean(node.getNamedItem("rateable").getNodeValue()) : false;
								rewards.add(new QuestRewardItem(itemId, rate, min, max, isRateable));
							}
							else if (reward.getNodeName().equalsIgnoreCase("variant"))
							{
								rewardList = new ArrayList<>();
								final int varId = Integer.valueOf(reward.getAttributes().getNamedItem("id").getNodeValue());
								for (Node group = reward.getFirstChild(); group != null; group = group.getNextSibling())
								{
									if (group.getNodeName().equalsIgnoreCase("item"))
									{
										node = group.getAttributes();
										final int itemId = Integer.valueOf(node.getNamedItem("id").getNodeValue());
										final double rate = node.getNamedItem("rate") != null ? Double.valueOf(node.getNamedItem("rate").getNodeValue()) : 1;
										final long min = Long.valueOf(node.getNamedItem("min").getNodeValue());
										final long max = node.getNamedItem("max") != null ? Long.valueOf(node.getNamedItem("max").getNodeValue()) : 0;
										final boolean isRateable = node.getNamedItem("rateable") != null ? Boolean.parseBoolean(node.getNamedItem("rateable").getNodeValue()) : false;
										rewardList.add(new QuestRewardItem(itemId, rate, min, max, isRateable));
									}
								}
								groupRewards.put(varId, rewardList);
							}
						}
					}
				}
				
				if (expReward != 0 || spReward != 0)
				{
					experience = new QuestExperience(expReward, spReward, expRate, spRate, expRateable, spRateable);
				}
				final QuestTemplate template = new QuestTemplate(id, minLvl, maxLvl, dropList, experience, rewards, groupRewards, rateable, params);
				_quests.put(id, template);
			}
		}
	}
	
	public QuestTemplate getTemplate(int id)
	{
		return _quests.get(id);
	}
	
	public static QuestsParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final QuestsParser _instance = new QuestsParser();
	}
}