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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.reward.RewardGroup;
import l2e.gameserver.model.reward.RewardList;
import l2e.gameserver.model.reward.RewardType;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter 24.12.2020
 */
public class DropManager extends LoggerObject
{
	public DropManager()
	{
		loadDropSettings();
		loadSkillSettings();
	}
	
	public void reload()
	{
		loadDropSettings();
		loadSkillSettings();
	}

	private void loadDropSettings()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/customDrop.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);

			int counter = 0;
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (Node cat = n1.getFirstChild(); cat != null; cat = cat.getNextSibling())
					{
						if ("rewardlist".equalsIgnoreCase(cat.getNodeName()))
						{
							final int id = Integer.parseInt(cat.getAttributes().getNamedItem("id").getNodeValue());
							final RewardType type = RewardType.valueOf(cat.getAttributes().getNamedItem("type").getNodeValue());
							final String lvlDiff = cat.getAttributes().getNamedItem("lvlDiff") != null ? cat.getAttributes().getNamedItem("lvlDiff").getNodeValue() : "1-85";
							final String npcType = cat.getAttributes().getNamedItem("npcType") != null ? cat.getAttributes().getNamedItem("npcType").getNodeValue() : "";
							final String npcId = cat.getAttributes().getNamedItem("npcId") != null ? cat.getAttributes().getNamedItem("npcId").getNodeValue() : "";
							final String forbiddenId = cat.getAttributes().getNamedItem("forbiddenList") != null ? cat.getAttributes().getNamedItem("forbiddenList").getNodeValue() : "";
							List<Integer> forbiddenList = null;
							if (!forbiddenId.isEmpty())
							{
								final String[] list = forbiddenId.split(";");
								forbiddenList = new ArrayList<>(list.length);
								for (final String nId : list)
								{
									forbiddenList.add(Integer.parseInt(nId));
								}
							}
							
							final Map<RewardType, List<RewardGroup>> groupList = new HashMap<>();
							for (Node reward = cat.getFirstChild(); reward != null; reward = reward.getNextSibling())
							{
								final RewardList rewards = RewardList.parseRewardList(_log, cat, cat.getAttributes(), type, (!npcType.isEmpty() && (npcType.equals("RaidBoss") || npcType.equals("GrandBoss"))), String.valueOf(id));
								if (rewards != null)
								{
									groupList.put(type, rewards);
								}
							}
							addDropToNpc(groupList, lvlDiff, npcType, npcId, forbiddenList);
							counter++;
						}
					}
				}
			}
			info("Loaded " + counter + " custom drop templates.");
		}
		catch (
		    NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("customDrop.xml could not be initialized.", e);
		}
		catch (
		    IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	private void loadSkillSettings()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/customSkills.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);
			
			int counter = 0;
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (Node cat = n1.getFirstChild(); cat != null; cat = cat.getNextSibling())
					{
						if ("npclist".equalsIgnoreCase(cat.getNodeName()))
						{
							final String lvlDiff = cat.getAttributes().getNamedItem("lvlDiff") != null ? cat.getAttributes().getNamedItem("lvlDiff").getNodeValue() : "1-85";
							final String npcType = cat.getAttributes().getNamedItem("npcType") != null ? cat.getAttributes().getNamedItem("npcType").getNodeValue() : "";
							final String npcId = cat.getAttributes().getNamedItem("npcId") != null ? cat.getAttributes().getNamedItem("npcId").getNodeValue() : "";
							final String forbiddenId = cat.getAttributes().getNamedItem("forbiddenList") != null ? cat.getAttributes().getNamedItem("forbiddenList").getNodeValue() : "";
							List<Integer> forbiddenList = null;
							if (!forbiddenId.isEmpty())
							{
								final String[] list = forbiddenId.split(";");
								forbiddenList = new ArrayList<>(list.length);
								for (final String nId : list)
								{
									forbiddenList.add(Integer.parseInt(nId));
								}
							}
							
							final List<Skill> groupList = new ArrayList<>();
							for (Node sk = cat.getFirstChild(); sk != null; sk = sk.getNextSibling())
							{
								if ("skill".equalsIgnoreCase(sk.getNodeName()))
								{
									final int skillId = Integer.parseInt(sk.getAttributes().getNamedItem("id").getNodeValue());
									final int level = Integer.parseInt(sk.getAttributes().getNamedItem("level").getNodeValue());
									
									final Skill data = SkillsParser.getInstance().getInfo(skillId, level);
									if (data != null)
									{
										groupList.add(data);
									}
								}
							}
							addSkillToNpc(groupList, lvlDiff, npcType, npcId, forbiddenList);
							counter++;
						}
					}
				}
			}
			info("Added " + counter + " custom skill templates.");
		}
		catch (
		    NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("customSkills.xml could not be initialized.", e);
		}
		catch (
		    IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	private void addDropToNpc(Map<RewardType, List<RewardGroup>> groupList, String lvlDiff, String npcType, String npcId, List<Integer> forbiddenList)
	{
		if (groupList.isEmpty())
		{
			return;
		}
		
		final String[] splitLvl = lvlDiff.split("-");
		final int minLvl = Integer.parseInt(splitLvl[0]);
		final int maxLvl = Integer.parseInt(splitLvl[1]);
		final boolean isDropForAll = !npcType.isEmpty() && npcType.equals("ALL");
		final boolean isDropbyId = !npcId.isEmpty();
		final boolean isHaveForbidden = forbiddenList != null && !forbiddenList.isEmpty();
		if (isDropForAll)
		{

			for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
			{
				if (template != null && !template.isType("Npc"))
				{
					if (template.getLevel() < minLvl || template.getLevel() > maxLvl)
					{
						continue;
					}
					
					if (isHaveForbidden && forbiddenList.contains(template.getId()))
					{
						continue;
					}
					addDrop(template, groupList);
				}
			}
		}
		else
		{
			if (isDropbyId)
			{
				final String[] npcList = npcId.split(";");
				for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
				{
					if (template != null && isValidId(template, npcList))
					{
						addDrop(template, groupList);
					}
				}
			}
			else
			{
				if (npcType.isEmpty())
				{
					return;
				}
				
				final String[] npcList = npcType.split(";");
				for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
				{
					if (template != null && isValidType(template, npcList))
					{
						if (template.getLevel() < minLvl || template.getLevel() > maxLvl)
						{
							continue;
						}
						
						if (isHaveForbidden && forbiddenList.contains(template.getId()))
						{
							continue;
						}
						addDrop(template, groupList);
					}
				}
			}
		}
	}
	
	private void addDrop(NpcTemplate template, Map<RewardType, List<RewardGroup>> groupList)
	{
		for (final RewardType type : groupList.keySet())
		{
			final RewardList dropList = template.getRewardList(type);
			if (dropList != null)
			{
				for (final RewardGroup group : groupList.get(type))
				{
					dropList.add(group);
				}
			}
			else
			{
				final RewardList newList = new RewardList(type, false);
				for (final RewardGroup group : groupList.get(type))
				{
					newList.add(group);
				}
				template.putRewardList(type, newList);
			}
		}
	}
	
	private boolean isValidType(NpcTemplate template, String[] npcList)
	{
		for (final String type : npcList)
		{
			if (template.isType(type))
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isValidId(NpcTemplate template, String[] npcList)
	{
		for (final String id : npcList)
		{
			if (Integer.parseInt(id) == template.getId())
			{
				return true;
			}
		}
		return false;
	}
	
	private void addSkillToNpc(List<Skill> groupList, String lvlDiff, String npcType, String npcId, List<Integer> forbiddenList)
	{
		if (groupList.isEmpty())
		{
			return;
		}
		
		final String[] splitLvl = lvlDiff.split("-");
		final int minLvl = Integer.parseInt(splitLvl[0]);
		final int maxLvl = Integer.parseInt(splitLvl[1]);
		final boolean isDropForAll = !npcType.isEmpty() && npcType.equals("ALL");
		final boolean isDropbyId = !npcId.isEmpty();
		final boolean isHaveForbidden = forbiddenList != null && !forbiddenList.isEmpty();
		if (isDropForAll)
		{
			
			for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
			{
				if (template != null && !template.isType("Npc"))
				{
					if (template.getLevel() < minLvl || template.getLevel() > maxLvl)
					{
						continue;
					}
					
					if (isHaveForbidden && forbiddenList.contains(template.getId()))
					{
						continue;
					}
					addSkill(template, groupList);
				}
			}
		}
		else
		{
			if (isDropbyId)
			{
				final String[] npcList = npcId.split(";");
				for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
				{
					if (template != null && isValidId(template, npcList))
					{
						addSkill(template, groupList);
					}
				}
			}
			else
			{
				if (npcType.isEmpty())
				{
					return;
				}
				
				final String[] npcList = npcType.split(";");
				for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
				{
					if (template != null && isValidType(template, npcList))
					{
						if (template.getLevel() < minLvl || template.getLevel() > maxLvl)
						{
							continue;
						}
						
						if (isHaveForbidden && forbiddenList.contains(template.getId()))
						{
							continue;
						}
						addSkill(template, groupList);
					}
				}
			}
		}
	}
	
	private void addSkill(NpcTemplate template, List<Skill> groupList)
	{
		for (final Skill skill : groupList)
		{
			if (skill != null && !template.getSkills().containsKey(skill.getId()))
			{
				template.addSkill(skill);
			}
		}
	}
	
	public static final DropManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DropManager _instance = new DropManager();
	}
}