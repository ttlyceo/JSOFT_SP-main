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
package l2e.gameserver.instancemanager.games.krateiscube;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.games.krateiscube.model.Arena;
import l2e.gameserver.instancemanager.games.krateiscube.model.KrateiCubePlayer;
import l2e.gameserver.instancemanager.games.krateiscube.model.KrateisReward;
import l2e.gameserver.instancemanager.games.krateiscube.model.MsgType;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.CreatureSay;

/**
 * Created by LordWinter
 */
public final class KrateisCubeManager extends LoggerObject
{
	private final Map<Integer, Arena> _arenas = new HashMap<>(3);
	private Npc _manager = null;
	private long _nextMatchTime;
	private boolean _registerActive = false;
	private boolean _isHalfAnHour = false;
	private EventState _state = EventState.REGISTRATION;
	
	private ScheduledFuture<?> _periodTask = null;
	private ScheduledFuture<?> _eventTask = null;
	private ScheduledFuture<?> _msgTask = null;
	
	private enum EventState
	{
		REGISTRATION, PREPARING, STARTED
	}
	
	public KrateisCubeManager()
	{
		load();
		info("Loaded " + _arenas.size() + " arena templates.");
		
		boolean spawnManager = false;
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn != null && spawn.getLastSpawn() != null && spawn.getLastSpawn().getId() == 32503)
			{
				_manager = spawn.getLastSpawn();
				spawnManager = true;
			}
		}
		
		if (spawnManager)
		{
			recalcEventTime();
		}
		else
		{
			info("Event can't be started because npc not found!");
		}
	}
	
	private void load()
	{
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/spawnZones/krateisCube.xml");
			if (!file.exists())
			{
				info("krateisCube.xml file is missing.");
				return;
			}
			final Document doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap map;
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("arena".equalsIgnoreCase(d.getNodeName()))
						{
							map = d.getAttributes();
							final int id = Integer.parseInt(map.getNamedItem("id").getNodeValue());
							final int manager = Integer.parseInt(map.getNamedItem("manager").getNodeValue());
							final int minLevel = Integer.parseInt(map.getNamedItem("minLevel").getNodeValue());
							final int maxLevel = Integer.parseInt(map.getNamedItem("maxLevel").getNodeValue());
							final int minPlayers = Integer.parseInt(map.getNamedItem("minPlayers").getNodeValue());
							final int maxPlayers = Integer.parseInt(map.getNamedItem("maxPlayers").getNodeValue());
							
							final Arena arena = new Arena(id, manager, minLevel, maxLevel, minPlayers, maxPlayers);
							final StatsSet params = new StatsSet();
							
							for (Node kc = d.getFirstChild(); kc != null; kc = kc.getNextSibling())
							{
								if ("doorA".equalsIgnoreCase(kc.getNodeName()))
								{
									map = kc.getAttributes();
									final String doorList = map.getNamedItem("list") != null ? map.getNamedItem("list").getNodeValue() : "";
									if (!doorList.isEmpty())
									{
										final String[] doorSplint = doorList.split(",");
										for (final String doorId : doorSplint)
										{
											final DoorInstance door = DoorParser.getInstance().getDoor(Integer.parseInt(doorId));
											if (door != null)
											{
												arena.addDoorA(door);
											}
										}
									}
								}
								else if ("doorB".equalsIgnoreCase(kc.getNodeName()))
								{
									map = kc.getAttributes();
									final String doorList = map.getNamedItem("list") != null ? map.getNamedItem("list").getNodeValue() : "";
									if (!doorList.isEmpty())
									{
										final String[] doorSplint = doorList.split(",");
										for (final String doorId : doorSplint)
										{
											final DoorInstance door = DoorParser.getInstance().getDoor(Integer.parseInt(doorId));
											if (door != null)
											{
												arena.addDoorB(door);
											}
										}
									}
								}
								else if ("waitLocations".equalsIgnoreCase(kc.getNodeName()))
								{
									for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("point".equalsIgnoreCase(c.getNodeName()))
										{
											map = c.getAttributes();
											final int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
											final int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
											final int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
											arena.addWaitLoc(new Location(x, y, z));
										}
									}
								}
								else if ("battleLocations".equalsIgnoreCase(kc.getNodeName()))
								{
									for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("point".equalsIgnoreCase(c.getNodeName()))
										{
											map = c.getAttributes();
											final int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
											final int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
											final int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
											arena.addBattleLoc(new Location(x, y, z));
										}
									}
								}
								else if ("watcherLocations".equalsIgnoreCase(kc.getNodeName()))
								{
									for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("point".equalsIgnoreCase(c.getNodeName()))
										{
											map = c.getAttributes();
											final int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
											final int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
											final int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
											arena.addWatcherLoc(new Location(x, y, z));
										}
									}
								}
								else if ("spawns".equalsIgnoreCase(kc.getNodeName()))
								{
									for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("spawn".equalsIgnoreCase(c.getNodeName()))
										{
											arena.addSpawnGroup(c.getAttributes().getNamedItem("group").getNodeValue());
										}
									}
								}
								else if ("buffs".equalsIgnoreCase(kc.getNodeName()))
								{
									for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("skill".equalsIgnoreCase(c.getNodeName()))
										{
											final int skillId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
											final int level = Integer.parseInt(c.getAttributes().getNamedItem("level").getNodeValue());
											final Skill skill = SkillsParser.getInstance().getInfo(skillId, level);
											if (skill != null)
											{
												arena.addBuff(skill);
											}
										}
									}
								}
								else if ("rewards".equalsIgnoreCase(kc.getNodeName()))
								{
									for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("item".equalsIgnoreCase(c.getNodeName()))
										{
											final int itemId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
											final long amount = Long.parseLong(c.getAttributes().getNamedItem("amount").getNodeValue());
											final boolean useModifier = c.getAttributes().getNamedItem("useModifers") != null ? Boolean.parseBoolean(c.getAttributes().getNamedItem("useModifers").getNodeValue()) : false;
											arena.setReward(new KrateisReward(itemId, amount, useModifier));
										}
									}
								}
								else if ("add_parameters".equalsIgnoreCase(kc.getNodeName()))
								{
									for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("set".equalsIgnoreCase(c.getNodeName()))
										{
											params.set(c.getAttributes().getNamedItem("name").getNodeValue(), c.getAttributes().getNamedItem("value").getNodeValue());
										}
									}
									arena.addParam(params);
								}
							}
							_arenas.put(arena.getId(), arena);
						}
					}
				}
			}
		}
		catch (
		    NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("krateisCube.xml could not be initialized.", e);
		}
		catch (
		    IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	public void prepareEvent()
	{
		if (_registerActive)
		{
			_registerActive = false;
		}
		
		if (_periodTask != null)
		{
			_periodTask.cancel(false);
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
		}
		
		if (_msgTask != null)
		{
			_msgTask.cancel(false);
		}
		
		boolean isActive = false;
		for (final Arena arena : _arenas.values())
		{
			if (arena != null && arena.getPlayers().size() >= arena.getMinPlayers())
			{
				int count = 0;
				final List<Player> invalidPlayers = new ArrayList<>();
				for (final KrateiCubePlayer pl : arena.getPlayers().values())
				{
					if (pl != null && pl.isRegister())
					{
						if (pl.getPlayer().isInsideRadius(_manager, 1500, false, true))
						{
							pl.setIsInside(true);
							pl.setIsRegister(false);
							count++;
						}
						else
						{
							invalidPlayers.add(pl.getPlayer());
						}
					}
				}
				
				if (!invalidPlayers.isEmpty())
				{
					for (final Player pl : invalidPlayers)
					{
						arena.removePlayer(pl);
					}
				}
				invalidPlayers.clear();
				
				if (count > 1)
				{
					arena.setIsBattleNow(true);
					arena.teleportToBattle(null);
					arena.waitTimeInfo();
					isActive = true;
				}
			}
		}
		
		if (isActive)
		{
			_eventTask = ThreadPoolManager.getInstance().schedule(() -> startEvent(), 11000);
			getManagerMessage(MsgType.STARTED);
		}
		else
		{
			recalcEventTime();
		}
	}
	
	public void startEvent()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
		}
		
		for (final Arena arena : _arenas.values())
		{
			if (arena != null && arena.isBattleNow())
			{
				arena.startEvent();
				_state = EventState.STARTED;
			}
		}
		_periodTask = ThreadPoolManager.getInstance().schedule(() -> recalcEventTime(), 180000);
	}
	
	public void abortEvent()
	{
		for (final Arena arena : _arenas.values())
		{
			if (arena != null && arena.isBattleNow())
			{
				arena.endEvent();
			}
		}
		
		if (!_registerActive)
		{
			recalcEventTime();
		}
	}
	
	public boolean isRegisterTime()
	{
		return _registerActive;
	}
	
	public boolean isActive()
	{
		return _state == EventState.STARTED;
	}
	
	public boolean isPreparing()
	{
		return _state == EventState.PREPARING;
	}
	
	public void setIsActivate(boolean val)
	{
		if (!val)
		{
			_state = EventState.REGISTRATION;
		}
	}
	
	public void recalcEventTime()
	{
		if (!_registerActive)
		{
			_registerActive = true;
		}
		
		if (_periodTask != null)
		{
			_periodTask.cancel(false);
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
		}
		
		final Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.MINUTE) >= 57)
		{
			cal.add(Calendar.HOUR, 1);
			cal.set(Calendar.MINUTE, 27);
		}
		else if ((cal.get(Calendar.MINUTE) >= 0) && (cal.get(Calendar.MINUTE) <= 26))
		{
			cal.set(Calendar.MINUTE, 27);
			cal.set(Calendar.SECOND, 0);
			_isHalfAnHour = true;
		}
		else
		{
			cal.set(Calendar.MINUTE, 57);
			cal.set(Calendar.SECOND, 0);
			_isHalfAnHour = false;
		}
		_nextMatchTime = cal.getTimeInMillis();
		final long lastTime = _nextMatchTime - System.currentTimeMillis();
		_eventTask = ThreadPoolManager.getInstance().schedule(() -> closeRegistration(), lastTime);
		getManagerMessage(MsgType.INITIALIZED);
		final int time = (int) ((lastTime / 1000) / 60);
		if (time >= 5)
		{
			_msgTask = ThreadPoolManager.getInstance().schedule(() -> getManagerMessage(MsgType.REGISTRATION_5), lastTime - 300000);
		}
		else if (time >= 3 && time < 5)
		{
			_msgTask = ThreadPoolManager.getInstance().schedule(() -> getManagerMessage(MsgType.REGISTRATION_3), lastTime - 180000);
		}
		else if (time >= 1 && time < 3)
		{
			_msgTask = ThreadPoolManager.getInstance().schedule(() -> getManagerMessage(MsgType.REGISTRATION_1), lastTime - 60000);
		}
		
		if (_state != EventState.STARTED)
		{
			_state = EventState.REGISTRATION;
		}
		info("Next match " + new Date(cal.getTimeInMillis()));
	}
	
	public void closeRegistration()
	{
		if (_periodTask != null)
		{
			_periodTask.cancel(false);
		}
		_registerActive = false;
		getManagerMessage(MsgType.PREPATING);
		_state = EventState.PREPARING;
		_periodTask = ThreadPoolManager.getInstance().schedule(() -> prepareEvent(), 180000);
	}
	
	private void getManagerMessage(MsgType state)
	{
		final long lastTime = _nextMatchTime - System.currentTimeMillis();
		switch (state)
		{
			case INITIALIZED :
				final CreatureSay msg = new CreatureSay(_manager.getObjectId(), 0, _manager.getName(null), NpcStringId.REGISTRATION_FOR_THE_NEXT_MATCH_WILL_END_AT_S1_MINUTES_AFTER_HOUR);
				msg.addStringParameter(String.valueOf(!_isHalfAnHour ? 57 : 27));
				_manager.broadcastPacketToOthers(1500, msg);
				break;
			case REGISTRATION_5 :
				_manager.broadcastPacketToOthers(1500, new CreatureSay(_manager.getObjectId(), 0, _manager.getName(null), NpcStringId.THERE_ARE_5_MINUTES_REMAINING_TO_REGISTER_FOR_KRATEIS_CUBE_MATCH));
				_msgTask = ThreadPoolManager.getInstance().schedule(() -> getManagerMessage(MsgType.REGISTRATION_3), lastTime - 180000);
				break;
			case REGISTRATION_3 :
				_manager.broadcastPacketToOthers(1500, new CreatureSay(_manager.getObjectId(), 0, _manager.getName(null), NpcStringId.THERE_ARE_3_MINUTES_REMAINING_TO_REGISTER_FOR_KRATEIS_CUBE_MATCH));
				_msgTask = ThreadPoolManager.getInstance().schedule(() -> getManagerMessage(MsgType.REGISTRATION_1), lastTime - 60000);
				break;
			case REGISTRATION_1 :
				_manager.broadcastPacketToOthers(1500, new CreatureSay(_manager.getObjectId(), 0, _manager.getName(null), NpcStringId.THERE_ARE_1_MINUTES_REMAINING_TO_REGISTER_FOR_KRATEIS_CUBE_MATCH));
				break;
			case PREPATING :
				final CreatureSay msg3 = new CreatureSay(_manager.getObjectId(), 0, _manager.getName(null), NpcStringId.THE_MATCH_WILL_BEGIN_IN_S1_MINUTES);
				msg3.addStringParameter(String.valueOf(3));
				_manager.broadcastPacketToOthers(1500, msg3);
				break;
			case STARTED :
				_manager.broadcastPacketToOthers(1500, new CreatureSay(_manager.getObjectId(), 0, _manager.getName(null), NpcStringId.THE_MATCH_WILL_BEGIN_SHORTLY));
				break;
		}
	}
	
	public long getNextMatchTime()
	{
		return _nextMatchTime;
	}
	
	public Arena getArenaId(int id)
	{
		return _arenas.get(id);
	}
	
	public Map<Integer, Arena> getArenas()
	{
		return _arenas;
	}
	
	public static KrateisCubeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final KrateisCubeManager INSTANCE = new KrateisCubeManager();
	}
}
