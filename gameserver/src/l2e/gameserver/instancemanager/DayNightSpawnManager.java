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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.spawn.Spawner;

public class DayNightSpawnManager extends LoggerObject
{
	private final List<Spawner> _dayCreatures = new ArrayList<>();
	private final List<Spawner> _nightCreatures = new ArrayList<>();
	private final Map<Spawner, RaidBossInstance> _bosses = new ConcurrentHashMap<>();
	private int _mode = -1;

	public static DayNightSpawnManager getInstance()
	{
		return SingletonHolder._instance;
	}

	protected DayNightSpawnManager()
	{
	}

	public void addDayCreature(Spawner spawnDat)
	{
		_dayCreatures.add(spawnDat);
	}

	public void addNightCreature(Spawner spawnDat)
	{
		_nightCreatures.add(spawnDat);
	}

	public void spawnDayCreatures()
	{
		spawnCreatures(_nightCreatures, _dayCreatures, "night", "day");
	}

	public void spawnNightCreatures()
	{
		spawnCreatures(_dayCreatures, _nightCreatures, "day", "night");
	}

	private void spawnCreatures(List<Spawner> unSpawnCreatures, List<Spawner> spawnCreatures, String UnspawnLogInfo, String SpawnLogInfo)
	{
		try
		{
			if (!unSpawnCreatures.isEmpty())
			{
				int i = 0;
				for (final Spawner spawn : unSpawnCreatures)
				{
					if (spawn == null)
					{
						continue;
					}

					spawn.stopRespawn();
					final Npc last = spawn.getLastSpawn();
					if (last != null)
					{
						last.deleteMe();
						i++;
					}
				}
				info("Removed " + i + " " + UnspawnLogInfo + " creatures");
			}

			int i = 0;
			for (final Spawner spawnDat : spawnCreatures)
			{
				if (spawnDat == null)
				{
					continue;
				}
				spawnDat.startRespawn();
				spawnDat.doSpawn();
				i++;
			}

			info("Spawned " + i + " " + SpawnLogInfo + " creatures");
		}
		catch (final Exception e)
		{
			warn("Error while spawning creatures: " + e.getMessage(), e);
		}
	}

	public void changeMode(int mode)
	{
		if (_nightCreatures.isEmpty() && _dayCreatures.isEmpty() || _mode == mode)
		{
			return;
		}
		_mode = mode;
		switch (mode)
		{
			case 0 :
				spawnDayCreatures();
				specialNightBoss(0);
				break;
			case 1 :
				spawnNightCreatures();
				specialNightBoss(1);
				break;
		}
	}

	public DayNightSpawnManager trim()
	{
		((ArrayList<?>) _nightCreatures).trimToSize();
		((ArrayList<?>) _dayCreatures).trimToSize();
		return this;
	}

	public void notifyChangeMode()
	{
		try
		{
			if (GameTimeController.getInstance().isNight())
			{
				changeMode(1);
			}
			else
			{
				changeMode(0);
			}
		}
		catch (final Exception e)
		{
			warn("Error while notifyChangeMode(): " + e.getMessage(), e);
		}
	}

	public void cleanUp()
	{
		_nightCreatures.clear();
		_dayCreatures.clear();
		_bosses.clear();
	}

	public void specialNightBoss(int mode)
	{
		try
		{
			RaidBossInstance boss;
			for (final Spawner spawn : _bosses.keySet())
			{
				boss = _bosses.get(spawn);
				if ((boss == null || !boss.isVisible()) && (mode == 1))
				{
					boss = (RaidBossInstance) spawn.doSpawn();
					RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
					_bosses.remove(spawn);
					_bosses.put(spawn, boss);
					continue;
				}
				
				if ((boss == null) && (mode == 0))
				{
					continue;
				}
				
				if ((boss != null) && boss.getRaidStatus() == RaidBossSpawnManager.StatusEnum.ALIVE)
				{
					handleNightBoss(boss, mode);
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error while specialNoghtBoss(): " + e.getMessage(), e);
		}
	}

	private void handleNightBoss(RaidBossInstance boss, int mode)
	{
		switch (mode)
		{
			case 0 :
				if (boss.getSpawn().getLastSpawn() != null)
				{
					boss.getSpawn().getLastSpawn().deleteMe();
					info("Deleting Night Raid Boss " + boss.getName(null));
				}
				break;
			case 1 :
				if (boss.getSpawn().getLastSpawn() == null)
				{
					boss.getSpawn().doSpawn();
					info("Spawning Night Raid Boss " + boss.getName(null));
				}
				break;
		}
	}

	public RaidBossInstance handleBoss(Spawner spawnDat)
	{
		if (_bosses.containsKey(spawnDat))
		{
			return _bosses.get(spawnDat);
		}
		
		final var raidboss = (RaidBossInstance) spawnDat.doSpawn();
		_bosses.put(spawnDat, raidboss);

		if (!GameTimeController.getInstance().isNight())
		{
			if (raidboss.getSpawn().getLastSpawn() != null)
			{
				raidboss.getSpawn().getLastSpawn().deleteMe();
				return null;
			}
		}
		return raidboss;
	}

	private static class SingletonHolder
	{
		protected static final DayNightSpawnManager _instance = new DayNightSpawnManager();
	}
}