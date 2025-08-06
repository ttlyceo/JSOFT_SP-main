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
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.player.vip.VipNpcTemplate;
import l2e.gameserver.model.actor.templates.player.vip.VipTemplate;
import l2e.gameserver.model.holders.ItemHolder;

/**
 * Created by LordWinter extends LoggerObject
 */
public class VipManager extends LoggerObject
{
	private final List<VipTemplate> _templates = new ArrayList<>();
	private final Map<Integer, VipNpcTemplate> _npcList = new HashMap<>();
	private ScheduledFuture<?> _refreshTask = null;
	
	private int _maxLevel = 0;
	private long _maxPoints = 0;
	
	public VipManager()
	{
		_templates.clear();
		_npcList.clear();
		templaterParser();
		searchMaxLevel();
		checkTimeTask();
	}
	
	private void templaterParser()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/services/vipTemplates.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);
			
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("vip".equalsIgnoreCase(d1.getNodeName()))
						{
							final int id = Integer.parseInt(d1.getAttributes().getNamedItem("level").getNodeValue());
							final long points = Long.parseLong(d1.getAttributes().getNamedItem("needPoints").getNodeValue());
							
							double expRate = 1.0, spRate = 1.0, adenaRate = 1.0, dropRate = 1.0, dropRaidRate = 1.0, spoilRate = 1.0, epRate = 1.0;
							int enchantRate = 0;
							final List<ItemHolder> items = new ArrayList<>();
							final List<ItemHolder> requestItems = new ArrayList<>();
							final List<ItemHolder> rewardItems = new ArrayList<>();
							for (Node s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("set".equalsIgnoreCase(s1.getNodeName()))
								{
									if (s1.getAttributes().getNamedItem("expRate") != null)
									{
										expRate = Double.parseDouble(s1.getAttributes().getNamedItem("expRate").getNodeValue());
									}
									if (s1.getAttributes().getNamedItem("spRate") != null)
									{
										spRate = Double.parseDouble(s1.getAttributes().getNamedItem("spRate").getNodeValue());
									}
									if (s1.getAttributes().getNamedItem("adenaRate") != null)
									{
										adenaRate = Double.parseDouble(s1.getAttributes().getNamedItem("adenaRate").getNodeValue());
									}
									if (s1.getAttributes().getNamedItem("dropRate") != null)
									{
										dropRate = Double.parseDouble(s1.getAttributes().getNamedItem("dropRate").getNodeValue());
									}
									if (s1.getAttributes().getNamedItem("dropRaidRate") != null)
									{
										dropRaidRate = Double.parseDouble(s1.getAttributes().getNamedItem("dropRaidRate").getNodeValue());
									}
									if (s1.getAttributes().getNamedItem("spoilRate") != null)
									{
										spoilRate = Double.parseDouble(s1.getAttributes().getNamedItem("spoilRate").getNodeValue());
									}
									if (s1.getAttributes().getNamedItem("epRate") != null)
									{
										epRate = Double.parseDouble(s1.getAttributes().getNamedItem("epRate").getNodeValue());
									}
									if (s1.getAttributes().getNamedItem("enchantRate") != null)
									{
										enchantRate = Integer.parseInt(s1.getAttributes().getNamedItem("enchantRate").getNodeValue());
									}
								}
								else if ("daily".equalsIgnoreCase(s1.getNodeName()))
								{
									if ((s1.getAttributes().getNamedItem("itemId") != null) && (s1.getAttributes().getNamedItem("amount") != null))
									{
										final int itemId = Integer.parseInt(s1.getAttributes().getNamedItem("itemId").getNodeValue());
										final long itemAmount = Long.parseLong(s1.getAttributes().getNamedItem("amount").getNodeValue());
										items.add(new ItemHolder(itemId, itemAmount));
									}
								}
								else if ("requestItems".equalsIgnoreCase(s1.getNodeName()))
								{
									if ((s1.getAttributes().getNamedItem("itemId") != null) && (s1.getAttributes().getNamedItem("amount") != null))
									{
										final int itemId = Integer.parseInt(s1.getAttributes().getNamedItem("itemId").getNodeValue());
										final long itemAmount = Long.parseLong(s1.getAttributes().getNamedItem("amount").getNodeValue());
										requestItems.add(new ItemHolder(itemId, itemAmount));
									}
								}
								else if ("rewardItems".equalsIgnoreCase(s1.getNodeName()))
								{
									if ((s1.getAttributes().getNamedItem("itemId") != null) && (s1.getAttributes().getNamedItem("amount") != null))
									{
										final int itemId = Integer.parseInt(s1.getAttributes().getNamedItem("itemId").getNodeValue());
										final long itemAmount = Long.parseLong(s1.getAttributes().getNamedItem("amount").getNodeValue());
										rewardItems.add(new ItemHolder(itemId, itemAmount));
									}
								}
							}
							_templates.add(new VipTemplate(id, points, expRate, spRate, adenaRate, dropRate, dropRaidRate, spoilRate, epRate, enchantRate, items, rewardItems, requestItems));
						}
						else if ("npc".equalsIgnoreCase(d1.getNodeName()))
						{
							final int npcId = Integer.parseInt(d1.getAttributes().getNamedItem("id").getNodeValue());
							final long points = Long.parseLong(d1.getAttributes().getNamedItem("points").getNodeValue());
							if (getNpcTemplate(npcId) == null)
							{
								_npcList.put(npcId, new VipNpcTemplate(npcId, points));
							}
						}
						else if ("points".equalsIgnoreCase(d1.getNodeName()))
						{
							final int points = d1.getAttributes().getNamedItem("value") != null ? Integer.parseInt(d1.getAttributes().getNamedItem("value").getNodeValue()) : 0;
							final String lvlDiff = d1.getAttributes().getNamedItem("lvlDiff") != null ? d1.getAttributes().getNamedItem("lvlDiff").getNodeValue() : "1-85";
							final String npcType = d1.getAttributes().getNamedItem("npcType") != null ? d1.getAttributes().getNamedItem("npcType").getNodeValue() : "";
							final String npcId = d1.getAttributes().getNamedItem("npcId") != null ? d1.getAttributes().getNamedItem("npcId").getNodeValue() : "";
							final String forbiddenId = d1.getAttributes().getNamedItem("forbiddenList") != null ? d1.getAttributes().getNamedItem("forbiddenList").getNodeValue() : "";
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
							addPointsToNpc(points, lvlDiff, npcType, npcId, forbiddenList);
						}
					}
				}
			}
			info("Loaded " + _templates.size() + " vip templates.");
			info("Loaded " + _npcList.size() + " npc templates.");
		}
		catch (NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("contributions.xml could not be initialized.", e);
		}
		catch (IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	public VipNpcTemplate getNpcTemplate(int npcId)
	{
		return _npcList.get(npcId);
	}
	
	private void addPointsToNpc(int points, String lvlDiff, String npcType, String npcId, List<Integer> forbiddenList)
	{
		if (points <= 0)
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
					
					if (getNpcTemplate(template.getId()) == null)
					{
						_npcList.put(template.getId(), new VipNpcTemplate(template.getId(), points));
					}
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
						if (getNpcTemplate(template.getId()) == null)
						{
							_npcList.put(template.getId(), new VipNpcTemplate(template.getId(), points));
						}
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
						
						if (getNpcTemplate(template.getId()) == null)
						{
							_npcList.put(template.getId(), new VipNpcTemplate(template.getId(), points));
						}
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
	
	public void checkTimeTask()
	{
		final long lastUpdate = ServerVariables.getLong("RefreshVipTime", 0);
		if (System.currentTimeMillis() > lastUpdate)
		{
			cleanVip();
		}
		else
		{
			_refreshTask = ThreadPoolManager.getInstance().schedule(() -> cleanVip(), (lastUpdate - System.currentTimeMillis()));
		}
	}
	
	public void cleanVip()
	{
		for (final var player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				player.setVipLevel(0);
				player.setVipPoints(0);
			}
		}
		
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); var statement = con.prepareStatement("DELETE FROM character_variables WHERE name='vipLevel'"))
		{
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update vip level.", e);
		}
		
		try (
		    var con = DatabaseFactory.getInstance().getConnection(); var statement = con.prepareStatement("DELETE FROM character_variables WHERE name='vipPoints'"))
		{
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Failed update vip points.", e);
		}
		
		SchedulingPattern cronTime;
		try
		{
			cronTime = new SchedulingPattern("30 6 1 * *");
		}
		catch (final InvalidPatternException e)
		{
			warn("Wrong calc next vip refresh pattern!");
			return;
		}
		
		final long respawnTime = cronTime.next(System.currentTimeMillis());
		
		ServerVariables.set("RefreshVipTime", respawnTime);
		if (_refreshTask != null)
		{
			_refreshTask.cancel(false);
			_refreshTask = null;
		}
		_refreshTask = ThreadPoolManager.getInstance().schedule(() -> cleanVip(), (respawnTime - System.currentTimeMillis()));
		info("Reshresh completed.");
		info("Next refresh throught: " + Util.formatTime((int) (respawnTime - System.currentTimeMillis()) / 1000));
	}
	
	public VipTemplate getVipLevel(final int level)
	{
		for (final var template : _templates)
		{
			if ((template != null) && (template.getId() == level))
			{
				return template;
			}
		}
		return null;
	}
	
	public List<VipTemplate> getVipTemplates()
	{
		return _templates;
	}
	
	public void searchMaxLevel()
	{
		for (final var template : _templates)
		{
			if ((template != null) && (template.getId() > _maxLevel))
			{
				_maxLevel = template.getId();
				_maxPoints = template.getPoints();
			}
		}
	}
	
	public long getMaxPoints()
	{
		return _maxPoints;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	public static final VipManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final VipManager _instance = new VipManager();
	}
}