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
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

/**
 * Created by LordWinter 02.11.2021
 */
public class NpcStatManager extends LoggerObject
{
	public NpcStatManager()
	{
		loadStatSettings();
	}
	
	public void reload()
	{
		loadStatSettings();
	}

	private void loadStatSettings()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/customStats.xml");
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
						if ("stats".equalsIgnoreCase(cat.getNodeName()))
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
							
							final Map<CustomStatType, Double> statsList = new HashMap<>();
							for (Node reward = cat.getFirstChild(); reward != null; reward = reward.getNextSibling())
							{
								if ("stat".equalsIgnoreCase(reward.getNodeName()))
								{
									final CustomStatType tp = CustomStatType.valueOf(reward.getAttributes().getNamedItem("type").getNodeValue());
									final double value = Double.parseDouble(reward.getAttributes().getNamedItem("value").getNodeValue());
									statsList.put(tp, value);
								}
							}
							addStatsToNpc(statsList, lvlDiff, npcType, npcId, forbiddenList);
							counter++;
						}
					}
				}
			}
			info("Loaded " + counter + " custom stat templates.");
		}
		catch (
		    NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("customStats.xml could not be initialized.", e);
		}
		catch (
		    IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	private void addStatsToNpc(Map<CustomStatType, Double> statsList, String lvlDiff, String npcType, String npcId, List<Integer> forbiddenList)
	{
		if (statsList.isEmpty())
		{
			return;
		}
		
		for (final CustomStatType type : CustomStatType.values())
		{
			if (statsList.containsKey(type))
			{
				continue;
			}
			statsList.put(type, 1.0);
		}
		
		final String[] splitLvl = lvlDiff.split("-");
		final int minLvl = Integer.parseInt(splitLvl[0]);
		final int maxLvl = Integer.parseInt(splitLvl[1]);
		final boolean isStatForAll = !npcType.isEmpty() && npcType.equals("ALL");
		final boolean isStatbyId = !npcId.isEmpty();
		final boolean isHaveForbidden = forbiddenList != null && !forbiddenList.isEmpty();
		if (isStatForAll)
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
					template.setStatList(statsList);
				}
			}
		}
		else
		{
			if (isStatbyId)
			{
				final String[] npcList = npcId.split(";");
				for (final NpcTemplate template : NpcsParser.getInstance().getAllNpcs())
				{
					if (template != null && isValidId(template, npcList))
					{
						template.setStatList(statsList);
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
						template.setStatList(statsList);
					}
				}
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
	
	public enum CustomStatType
	{
		MAXHP, MAXMP, MATK, MSPEED, MDEF, PATK, PSPEED, PDEF
	}
	
	public static final NpcStatManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcStatManager _instance = new NpcStatManager();
	}
}