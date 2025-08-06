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
package l2e.scripts.hellbound;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.quest.Quest;

public class Engine extends Quest implements Runnable
{
	private static final String pointsInfoFile = "data/stats/npcs/hellboundTrustPoints.xml";
	
	private static final int UPDATE_INTERVAL = 10000;
	
	private static final int[][] DOOR_LIST =
	{
	        {
	                19250001, 5
			},
			{
			        19250002, 5
			},
			{
			        20250001, 9
			},
			{
			        20250002, 7
			}
	};
	
	private static final int[] MAX_TRUST =
	{
	        0, 300000, 600000, 1000000, 1010000, 1400000, 1490000, 2000000, 2000001, 2500000, 4000000, 0
	};
	
	private static final String ANNOUNCE = "Hellbound now has reached level: %lvl%";
	
	private int _cachedLevel = -1;
	
	private static Map<Integer, PointsInfoHolder> pointsInfo = new HashMap<>();
	
	private class PointsInfoHolder
	{
		protected int pointsAmount;
		protected int minHbLvl;
		protected int maxHbLvl;
		protected int lowestTrustLimit;
		
		protected PointsInfoHolder(int points, int min, int max, int trust)
		{
			pointsAmount = points;
			minHbLvl = min;
			maxHbLvl = max;
			lowestTrustLimit = trust;
		}
	}
	
	private final void onLevelChange(int newLevel)
	{
		try
		{
			HellboundManager.getInstance().setMaxTrust(MAX_TRUST[newLevel]);
			HellboundManager.getInstance().setMinTrust(MAX_TRUST[newLevel - 1]);
		}
		catch (final ArrayIndexOutOfBoundsException e)
		{
			HellboundManager.getInstance().setMaxTrust(0);
			HellboundManager.getInstance().setMinTrust(0);
		}
		
		HellboundManager.getInstance().updateTrust(0, false);
		HellboundManager.getInstance().doSpawn();
		
		for (final int[] doorData : DOOR_LIST)
		{
			try
			{
				final DoorInstance door = DoorParser.getInstance().getDoor(doorData[0]);
				if (door.getOpen())
				{
					if (newLevel < doorData[1])
					{
						door.closeMe();
					}
				}
				else
				{
					if (newLevel >= doorData[1])
					{
						door.openMe();
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("Hellbound doors problem!", e);
			}
		}
		
		if (_cachedLevel > 0)
		{
			Announcements.getInstance().announceToAll(ANNOUNCE.replace("%lvl%", String.valueOf(newLevel)));
			if (Config.DEBUG)
			{
				_log.info("HellboundEngine: New Level: " + newLevel);
			}
		}
		_cachedLevel = newLevel;
	}
	
	private void loadPointsInfoData()
	{
		final File file = new File(Config.DATAPACK_ROOT, pointsInfoFile);
		if (!file.exists())
		{
			_log.warn("Cannot locate points info file: " + pointsInfoFile);
			return;
		}
		
		Document doc = null;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(file);
		}
		catch (final Exception e)
		{
			_log.error("Could not parse " + pointsInfoFile + " file: " + e.getMessage(), e);
			return;
		}
		
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						Node att;
						
						att = attrs.getNamedItem("id");
						if (att == null)
						{
							_log.warn("[Hellbound Trust Points Info] Missing NPC ID, skipping record");
							continue;
						}
						
						final int npcId = Integer.parseInt(att.getNodeValue());
						
						att = attrs.getNamedItem("points");
						if (att == null)
						{
							_log.warn("[Hellbound Trust Points Info] Missing reward point info for NPC ID " + npcId + ", skipping record");
							continue;
						}
						final int points = Integer.parseInt(att.getNodeValue());
						
						att = attrs.getNamedItem("minHellboundLvl");
						if (att == null)
						{
							_log.warn("[Hellbound Trust Points Info] Missing minHellboundLvl info for NPC ID " + npcId + ", skipping record");
							continue;
						}
						final int minHbLvl = Integer.parseInt(att.getNodeValue());
						
						att = attrs.getNamedItem("maxHellboundLvl");
						if (att == null)
						{
							_log.warn("[Hellbound Trust Points Info] Missing maxHellboundLvl info for NPC ID " + npcId + ", skipping record");
							continue;
						}
						final int maxHbLvl = Integer.parseInt(att.getNodeValue());
						
						att = attrs.getNamedItem("lowestTrustLimit");
						int lowestTrustLimit = 0;
						if (att != null)
						{
							lowestTrustLimit = Integer.parseInt(att.getNodeValue());
						}
						
						pointsInfo.put(npcId, new PointsInfoHolder(points, minHbLvl, maxHbLvl, lowestTrustLimit));
					}
				}
			}
		}
		if (Config.DEBUG)
		{
			_log.info("HellboundEngine: Loaded: " + pointsInfo.size() + " trust point reward data");
		}
	}
	
	@Override
	public void run()
	{
		int level = HellboundManager.getInstance().getLevel();
		if ((level > 0) && (level == _cachedLevel))
		{
			if ((HellboundManager.getInstance().getTrust() == HellboundManager.getInstance().getMaxTrust()) && (level != 4))
			{
				level++;
				HellboundManager.getInstance().setLevel(level);
				onLevelChange(level);
			}
		}
		else
		{
			onLevelChange(level);
		}
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (pointsInfo.containsKey(npcId))
		{
			final PointsInfoHolder npcInfo = pointsInfo.get(npcId);
			
			if ((HellboundManager.getInstance().getLevel() >= npcInfo.minHbLvl) && (HellboundManager.getInstance().getLevel() <= npcInfo.maxHbLvl) && ((npcInfo.lowestTrustLimit == 0) || (HellboundManager.getInstance().getTrust() > npcInfo.lowestTrustLimit)))
			{
				HellboundManager.getInstance().updateTrust(npcInfo.pointsAmount, true);
			}
			
			if ((npc.getId() == 18465) && (HellboundManager.getInstance().getLevel() == 4))
			{
				HellboundManager.getInstance().setLevel(5);
			}
		}
		
		return super.onKill(npc, killer, isSummon);
	}
	
	public Engine(int questId, String name, String descr)
	{
		super(questId, name, descr);
		HellboundManager.getInstance().registerEngine(this, UPDATE_INTERVAL);
		loadPointsInfoData();
		
		for (final int npcId : pointsInfo.keySet())
		{
			addKillId(npcId);
		}
		
		if (Config.DEBUG)
		{
			_log.info("HellboundEngine: Mode: levels 0-3");
			_log.info("HellboundEngine: Level: " + HellboundManager.getInstance().getLevel());
			_log.info("HellboundEngine: Trust: " + HellboundManager.getInstance().getTrust());
		}

		if (HellboundManager.getInstance().isLocked())
		{
			if (Config.DEBUG)
			{
				_log.info("HellboundEngine: State: locked");
			}
		}
		else
		{
			if (Config.DEBUG)
			{
				_log.info("HellboundEngine: State: unlocked");
			}
		}
	}
	
	public static void main(String[] args)
	{
		new Engine(-1, Engine.class.getSimpleName(), "hellbound");
	}
}
