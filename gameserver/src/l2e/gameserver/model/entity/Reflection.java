package l2e.gameserver.model.entity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilderFactory;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.HashIntObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.ColosseumFence;
import l2e.gameserver.model.actor.ColosseumFence.FenceState;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.templates.door.DoorTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.holders.ReflectionReenterTimeHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.spawn.SpawnTemplate;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.type.ReflectionZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Reflection
{
	private static final Logger _log = LoggerFactory.getLogger(Reflection.class);
	
	private final int _id;
	private String _name;
	
	private int _ejectTime = Config.EJECT_DEAD_PLAYER_TIME;
	private final List<Integer> _players = new CopyOnWriteArrayList<>();
	private final List<Npc> _npcs = new CopyOnWriteArrayList<>();
	private final IntObjectMap<DoorInstance> _doors = new HashIntObjectMap<>();
	private final Map<Integer, ColosseumFence> _fences = new ConcurrentHashMap<>();
	private final List<ItemInstance> _items = new CopyOnWriteArrayList<>();
	private final Map<String, List<Spawner>> _manualSpawn = new HashMap<>();
	protected Map<String, List<Spawner>> _spawners = Collections.emptyMap();
	private final int[] _spawnsLoc = new int[3];
	private boolean _allowSummon = true;
	private long _emptyDestroyTime = -1;
	private long _lastLeft = -1;
	private long _instanceStartTime = -1;
	private long _instanceEndTime = -1;
	private boolean _isPvPInstance = false;
	private boolean _showTimer = false;
	private boolean _isTimerIncrease = true;
	private String _timerText = "";
	private Location _returnCoords = null;
	private boolean _disableMessages = false;
	private boolean _isHwidCheck = false;
	private final List<Integer> _zones = new ArrayList<>();
	
	private boolean _reuseUponEntry;
	private List<ReflectionReenterTimeHolder> _resetData = new ArrayList<>();
	private StatsSet _params = new StatsSet();
	
	protected ScheduledFuture<?> _checkTimeUpTask = null;
	protected final Map<Integer, ScheduledFuture<?>> _ejectDeadTasks = new ConcurrentHashMap<>();
	
	public Reflection(int id)
	{
		_id = id;
		_instanceStartTime = System.currentTimeMillis();
	}
	
	public Reflection(int id, String name)
	{
		_id = id;
		_name = name;
		_instanceStartTime = System.currentTimeMillis();
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public int getEjectTime()
	{
		return _ejectTime;
	}
	
	public void setEjectTime(int ejectTime)
	{
		_ejectTime = ejectTime;
	}
	
	public boolean isSummonAllowed()
	{
		return _allowSummon;
	}
	
	public void setAllowSummon(boolean b)
	{
		_allowSummon = b;
	}
	
	public boolean isPvPInstance()
	{
		return _isPvPInstance;
	}
	
	public void setPvPInstance(boolean b)
	{
		_isPvPInstance = b;
	}
	
	public void setDuration(int duration)
	{
		if (_checkTimeUpTask != null)
		{
			_checkTimeUpTask.cancel(true);
		}
		_checkTimeUpTask = ThreadPoolManager.getInstance().schedule(new CheckTimeUp(duration), 500);
		_instanceEndTime = System.currentTimeMillis() + duration + 500;
	}
	
	public void setEmptyDestroyTime(long time)
	{
		_emptyDestroyTime = time;
	}
	
	public boolean containsPlayer(int objectId)
	{
		return _players.contains(objectId);
	}
	
	public void addPlayer(int objectId)
	{
		_players.add(objectId);
	}
	
	public void removePlayer(Integer objectId)
	{
		_players.remove(objectId);
		if (_players.isEmpty() && (_emptyDestroyTime >= 0))
		{
			_lastLeft = System.currentTimeMillis();
			setDuration((int) (_instanceEndTime - System.currentTimeMillis() - 500));
		}
	}
	
	public void addNpc(Npc npc)
	{
		_npcs.add(npc);
	}
	
	public void removeNpc(Npc npc)
	{
		if (npc.getSpawn() != null)
		{
			npc.getSpawn().stopRespawn();
		}
		_npcs.remove(npc);
	}
	
	public void addDoor(int doorId, StatsSet set)
	{
		if (_doors.containsKey(doorId))
		{
			_log.warn("Door ID " + doorId + " already exists in instance " + getId());
			return;
		}
		
		final DoorTemplate temp = DoorParser.getInstance().getDoorTemplate(doorId);
		
		final DoorInstance newdoor = new DoorInstance(IdFactory.getInstance().getNextId(), temp, set);
		newdoor.setReflection(this);
		newdoor.setCurrentHp(newdoor.getMaxHp());
		
		final int gz = temp.posZ + 32;
		newdoor.spawnMe(temp.posX, temp.posY, gz);
		_doors.put(doorId, newdoor);
	}
	
	public void addEventDoor(int doorId, StatsSet set)
	{
		if (_doors.containsKey(doorId))
		{
			_log.warn("Door ID " + doorId + " already exists in instance " + getId());
			return;
		}
		
		final DoorTemplate temp = DoorParser.getInstance().getDoorTemplate(doorId);
		
		final DoorInstance newdoor = new DoorInstance(IdFactory.getInstance().getNextId(), temp, set);
		newdoor.setReflection(this);
		newdoor.setCurrentHp(newdoor.getMaxHp());
		
		final int gz = temp.posZ + 32;
		newdoor.spawnMe(temp.posX, temp.posY, gz);
		newdoor.openMe();
		_doors.put(doorId, newdoor);
	}
	
	public List<Integer> getPlayers()
	{
		return _players;
	}
	
	public List<Npc> getNpcs()
	{
		return _npcs;
	}
	
	public List<Npc> getAliveNpcs(int... id)
	{
		final List<Npc> result = new ArrayList<>();
		for (final Npc npc : _npcs)
		{
			if (npc != null && !npc.isDead() && ArrayUtils.contains(id, npc.getId()))
			{
				result.add(npc);
			}
		}
		return result;
	}
	
	public Collection<DoorInstance> getDoors()
	{
		return _doors.valueCollection();
	}
	
	public DoorInstance getDoor(int id)
	{
		return _doors.get(id);
	}
	
	public void openDoor(final int doorId)
	{
		final DoorInstance door = _doors.get(doorId);
		if (door != null)
		{
			door.openMe();
		}
	}
	
	public void closeDoor(final int doorId)
	{
		final DoorInstance door = _doors.get(doorId);
		if (door != null)
		{
			door.closeMe();
		}
	}
	
	public long getInstanceEndTime()
	{
		return _instanceEndTime;
	}
	
	public long getInstanceStartTime()
	{
		return _instanceStartTime;
	}
	
	public boolean isShowTimer()
	{
		return _showTimer;
	}
	
	public boolean isTimerIncrease()
	{
		return _isTimerIncrease;
	}
	
	public String getTimerText()
	{
		return _timerText;
	}
	
	public Location getReturnLoc()
	{
		return _returnCoords;
	}
	
	public void setReturnLoc(Location loc)
	{
		_returnCoords = loc;
	}
	
	public int[] getSpawnsLoc()
	{
		return _spawnsLoc;
	}
	
	public void setSpawnsLoc(int[] loc)
	{
		if ((loc == null) || (loc.length < 3))
		{
			return;
		}
		System.arraycopy(loc, 0, _spawnsLoc, 0, 3);
	}
	
	public void cleanupPlayers()
	{
		for (final Integer objectId : _players)
		{
			final Player player = GameObjectsStorage.getPlayer(objectId);
			if ((player != null) && (player.getReflectionId() == getId()))
			{
				if (player.getParty() != null && player.getParty().getCommandChannel() != null && player.getParty().getCommandChannel().isLeader(player))
				{
					player.getParty().getCommandChannel().setReflection(ReflectionManager.DEFAULT);
				}
				
				if (getReturnLoc() != null)
				{
					player.teleToLocation(getReturnLoc(), true, ReflectionManager.DEFAULT);
				}
				else
				{
					player.teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
				}
			}
		}
		_players.clear();
	}
	
	public void cleanupNpcs()
	{
		for (final Npc mob : _npcs)
		{
			if (mob != null)
			{
				if (mob.getSpawn() != null)
				{
					mob.getSpawn().stopRespawn();
				}
				mob.deleteMe();
			}
		}
		
		for (final String group : _spawners.keySet())
		{
			despawnByGroup(group);
		}
		_npcs.clear();
		_manualSpawn.clear();
		_spawners.clear();
	}
	
	public void cleanupDoors()
	{
		for (final DoorInstance door : _doors.valueCollection())
		{
			if (door != null)
			{
				door.decayMe();
			}
		}
		_doors.clear();
	}
	
	public List<Npc> spawnGroup(String groupName)
	{
		List<Npc> ret = null;
		if (_manualSpawn.containsKey(groupName))
		{
			final List<Spawner> manualSpawn = _manualSpawn.get(groupName);
			ret = new ArrayList<>(manualSpawn.size());
			
			for (final Spawner spawnDat : manualSpawn)
			{
				ret.add(spawnDat.doSpawn());
			}
		}
		else
		{
			_log.warn(getName() + " instance: cannot spawn NPC's, wrong group name: " + groupName);
		}
		
		return ret;
	}
	
	public void loadReflectionTemplate(ReflectionTemplate template)
	{
		if (template != null)
		{
			_name = template.getName();
			if (template.getTimelimit() != 0)
			{
				_checkTimeUpTask = ThreadPoolManager.getInstance().schedule(new CheckTimeUp(template.getTimelimit() * 60000), 15000);
				_instanceEndTime = System.currentTimeMillis() + (template.getTimelimit() * 60000) + 15000;
			}
			_ejectTime = template.getRespawnTime();
			_allowSummon = template.isSummonAllowed();
			_emptyDestroyTime = template.getCollapseIfEmpty() * 60000;
			_showTimer = template.isShowTimer();
			_isTimerIncrease = template.isTimerIncrease();
			_timerText = template.getTimerText();
			_isPvPInstance = template.isPvPInstance();
			_returnCoords = template.getReturnCoords();
			_reuseUponEntry = template.getReuseUponEntry();
			_resetData = template.getReenterData();
			_isHwidCheck = template.isHwidCheck();
			_params = template.getParams();
			
			if (template.getDoorList() != null && !template.getDoorList().isEmpty())
			{
				for (final int doorId : template.getDoorList().keySet())
				{
					addDoor(doorId, template.getDoorList().get(doorId));
				}
			}
			
			if (template.getSpawnsInfo() != null && !template.getSpawnsInfo().isEmpty())
			{
				for (final ReflectionTemplate.SpawnInfo s : template.getSpawnsInfo())
				{
					switch (s.getSpawnType())
					{
						case 0 :
							for (final Location loc : s.getCoords())
							{
								try
								{
									final SpawnTemplate tpl = new SpawnTemplate("none", s.getCount(), s.getRespawnDelay(), s.getRespawnRnd());
									tpl.addSpawnRange(loc);
									
									final Spawner c = new Spawner(NpcsParser.getInstance().getTemplate(s.getId()));
									c.setAmount(s.getCount());
									c.setSpawnTemplate(tpl);
									c.setLocation(c.calcSpawnRangeLoc(NpcsParser.getInstance().getTemplate(s.getId())));
									c.setReflection(this);
									c.setRespawnDelay(s.getRespawnDelay(), s.getRespawnRnd());
									if (s.getRespawnDelay() == 0)
									{
										c.stopRespawn();
									}
									else
									{
										c.startRespawn();
									}
									final Npc npc = c.spawnOne(true);
									addNpc(npc);
								}
								catch (final Exception e)
								{
									_log.warn(getClass().getSimpleName() + ": Spawn could not be initialized: " + e.getMessage(), e);
								}
							}
							break;
						case 1 :
							final Location loc = s.getCoords().get(Rnd.get(s.getCoords().size()));
							try
							{
								final SpawnTemplate tpl = new SpawnTemplate("none", s.getCount(), s.getRespawnDelay(), s.getRespawnRnd());
								tpl.addSpawnRange(loc);
								
								final Spawner c = new Spawner(NpcsParser.getInstance().getTemplate(s.getId()));
								c.setAmount(1);
								c.setSpawnTemplate(tpl);
								c.setLocation(c.calcSpawnRangeLoc(NpcsParser.getInstance().getTemplate(s.getId())));
								c.setReflection(this);
								c.setRespawnDelay(s.getRespawnDelay(), s.getRespawnRnd());
								if (s.getRespawnDelay() == 0)
								{
									c.stopRespawn();
								}
								else
								{
									c.startRespawn();
								}
								final Npc npc = c.spawnOne(true);
								addNpc(npc);
							}
							catch (final Exception e)
							{
								_log.warn(getClass().getSimpleName() + ": Spawn could not be initialized: " + e.getMessage(), e);
							}
							break;
						case 2 :
							int totalCount = 0;
							while (totalCount < s.getCount())
							{
								try
								{
									final SpawnTemplate tpl = new SpawnTemplate("none", s.getCount(), s.getRespawnDelay(), s.getRespawnRnd());
									tpl.addSpawnRange(s.getLoc());
									
									final Spawner c = new Spawner(NpcsParser.getInstance().getTemplate(s.getId()));
									c.setAmount(1);
									c.setSpawnTemplate(tpl);
									c.setLocation(c.calcSpawnRangeLoc(NpcsParser.getInstance().getTemplate(s.getId())));
									c.setReflection(this);
									c.setRespawnDelay(s.getRespawnDelay(), s.getRespawnRnd());
									if (s.getRespawnDelay() == 0)
									{
										c.stopRespawn();
									}
									else
									{
										c.startRespawn();
									}
									final Npc npc = c.spawnOne(true);
									addNpc(npc);
									totalCount++;
								}
								catch (final Exception e)
								{
									_log.warn(getClass().getSimpleName() + ": Spawn could not be initialized: " + e.getMessage(), e);
								}
							}
					}
				}
			}
			
			if (template.getSpawns().size() > 0)
			{
				_spawners = new HashMap<>(template.getSpawns().size());
				for (final Map.Entry<String, ReflectionTemplate.SpawnInfo2> entry : template.getSpawns().entrySet())
				{
					final List<Spawner> spawnList = new ArrayList<>(entry.getValue().getTemplates().size());
					_spawners.put(entry.getKey(), spawnList);
					
					for (final Spawner c : entry.getValue().getTemplates())
					{
						c.setReflection(this);
						spawnList.add(c);
					}
					
					if (entry.getValue().isSpawned())
					{
						spawnByGroup(entry.getKey());
					}
				}
			}
		}
	}
	
	public void loadInstanceTemplate(String filename)
	{
		Document doc = null;
		final File xml = new File(Config.DATAPACK_ROOT, "data/instances/" + filename);
		
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(xml);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("instance".equalsIgnoreCase(n.getNodeName()))
				{
					parseInstance(n);
				}
			}
		}
		catch (final IOException e)
		{
			_log.warn("Instance: can not find " + xml.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
		catch (final Exception e)
		{
			_log.warn("Instance: error while loading " + xml.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
	}
	
	private void parseInstance(Node n) throws Exception
	{
		Spawner spawnDat;
		NpcTemplate npcTemplate;
		_name = n.getAttributes().getNamedItem("name").getNodeValue();
		Node a = n.getAttributes().getNamedItem("ejectTime");
		if (a != null)
		{
			_ejectTime = 1000 * Integer.parseInt(a.getNodeValue());
		}
		final Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("activityTime".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					_checkTimeUpTask = ThreadPoolManager.getInstance().schedule(new CheckTimeUp(Integer.parseInt(a.getNodeValue()) * 60000), 15000);
					_instanceEndTime = System.currentTimeMillis() + (Long.parseLong(a.getNodeValue()) * 60000) + 15000;
				}
			}
			else if ("allowSummon".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					setAllowSummon(Boolean.parseBoolean(a.getNodeValue()));
				}
			}
			else if ("emptyDestroyTime".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					_emptyDestroyTime = Long.parseLong(a.getNodeValue()) * 1000;
				}
			}
			else if ("showTimer".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					_showTimer = Boolean.parseBoolean(a.getNodeValue());
				}
				a = n.getAttributes().getNamedItem("increase");
				if (a != null)
				{
					_isTimerIncrease = Boolean.parseBoolean(a.getNodeValue());
				}
				a = n.getAttributes().getNamedItem("text");
				if (a != null)
				{
					_timerText = a.getNodeValue();
				}
			}
			else if ("PvPInstance".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					setPvPInstance(Boolean.parseBoolean(a.getNodeValue()));
				}
			}
			else if ("doorlist".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					int doorId = 0;
					
					if ("door".equalsIgnoreCase(d.getNodeName()))
					{
						doorId = Integer.parseInt(d.getAttributes().getNamedItem("doorId").getNodeValue());
						final StatsSet set = new StatsSet();
						for (Node bean = d.getFirstChild(); bean != null; bean = bean.getNextSibling())
						{
							if ("set".equalsIgnoreCase(bean.getNodeName()))
							{
								final NamedNodeMap attrs = bean.getAttributes();
								final String setname = attrs.getNamedItem("name").getNodeValue();
								final String value = attrs.getNamedItem("val").getNodeValue();
								set.set(setname, value);
							}
						}
						addDoor(doorId, set);
					}
				}
			}
			else if ("colloseum_fence_list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node group = n.getFirstChild(); group != null; group = group.getNextSibling())
				{
					if ("colosseum_fence".equalsIgnoreCase(group.getNodeName()))
					{
						final int x = Integer.parseInt(group.getAttributes().getNamedItem("x").getNodeValue());
						final int y = Integer.parseInt(group.getAttributes().getNamedItem("y").getNodeValue());
						final int z = Integer.parseInt(group.getAttributes().getNamedItem("z").getNodeValue());
						final int minz = Integer.parseInt(group.getAttributes().getNamedItem("min_z").getNodeValue());
						final int maxz = Integer.parseInt(group.getAttributes().getNamedItem("max_z").getNodeValue());
						final int width = Integer.parseInt(group.getAttributes().getNamedItem("width").getNodeValue());
						final int height = Integer.parseInt(group.getAttributes().getNamedItem("height").getNodeValue());
						addFence(x, y, z, minz, maxz, width, height, FenceState.CLOSED);
					}
				}
			}
			else if ("spawnlist".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node group = n.getFirstChild(); group != null; group = group.getNextSibling())
				{
					if ("group".equalsIgnoreCase(group.getNodeName()))
					{
						final String spawnGroup = group.getAttributes().getNamedItem("name").getNodeValue();
						final List<Spawner> manualSpawn = new ArrayList<>();
						for (Node d = group.getFirstChild(); d != null; d = d.getNextSibling())
						{
							int npcId = 0, x = 0, y = 0, z = 0, heading = 0,
							        respawn = 0, respawnRandom = 0, delay = -1;
							
							if ("spawn".equalsIgnoreCase(d.getNodeName()))
							{
								
								npcId = Integer.parseInt(d.getAttributes().getNamedItem("npcId").getNodeValue());
								x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
								y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
								z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
								heading = Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue());
								respawn = Integer.parseInt(d.getAttributes().getNamedItem("respawn").getNodeValue());
								if (d.getAttributes().getNamedItem("onKillDelay") != null)
								{
									delay = Integer.parseInt(d.getAttributes().getNamedItem("onKillDelay").getNodeValue());
								}
								
								if (d.getAttributes().getNamedItem("respawnRandom") != null)
								{
									respawnRandom = Integer.parseInt(d.getAttributes().getNamedItem("respawnRandom").getNodeValue());
								}
								
								npcTemplate = NpcsParser.getInstance().getTemplate(npcId);
								if (npcTemplate != null)
								{
									spawnDat = new Spawner(npcTemplate);
									spawnDat.setX(x);
									spawnDat.setY(y);
									spawnDat.setZ(z);
									spawnDat.setAmount(1);
									spawnDat.setHeading(heading);
									spawnDat.setRespawnDelay(respawn, respawnRandom);
									if (respawn == 0)
									{
										spawnDat.stopRespawn();
									}
									else
									{
										spawnDat.startRespawn();
									}
									spawnDat.setReflection(this);
									if (spawnGroup.equals("general"))
									{
										final Npc spawned = spawnDat.doSpawn();
										if ((delay >= 0) && (spawned instanceof Attackable))
										{
											((Attackable) spawned).setOnKillDelay(delay);
										}
									}
									else
									{
										manualSpawn.add(spawnDat);
									}
								}
								else
								{
									_log.warn("Instance: Data missing in NPC table for ID: " + npcId + " in Instance " + getId());
								}
							}
						}
						if (!manualSpawn.isEmpty())
						{
							_manualSpawn.put(spawnGroup, manualSpawn);
						}
					}
				}
			}
			else if ("spawnpoint".equalsIgnoreCase(n.getNodeName()))
			{
				try
				{
					final int x = Integer.parseInt(n.getAttributes().getNamedItem("spawnX").getNodeValue());
					final int y = Integer.parseInt(n.getAttributes().getNamedItem("spawnY").getNodeValue());
					final int z = Integer.parseInt(n.getAttributes().getNamedItem("spawnZ").getNodeValue());
					_returnCoords = new Location(x, y, z);
				}
				catch (final Exception e)
				{
					_log.warn("Error parsing instance xml: " + e.getMessage(), e);
					_returnCoords = null;
				}
			}
		}
	}
	
	protected void doCheckTimeUp(int remaining)
	{
		CreatureSay cs = null;
		int timeLeft;
		int interval;
		
		if (_players.isEmpty() && (_emptyDestroyTime == 0))
		{
			remaining = 0;
			interval = 500;
		}
		else if (_players.isEmpty() && (_emptyDestroyTime > 0))
		{
			
			final Long emptyTimeLeft = (_lastLeft + _emptyDestroyTime) - System.currentTimeMillis();
			if (emptyTimeLeft <= 0)
			{
				interval = 0;
				remaining = 0;
			}
			else if ((remaining > 300000) && (emptyTimeLeft > 300000))
			{
				interval = 300000;
				remaining = remaining - 300000;
			}
			else if ((remaining > 60000) && (emptyTimeLeft > 60000))
			{
				interval = 60000;
				remaining = remaining - 60000;
			}
			else if ((remaining > 30000) && (emptyTimeLeft > 30000))
			{
				interval = 30000;
				remaining = remaining - 30000;
			}
			else
			{
				interval = 10000;
				remaining = remaining - 10000;
			}
		}
		else if (remaining > 300000)
		{
			timeLeft = remaining / 60000;
			interval = 300000;
			if (!_disableMessages)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
				sm.addString(Integer.toString(timeLeft));
				Announcements.getInstance().announceToInstance(sm, getId());
			}
			remaining = remaining - 300000;
		}
		else if (remaining > 60000)
		{
			timeLeft = remaining / 60000;
			interval = 60000;
			if (!_disableMessages)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
				sm.addString(Integer.toString(timeLeft));
				Announcements.getInstance().announceToInstance(sm, getId());
			}
			remaining = remaining - 60000;
		}
		else if (remaining > 30000)
		{
			timeLeft = remaining / 1000;
			interval = 30000;
			if (!_disableMessages)
			{
				cs = new CreatureSay(0, Say2.ALLIANCE, "Notice", timeLeft + " seconds left.");
			}
			remaining = remaining - 30000;
		}
		else
		{
			timeLeft = remaining / 1000;
			interval = 10000;
			if (!_disableMessages)
			{
				cs = new CreatureSay(0, Say2.ALLIANCE, "Notice", timeLeft + " seconds left.");
			}
			remaining = remaining - 10000;
		}
		
		if (cs != null)
		{
			for (final Integer objectId : _players)
			{
				final Player player = GameObjectsStorage.getPlayer(objectId);
				if ((player != null) && (player.getReflectionId() == getId()))
				{
					player.sendPacket(cs);
				}
			}
		}
		
		cancelTimer();
		if (remaining >= 10000)
		{
			_checkTimeUpTask = ThreadPoolManager.getInstance().schedule(new CheckTimeUp(remaining), interval);
		}
		else
		{
			_checkTimeUpTask = ThreadPoolManager.getInstance().schedule(new TimeUp(), interval);
		}
	}
	
	public void cancelTimer()
	{
		if (_checkTimeUpTask != null)
		{
			_checkTimeUpTask.cancel(true);
		}
	}
	
	public void cancelEjectDeadPlayer(Player player)
	{
		if (_ejectDeadTasks.containsKey(player.getObjectId()))
		{
			final ScheduledFuture<?> task = _ejectDeadTasks.remove(player.getObjectId());
			if (task != null)
			{
				task.cancel(true);
			}
		}
	}
	
	public void addEjectDeadTask(Player player)
	{
		if ((player != null))
		{
			_ejectDeadTasks.put(player.getObjectId(), ThreadPoolManager.getInstance().schedule(new EjectPlayer(player), _ejectTime));
		}
	}
	
	public final void notifyDeath(Creature killer, Creature victim)
	{
		final ReflectionWorld instance = ReflectionManager.getInstance().getPlayerWorld(victim.getActingPlayer());
		if (instance != null)
		{
			instance.onDeath(killer, victim);
		}
	}
	
	public class CheckTimeUp implements Runnable
	{
		private final int _remaining;
		
		public CheckTimeUp(int remaining)
		{
			_remaining = remaining;
		}
		
		@Override
		public void run()
		{
			doCheckTimeUp(_remaining);
		}
	}
	
	public class TimeUp implements Runnable
	{
		@Override
		public void run()
		{
			collapse();
		}
	}
	
	protected class EjectPlayer implements Runnable
	{
		private final Player _player;
		
		public EjectPlayer(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if ((_player != null) && _player.isDead() && (_player.getReflectionId() == getId()))
			{
				if (getReturnLoc() != null)
				{
					_player.teleToLocation(getReturnLoc(), true, ReflectionManager.DEFAULT);
				}
				else
				{
					_player.teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
				}
			}
		}
	}
	
	public void disableMessages()
	{
		_disableMessages = true;
	}

	public Npc getNpc(int id)
	{
		for (final Npc mob : _npcs)
		{
			if (mob != null)
			{
				if (mob.getId() == id)
				{
					return mob;
				}
			}
		}
		return null;
	}
	
	public void spawnByGroup(final String name)
	{
		final List<Spawner> list = _spawners.get(name);
		if (list == null)
		{
			return;
		}
		
		for (final Spawner s : list)
		{
			final Npc npc = s.spawnOne(true);
			addNpc(npc);
		}
	}
	
	public void despawnByGroup(final String name)
	{
		final List<Spawner> list = _spawners.get(name);
		if (list == null)
		{
			return;
		}
		
		for (final Spawner s : list)
		{
			s.stopRespawn();
			if (s.getLastSpawn() != null)
			{
				s.getLastSpawn().deleteMe();
			}
		}
	}
	
	public ColosseumFence addFence(int x, int y, int z, int minZ, int maxZ, int width, int height, FenceState state)
	{
		final ColosseumFence newFence = new ColosseumFence(this, x, y, z, minZ, maxZ, width, height, state);
		newFence.spawnMe();
		_fences.put(newFence.getObjectId(), newFence);
		return newFence;
	}
	
	public Collection<ColosseumFence> getFences()
	{
		return _fences.values();
	}

	public void cleanupFences()
	{
		for (final ColosseumFence fence : _fences.values())
		{
			if (fence == null)
			{
				continue;
			}
			fence.decayMe();
		}
		_fences.clear();
	}
	
	public void addItem(ItemInstance item)
	{
		_items.add(item);
	}
	
	public void removeItem(ItemInstance item)
	{
		_items.remove(item);
	}
	
	public void cleanupItems()
	{
		for (final ItemInstance item : _items)
		{
			if (item != null)
			{
				item.decayMe();
			}
		}
		_items.clear();
	}
	
	public boolean getReuseUponEntry()
	{
		return _reuseUponEntry;
	}
	
	public List<ReflectionReenterTimeHolder> getReenterData()
	{
		return _resetData;
	}
	
	public boolean isHwidCheck()
	{
		return _isHwidCheck;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
	
	public void removeZone(int id)
	{
		if (_zones.contains(id))
		{
			_zones.remove(_zones.indexOf(Integer.valueOf(id)));
		}
	}
	
	public void addZone(int id)
	{
		if (!_zones.contains(id))
		{
			_zones.add(id);
		}
	}
	
	public void cleanupZones()
	{
		if (!_zones.isEmpty())
		{
			for (final int zoneId : _zones)
			{
				final ReflectionZone zone = ZoneManager.getInstance().getZoneById(zoneId, ReflectionZone.class);
				if (zone != null)
				{
					zone.removeRef(getId());
				}
			}
		}
	}
	
	public boolean isDefault()
	{
		return getId() <= 0;
	}
	
	public void collapse()
	{
		cleanupNpcs();
		cleanupPlayers();
		cleanupDoors();
		cleanupItems();
		cleanupFences();
		cleanupZones();
		cancelTimer();
		ReflectionManager.getInstance().destroyRef(getId());
	}
}