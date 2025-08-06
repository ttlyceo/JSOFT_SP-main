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
package l2e.gameserver.model.entity.events.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.spawn.Spawner;

/**
 * Created by LordWinter
 */
public class FightEventNpcManager extends LoggerObject
{
	private final List<Npc> _npclist = new ArrayList<>();
	private final List<Location> _locations = new ArrayList<>();
	private final List<Npc> _globalList = new ArrayList<>();
	private final List<Location> _globalLocations = new ArrayList<>();
	private boolean _isSpawned = false;
	private boolean _isGlobalSpawned = false;

	public FightEventNpcManager()
	{
		_npclist.clear();
		_globalList.clear();
		_locations.clear();
		_globalLocations.clear();
		parseLocations();
		info("Loaded " + _locations.size() + " locations for event manager.");
	}
	
	public void reload()
	{
		_npclist.clear();
		_locations.clear();
		parseLocations();
		info("Loaded " + _locations.size() + " locations for event manager.");
	}

	private void parseLocations()
	{
		final File spawnFile = new File("data/stats/events/manager/spawnlist.xml");
		try
		{
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.parse(spawnFile);
			if (!doc.getDocumentElement().getNodeName().equalsIgnoreCase("list"))
			{
				throw new NullPointerException("WARNING!!! stats/events/manager/spawnlist.xml bad spawn file!");
			}

			final Node first = doc.getDocumentElement().getFirstChild();
			for (Node n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeName().equalsIgnoreCase("manager"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("loc"))
						{
							try
							{
								final int x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
								final int y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
								final int z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
								final int h = d.getAttributes().getNamedItem("heading").getNodeValue() != null ? Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue()) : 0;
								_locations.add(new Location(x, y, z, h));
							}
							catch (final NumberFormatException nfe)
							{
								warn("Wrong number format in stats/events/manager/spawnlist.xml");
							}
						}
					}
				}
				else if (n.getNodeName().equalsIgnoreCase("globalManager"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("loc"))
						{
							try
							{
								final int x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
								final int y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
								final int z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
								final int h = d.getAttributes().getNamedItem("heading").getNodeValue() != null ? Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue()) : 0;
								_globalLocations.add(new Location(x, y, z, h));
							}
							catch (final NumberFormatException nfe)
							{
								warn("Wrong number format in stats/events/manager/spawnlist.xml");
							}
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("error reading " + spawnFile.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
	}
	
	public void trySpawnRegNpc()
	{
		if (!isSpawned())
		{
			for (final Location loc : _locations)
			{
				if (loc != null)
				{
					try
					{
						final NpcTemplate template = NpcsParser.getInstance().getTemplate(53015);
						if (template != null)
						{
							final Spawner spawn = new Spawner(template);
							spawn.setX(loc.getX());
							spawn.setY(loc.getY());
							spawn.setZ(loc.getZ());
							spawn.setHeading(loc.getHeading());
							spawn.setAmount(1);
							spawn.setRespawnDelay(0);
							spawn.stopRespawn();
							spawn.init();
							_npclist.add(spawn.getLastSpawn());
						}
					}
					catch (final Exception e1)
					{}
				}
			}
			_isSpawned = true;
		}
	}

	public void tryUnspawnRegNpc()
	{
		if (isSpawned())
		{
			if (!isAcviteRegister())
			{
				if (!_npclist.isEmpty())
				{
					for (final Npc _npc : _npclist)
					{
						if (_npc != null)
						{
							_npc.deleteMe();
						}
					}
					_isSpawned = false;
				}
			}
		}
	}
	
	public void tryGlobalSpawnRegNpc()
	{
		if (!isGlobalSpawned())
		{
			for (final Location loc : _globalLocations)
			{
				if (loc != null)
				{
					try
					{
						final NpcTemplate template = NpcsParser.getInstance().getTemplate(53016);
						if (template != null)
						{
							final Spawner spawn = new Spawner(template);
							spawn.setX(loc.getX());
							spawn.setY(loc.getY());
							spawn.setZ(loc.getZ());
							spawn.setHeading(loc.getHeading());
							spawn.setAmount(1);
							spawn.setRespawnDelay(0);
							spawn.stopRespawn();
							spawn.init();
							_globalList.add(spawn.getLastSpawn());
						}
					}
					catch (final Exception e1)
					{}
				}
			}
			_isGlobalSpawned = true;
		}
	}
	
	public void tryUnspawnGlobalRegNpc()
	{
		if (isGlobalSpawned())
		{
			if (!isAcviteGlobalRegister())
			{
				if (!_globalList.isEmpty())
				{
					for (final Npc _npc : _globalList)
					{
						if (_npc != null)
						{
							_npc.deleteMe();
						}
					}
					_isGlobalSpawned = false;
				}
			}
		}
	}
	
	private boolean isSpawned()
	{
		return _isSpawned;
	}
	
	private boolean isGlobalSpawned()
	{
		return _isGlobalSpawned;
	}
	
	private boolean isAcviteRegister()
	{
		boolean activeReg = false;
		for (final AbstractFightEvent event : FightEventManager.getInstance().getActiveEvents().values())
		{
			if (event != null && FightEventManager.getInstance().isRegistrationOpened(event))
			{
				activeReg = true;
				break;
			}
		}
		return activeReg;
	}
	
	private boolean isAcviteGlobalRegister()
	{
		boolean activeReg = false;
		for (final AbstractFightEvent event : FightEventManager.getInstance().getGlobalActiveEvents().values())
		{
			if (event != null && event.isGlobal() && FightEventManager.getInstance().isRegistrationOpened(event))
			{
				activeReg = true;
				break;
			}
		}
		return activeReg;
	}

	public static final FightEventNpcManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FightEventNpcManager _instance = new FightEventNpcManager();
	}
}