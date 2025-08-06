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
package l2e.gameserver.model.spawn;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.ChampionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.interfaces.ILocational;
import l2e.gameserver.model.interfaces.IPositionable;
import l2e.gameserver.taskmanager.SpawnTaskManager;

public class Spawner implements IPositionable, IIdentifiable
{
	protected static final Logger _log = LoggerFactory.getLogger(Spawner.class);
	
	private NpcTemplate _template;
	private int _maximumCount;
	private int _currentCount;
	protected int _scheduledCount;
	private int _locationId;
	private String _territoryName;
	private Location _location = new Location();
	private int _respawnMinDelay;
	private int _respawnMaxDelay;
	private SchedulingPattern _respawnPattern;
	private Constructor<?> _constructor;
	private boolean _doRespawn;
	private boolean _customSpawn;
	private SpawnTemplate _spawnTemplate;
	private int _spawnIndex;
	private Reflection _reflection = ReflectionManager.DEFAULT;
	private String _minionList = null;
	private boolean _isFromDatabase;
	
	private final Deque<Npc> _spawnedNpcs = new ConcurrentLinkedDeque<>();
	private Map<Integer, Location> _lastSpawnPoints;
	
	public Spawner(NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		_template = mobTemplate;
		
		if (_template == null)
		{
			return;
		}
		final Class<?>[] parameters =
		{
		        int.class, Class.forName("l2e.gameserver.model.actor.templates.npc.NpcTemplate")
		};
		
		try
		{
			_constructor = Class.forName("l2e.gameserver.model.actor.instance." + _template.getType() + "Instance").getConstructor(parameters);
		}
		catch (final ClassNotFoundException e)
		{
			_constructor = Class.forName("l2e.scripts.types." + _template.getType() + "Instance").getConstructor(parameters);
		}
	}
	
	public int getAmount()
	{
		return _maximumCount;
	}
	
	public int getLocationId()
	{
		return _locationId;
	}

	@Override
	public Location getLocation()
	{
		return _location;
	}
	
	public Location getLocation(GameObject obj)
	{
		return ((_lastSpawnPoints == null) || (obj == null) || !_lastSpawnPoints.containsKey(obj.getObjectId())) ? _location : _lastSpawnPoints.get(obj.getObjectId());
	}
	
	@Override
	public int getId()
	{
		return _template.getId();
	}
	
	public int getX(GameObject obj)
	{
		return getLocation(obj).getX();
	}
	
	@Override
	public int getX()
	{
		return _location.getX();
	}
	
	@Override
	public void setX(int x)
	{
		_location.setX(x);
	}
	
	public int getY(GameObject obj)
	{
		return getLocation(obj).getY();
	}
	
	@Override
	public int getY()
	{
		return _location.getY();
	}
	
	@Override
	public void setY(int y)
	{
		_location.setY(y);
	}
	
	public int getZ(GameObject obj)
	{
		return getLocation(obj).getZ();
	}
	
	@Override
	public int getZ()
	{
		return _location.getZ();
	}
	
	@Override
	public void setZ(int z)
	{
		_location.setZ(z);
	}
	
	@Override
	public int getHeading()
	{
		return _location.getHeading();
	}
	
	@Override
	public void setHeading(int heading)
	{
		_location.setHeading(heading);
	}
	
	@Override
	public boolean setLocation(Location loc)
	{
		_location = loc;
		return true;
	}
	
	public void setLocationId(int id)
	{
		_locationId = id;
	}
	
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	public SchedulingPattern getRespawnPattern()
	{
		return _respawnPattern;
	}
	
	public void setAmount(int amount)
	{
		_maximumCount = amount;
	}
	
	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}
	
	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}
	
	public void setRespawnPattern(SchedulingPattern pattern)
	{
		_respawnPattern = pattern;
	}
	
	public void setCustom(boolean custom)
	{
		_customSpawn = custom;
	}
	
	public boolean isCustom()
	{
		return _customSpawn;
	}
	
	
	public void decreaseCount(Npc oldNpc)
	{
		if (_currentCount <= 0)
		{
			return;
		}
		_currentCount--;
		
		_spawnedNpcs.remove(oldNpc);
		if (_lastSpawnPoints != null)
		{
			_lastSpawnPoints.remove(oldNpc.getObjectId());
		}
		
		if (_doRespawn && ((_scheduledCount + _currentCount) < _maximumCount))
		{
			_scheduledCount++;
			
			int respawnTime = 0;
			if (getRespawnPattern() != null)
			{
				respawnTime = (int) (getRespawnPattern().next(System.currentTimeMillis()) - System.currentTimeMillis());
				_log.info("Spawner: " + oldNpc.getName(null) + " Dead! Respawn date [" + new Date((respawnTime + System.currentTimeMillis())) + "].");
			}
			else
			{
				respawnTime = hasRespawnRandom() ? Rnd.get(_respawnMinDelay, _respawnMaxDelay) : _respawnMinDelay;
			}
			SpawnTaskManager.getInstance().addSpawnTask(oldNpc, respawnTime);
		}
	}
	
	public int init()
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn();
		}
		_doRespawn = _respawnMinDelay != 0 || getRespawnPattern() != null;
		
		return _currentCount;
	}
	
	public Npc spawnOne(boolean val)
	{
		return doSpawn(val, 0);
	}
	
	public Npc spawnOne(boolean val, int triggerId)
	{
		return doSpawn(val, triggerId);
	}
	
	public boolean isRespawnEnabled()
	{
		return _doRespawn;
	}
	
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
	public Npc doSpawn()
	{
		return doSpawn(false, 0);
	}
	
	public Npc doSpawn(boolean isSummonSpawn, int triggerId)
	{
		Npc mob = null;
		try
		{
			if (_template.isType("Pet") || _template.isType("Decoy") || _template.isType("Trap") || _template.isType("EffectPoint"))
			{
				_currentCount++;
				
				return mob;
			}
			
			final Object[] parameters =
			{
			        IdFactory.getInstance().getNextId(), _template
			};
			final Object tmp = _constructor.newInstance(parameters);
			((GameObject) tmp).setReflection(getReflection());
			if (isSummonSpawn && (tmp instanceof Creature))
			{
				((Creature) tmp).setShowSummonAnimation(isSummonSpawn);
			}
			
			if (!(tmp instanceof Npc))
			{
				return mob;
			}
			mob = (Npc) tmp;
			return initializeNpcInstance(mob, triggerId);
		}
		catch (final Exception e)
		{
			_log.warn("NPC " + _template.getId() + " class not found", e);
		}
		return mob;
	}

	private Npc initializeNpcInstance(Npc mob, int triggerId)
	{
		int newlocx, newlocy, newlocz;
		if (triggerId != 0)
		{
			mob.setTriggerId(triggerId);
		}
		if (getSpawnTemplate() != null)
		{
			final Location loc = calcSpawnRangeLoc(mob.getTemplate());
			if (loc != null)
			{
				setX(loc.getX());
				setY(loc.getY());
				if (mob.isAttackable() && !mob.isFlying())
				{
					setZ(GeoEngine.getInstance().getSpawnHeight(loc.getX(), loc.getY(), loc.getZ()));
				}
				else
				{
					setZ(loc.getZ());
				}
			}
		}
		else if ((getX() == 0) && (getY() == 0))
		{
			_log.warn("Problem with spawn location at npc id:" + mob.getId());
			return null;
		}
		else
		{
			if (mob.isAttackable() && !mob.isFlying())
			{
				final var z = mob.getGeoZ(getLocation());
				if (!(Math.abs(z - getZ()) > 200))
				{
					setZ(z);
				}
			}
		}
		
		newlocx = getX();
		newlocy = getY();
		newlocz = getZ();
		
		mob.stopAllEffects();
		mob.setIsDead(false);
		mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
		mob.setScriptValue(0);
		if (getHeading() == -1)
		{
			mob.setHeading(Rnd.nextInt(61794));
		}
		else
		{
			mob.setHeading(getHeading());
		}
		
		var lifeTime = 0;
		if (mob.isMonster() && !getTemplate().getCanChampion() && !getTemplate().isQuestMonster() && !mob.isRaid() && !mob.isRaidMinion())
		{
			if (mob.getChampionTemplate() != null)
			{
				if (mob instanceof Attackable)
				{
					((Attackable) mob).setIsGlobalAI(false);
				}
				mob.setChampionTemplate(null);
				mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			}

			if (ChampionManager.getInstance().ENABLE_EXT_CHAMPION_MODE)
			{
				final int rnd = Rnd.get(ChampionManager.getInstance().EXT_CHAMPION_MODE_MAX_ROLL_VALUE);
				for (final ChampionTemplate ct : ChampionManager.getInstance().getChampionTemplates())
				{
					if ((ct.switchIdList && !ct.npcIdList.isEmpty() && !ct.npcIdList.contains(mob.getId())) || (!ct.switchIdList && !ct.npcIdList.isEmpty() && ct.npcIdList.contains(mob.getId())))
					{
						continue;
					}
					
					if ((ct.spawnsInInstances || mob.getReflectionId() == 0) && rnd >= ct.minChance && rnd <= ct.maxChance && mob.getLevel() >= ct.minLevel && mob.getLevel() <= ct.maxLevel)
					{
						mob.setChampionTemplate(ct);
						mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
						if (ct.lifeTime > 0)
						{
							lifeTime = ct.lifeTime;
						}
						break;
					}
				}
			}
		}
		mob.setSpawn(this);
		mob.spawnMe(newlocx, newlocy, newlocz);
		if (lifeTime > 0)
		{
			final var ai = mob.getAI();
			if (ai != null)
			{
				if (mob instanceof Attackable)
				{
					((Attackable) mob).setIsGlobalAI(true);
				}
				ai.setLifeTime(lifeTime);
				ai.enableAI();
			}
		}
		_spawnedNpcs.add(mob);
		if (_lastSpawnPoints != null)
		{
			_lastSpawnPoints.put(mob.getObjectId(), new Location(newlocx, newlocy, newlocz));
		}
		
		if (Config.DEBUG)
		{
			_log.info("Spawned Mob Id: " + _template.getId() + " , at: X: " + mob.getX() + " Y: " + mob.getY() + " Z: " + mob.getZ());
		}
		_currentCount++;
		return mob;
	}
	
	public void setRespawnDelay(int delay, int randomInterval)
	{
		if (delay != 0)
		{
			if (delay < 0)
			{
				_log.warn("respawn delay is negative for spawn:" + this);
			}
			final int minDelay = delay - randomInterval;
			final int maxDelay = delay + randomInterval;
			_respawnMinDelay = Math.max(10, minDelay) * 1000;
			_respawnMaxDelay = Math.max(10, maxDelay) * 1000;
		}
		else
		{
			_respawnMinDelay = 0;
			_respawnMaxDelay = 0;
		}
	}
	
	public void setRespawnDelay(int delay)
	{
		setRespawnDelay(delay, 0);
	}
	
	public int getRespawnDelay()
	{
		return (_respawnMinDelay + _respawnMaxDelay) / 2;
	}
	
	public boolean hasRespawnRandom()
	{
		return _respawnMinDelay != _respawnMaxDelay;
	}
	
	public Npc getLastSpawn()
	{
		return _spawnedNpcs.peekLast();
	}
	
	public final Deque<Npc> getSpawnedNpcs()
	{
		return _spawnedNpcs;
	}
	
	public void respawnNpc(Npc oldNpc)
	{
		if (_doRespawn)
		{
			oldNpc.refreshID();
			initializeNpcInstance(oldNpc, 0);
		}
	}
	
	public NpcTemplate getTemplate()
	{
		return _template;
	}
	
	public Reflection getReflection()
	{
		return _reflection;
	}
	
	public void setReflection(Reflection ref)
	{
		_reflection = ref;
	}

	public SpawnTemplate getSpawnTemplate()
	{
		return _spawnTemplate;
	}

	public void setSpawnTemplate(SpawnTemplate spawnRange)
	{
		_spawnTemplate = spawnRange;
	}
	
	public int generateSpawnIndex()
	{
		return _spawnIndex = _spawnTemplate != null && !_spawnTemplate.getSpawnRangeList().isEmpty() ? Rnd.get(_spawnTemplate.getSpawnRangeList().size()) : 0;
	}
	
	public SpawnRange calcSpawnRange()
	{
		if (getSpawnTemplate() != null)
		{
			final SpawnRange spawnRange = getSpawnTemplate().getSpawnRangeList().get(generateSpawnIndex());
			if (spawnRange == null)
			{
				_log.warn("Problem with calc SpawnRange at npc id:" + getId());
				return null;
			}
			return spawnRange;
		}
		return null;
	}
	
	public Location calcSpawnRangeLoc(NpcTemplate template)
	{
		final SpawnRange spawnRange = calcSpawnRange();
		if (spawnRange == null)
		{
			_log.warn("Problem with calc SpawnRange at npc id:" + getId());
			return null;
		}
		return spawnRange.getRandomLoc(template.isFlying());
	}
	
	public int getSpawnIndex()
	{
		return _spawnIndex;
	}

	@Override
	public String toString()
	{
		return "Spawner [_template=" + getId() + ", _locX=" + getX() + ", _locY=" + getY() + ", _locZ=" + getZ() + ", _heading=" + getHeading() + "]";
	}
	
	@Override
	public boolean setXYZ(int x, int y, int z)
	{
		setX(x);
		setY(y);
		setZ(z);
		return true;
	}
	
	@Override
	public boolean setXYZ(ILocational loc)
	{
		return setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public void setTerritoryName(String territoryName)
	{
		_territoryName = territoryName;
	}
	
	public String getTerritoryName()
	{
		return _territoryName;
	}
	
	public void setFromDatabase(boolean value)
	{
		_isFromDatabase = value;
	}
	
	public boolean isFromDatabase()
	{
		return _isFromDatabase;
	}
	
	public void decreaseScheduledCount()
	{
		_scheduledCount--;
	}
	
	public void setMinionList(String list)
	{
		_minionList = list;
	}
	
	public int[] getMinionList()
	{
		if (_minionList == null || _minionList.isEmpty())
		{
			return null;
		}
		
		final String[] list = _minionList.split(",");
		if (list == null || list.length < 2)
		{
			return null;
		}
		
		return new int[]
		{
		        Integer.parseInt(list[0]), Integer.parseInt(list[1])
		};
	}
}