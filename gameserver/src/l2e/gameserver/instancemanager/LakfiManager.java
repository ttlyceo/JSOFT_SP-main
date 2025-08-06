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
import java.util.Collections;
import java.util.Comparator;
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
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.LakfiNpcTemplate;
import l2e.gameserver.model.actor.templates.npc.LakfiRewardTemplate;
import l2e.gameserver.model.spawn.Spawner;

/**
 * Created by LordWinter
 */
public class LakfiManager extends LoggerObject
{
	protected Map<Integer, List<LakfiNpcTemplate>> _lakfiList = new HashMap<>();
	private final List<Npc> _npcs = new ArrayList<>();
	private ScheduledFuture<?> _despawnTask = null;
	
	public LakfiManager()
	{
		if (Config.LAKFI_ENABLED)
		{
			_lakfiList.clear();
			_npcs.clear();
			loadRewards();
			initSpawns();
			info("Activated " + _npcs.size() + " lakfi-lakfi npcs.");
		}
	}
	
	private void loadRewards()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/lakfiRewards.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);

			int counter = 0;
			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("lakfi".equalsIgnoreCase(d1.getNodeName()))
						{
							final int level = Integer.parseInt(d1.getAttributes().getNamedItem("level").getNodeValue());
							final List<LakfiNpcTemplate> npcList = new ArrayList<>();
							for (Node s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("npc".equalsIgnoreCase(s1.getNodeName()))
								{
									final List<LakfiRewardTemplate> rewards = new ArrayList<>();
									final int npcId = Integer.parseInt(s1.getAttributes().getNamedItem("id").getNodeValue());
									for (Node z1 = s1.getFirstChild(); z1 != null; z1 = z1.getNextSibling())
									{
										if ("item".equalsIgnoreCase(z1.getNodeName()))
										{
											final int itemId = Integer.parseInt(z1.getAttributes().getNamedItem("id").getNodeValue());
											final long min = Long.parseLong(z1.getAttributes().getNamedItem("min").getNodeValue());
											final long max = z1.getAttributes().getNamedItem("max") != null ? Long.parseLong(z1.getAttributes().getNamedItem("max").getNodeValue()) : min;
											final double chance = z1.getAttributes().getNamedItem("chance") != null ? Double.parseDouble(z1.getAttributes().getNamedItem("chance").getNodeValue()) : 100;
											rewards.add(new LakfiRewardTemplate(itemId, min, max, chance));
											counter++;
										}
									}
									npcList.add(new LakfiNpcTemplate(npcId, rewards));
								}
							}
							_lakfiList.put(level, npcList);
						}
					}
				}
			}
			info("Loaded " + counter + " lakfi reward templates.");
		}
		catch (NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("lakfiRewards.xml could not be initialized.", e);
		}
		catch (IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	public void getLakfiRewards(int lakfiLvl, Attackable actor, Player killer, double chance)
	{
		final List<LakfiNpcTemplate> npcs = _lakfiList.get(lakfiLvl);
		if (npcs != null)
		{
			for (final LakfiNpcTemplate template : npcs)
			{
				if (actor.getId() == template.getId())
				{
					final List<LakfiRewardTemplate> rewards = template.getRewards();
					if (rewards != null)
					{
						final List<LakfiRewardTemplate> curRewards = new ArrayList<>();
						for (final LakfiRewardTemplate tp : rewards)
						{
							if (tp != null && tp.getChance() >= chance)
							{
								curRewards.add(tp);
							}
						}
						
						if (!curRewards.isEmpty())
						{
							final Comparator<LakfiRewardTemplate> statsComparator = new LakfiRewardInfo();
							Collections.sort(curRewards, statsComparator);
							final LakfiRewardTemplate tpl = curRewards.get(curRewards.size() - 1);
							if (tpl != null)
							{
								final long amount = tpl.getMinCount() != tpl.getMaxCount() ? Rnd.get(tpl.getMinCount(), tpl.getMaxCount()) : tpl.getMinCount();
								actor.dropSingleItem(killer, tpl.getId(), (int) amount);
							}
						}
					}
				}
			}
		}
	}
	
	private static class LakfiRewardInfo implements Comparator<LakfiRewardTemplate>
	{
		@Override
		public int compare(LakfiRewardTemplate o1, LakfiRewardTemplate o2)
		{
			return Double.compare(o2.getChance(), o1.getChance());
		}
	}
	
	private void initSpawns()
	{
		_npcs.clear();
		
		final List<Spawner> spawns = new ArrayList<>();
		for (int i = 1; i < 10; i++)
		{
			final Spawner lakfiSpawn = getRndSpawn(SpawnParser.getInstance().getSpawn("lakkfi_" + i + ""));
			if (lakfiSpawn != null)
			{
				spawns.add(lakfiSpawn);
			}
		}
		
		if (spawns != null && !spawns.isEmpty())
		{
			initSpawnGroups(spawns);
		}
		_despawnTask = ThreadPoolManager.getInstance().schedule(new DespawnTask(), Config.TIME_CHANGE_SPAWN * 60000);
	}
	
	private class DespawnTask implements Runnable
	{
		@Override
		public void run()
		{
			for (final Npc npc : _npcs)
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
			}
			initSpawns();
		}
	}
	
	public void stopTimer()
	{
		if (_despawnTask != null)
		{
			_despawnTask.cancel(false);
			_despawnTask = null;
		}
		_npcs.clear();
	}
	
	private void initSpawnGroups(List<Spawner> spawns)
	{
		for (final Spawner spawn : spawns)
		{
			final Npc npc = spawn.doSpawn();
			if (npc != null)
			{
				_npcs.add(npc);
			}
		}
	}
	
	private Spawner getRndSpawn(List<Spawner> list)
	{
		if(list == null || list.isEmpty())
		{
			return null;
		}
		final int index = Rnd.get(0, list.size() - 1);
		return list.get(index);
	}
	
	public static final LakfiManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final LakfiManager _instance = new LakfiManager();
	}
}