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
package l2e.gameserver.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.holder.SpawnHolder;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.instance.DoorInstance;

public final class WorldRegion extends LoggerObject
{
	private final Set<Playable> _allPlayable = ConcurrentHashMap.newKeySet();
	private final Set<GameObject> _visibleObjects = ConcurrentHashMap.newKeySet();
	private final Set<DoorInstance> _visibleDoors = ConcurrentHashMap.newKeySet();

	private final int _tileX, _tileY, _tileZ;
	private boolean _active;
	private Future<?> _switchTask = null;

	public WorldRegion(int pTileX, int pTileY, int pTileZ)
	{
		_tileX = pTileX;
		_tileY = pTileY;
		_tileZ = pTileZ;
	}
	
	public void switchActive(boolean value)
	{
		if (!value)
		{
			final var oldTask = _switchTask;
			if (oldTask != null)
			{
				_switchTask = null;
				oldTask.cancel(false);
			}
			_switchTask = ThreadPoolManager.getInstance().schedule(new SwitchTask(value), 60000);
		}
		else
		{
			setActive(value);
		}
	}
	
	private class SwitchTask implements Runnable
	{
		boolean _value;
		public SwitchTask(boolean value)
		{
			_value = value;
		}
		
		@Override
		public void run()
		{
			if (isEmptyNeighborhood())
			{
				setActive(_value);
			}
		}
	}

	private void setActive(boolean value)
	{
		if (_active == value)
		{
			return;
		}
		
		_active = value;
		
		if (!value)
		{
			for (final var o : _visibleObjects)
			{
				if (o == null)
				{
					continue;
				}
				
				if (o instanceof Attackable)
				{
					final var mob = (Attackable) o;
					
					if (mob.isDead() || mob.isGlobalAI() || (mob.getLeader() != null && !mob.getLeader().isVisible()))
					{
						continue;
					}
					
					mob.setTarget(null);
					mob.stopMove(null);
					mob.stopAllEffects();
					mob.clearAggroList(false);
					mob.getAttackByList().clear();
					if (mob.hasAI())
					{
						mob.getAI().setIntention(CtrlIntention.IDLE);
						mob.getAI().stopAITask();
					}
				}
				
				if (o.isNpc())
				{
					((Npc) o).stopRandomAnimation();
				}
			}
		}
		else
		{
			for (final GameObject o : _visibleObjects)
			{
				if (o == null)
				{
					continue;
				}
				
				if (o instanceof Attackable)
				{
					((Attackable) o).getStatus().startHpMpRegeneration();
				}
				
				if (o.isNpc())
				{
					((Npc) o).startRandomAnimation();
				}
			}
		}
	}

	public boolean isActive()
	{
		return _active;
	}

	public boolean isEmptyNeighborhood()
	{
		for (final var neighbor : getNeighbors())
		{
			if (!neighbor.getVisiblePlayable().isEmpty())
			{
				return false;
			}
		}
		return true;
	}
	
	public void addVisibleObject(GameObject object)
	{
		if (object == null)
		{
			return;
		}

		_visibleObjects.add(object);
		if (object.isPlayable())
		{
			_allPlayable.add((Playable) object);
		}
		else if (object.isDoor())
		{
			_visibleDoors.add((DoorInstance) object);
		}
	}

	public void removeVisibleObject(GameObject object)
	{
		if (object == null || _visibleObjects.isEmpty())
		{
			return;
		}
		_visibleObjects.remove(object);
		if (object.isPlayable())
		{
			_allPlayable.remove(object);
		}
		else if (object.isDoor())
		{
			_visibleDoors.remove(object);
		}
	}

	public List<WorldRegion> getNeighbors()
	{
		return World.getInstance().getNeighbors(_tileX, _tileY, _tileZ, Config.REGIONS_DEEP_XY, Config.REGIONS_DEEP_Z);
	}

	public Collection<Playable> getVisiblePlayable()
	{
		return _allPlayable;
	}
	
	public Collection<DoorInstance> getVisibleDoors()
	{
		return _visibleDoors;
	}

	public Collection<GameObject> getVisibleObjects()
	{
		return _visibleObjects;
	}

	public String getName()
	{
		return "(" + _tileX + ", " + _tileY + ", " + _tileZ + ")";
	}

	public void deleteVisibleNpcSpawns()
	{
		info("Deleting all visible NPC's in Region: " + getName());
		for (final var obj : _visibleObjects)
		{
			if (obj instanceof Npc)
			{
				final var target = (Npc) obj;
				target.deleteMe();
				final var spawn = target.getSpawn();
				if (spawn != null)
				{
					spawn.stopRespawn();
					SpawnParser.getInstance().deleteSpawn(spawn);
					SpawnHolder.getInstance().deleteSpawn(spawn, false);
				}
			}
		}
		info("All visible NPC's deleted in Region: " + getName());
	}
}