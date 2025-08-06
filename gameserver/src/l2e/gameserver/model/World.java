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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.TreasureChestInstance;

public final class World extends LoggerObject
{
	public static final int GRACIA_MAX_X = -166168;
	public static final int GRACIA_MAX_Z = 6105;
	public static final int GRACIA_MIN_Z = -895;
	public static final int TILE_SIZE = 32768;
	
	public static final int TILE_X_MIN = 11;
	public static final int TILE_Y_MIN = 10;
	public static final int TILE_X_MAX = 26;
	public static final int TILE_Y_MAX = 26;
	public static final int TILE_ZERO_COORD_X = 20;
	public static final int TILE_ZERO_COORD_Y = 18;
	
	public static final int WORLD_SIZE_X = 26 - 11 + 1;
	public static final int WORLD_SIZE_Y = 26 - 10 + 1;
	
	public static final int MAP_MIN_X = TILE_X_MIN - TILE_ZERO_COORD_X << 15;
	public static final int MAP_MAX_X = (TILE_X_MAX - 19 << 15) - 1;
	public static final int MAP_MIN_Y = TILE_Y_MIN - TILE_ZERO_COORD_Y << 15;
	public static final int MAP_MAX_Y = (TILE_Y_MAX - 17 << 15) - 1;
	public static final int MAP_MIN_Z = Config.MAP_MIN_Z;
	public static final int MAP_MAX_Z = Config.MAP_MAX_Z;
	
	public static final int SHIFT_BY = Config.SHIFT_BY;
	public static final int SHIFT_BY_FOR_Z = Config.SHIFT_BY_Z;
	
	public static final int OFFSET_X = Math.abs(MAP_MIN_X >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(MAP_MIN_Y >> SHIFT_BY);
	public static final int OFFSET_Z = Math.abs(MAP_MIN_Z >> SHIFT_BY_FOR_Z);
	
	private static final int REGIONS_X = (MAP_MAX_X >> SHIFT_BY) + OFFSET_X;
	private static final int REGIONS_Y = (MAP_MAX_Y >> SHIFT_BY) + OFFSET_Y;
	private static final int REGIONS_Z = (MAP_MAX_Z >> SHIFT_BY_FOR_Z) + OFFSET_Z;
	
	private final WorldRegion[][][] _worldRegions = new WorldRegion[REGIONS_X + 1][REGIONS_Y + 1][];
	
	protected World()
	{
		for (int x = 0; x <= REGIONS_X; x++)
		{
			for(int y = 0; y <= REGIONS_Y; y++)
			{
				_worldRegions[x][y] = new WorldRegion[REGIONS_Z + 1];
			}
		}
		info("(" + REGIONS_X + " by " + REGIONS_Y + ") world region grid set up.");
	}
	
	public static World getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public List<Player> getAllGMs()
	{
		return AdminParser.getInstance().getAllGms(true);
	}
	
	public List<WorldRegion> getNeighbors(int regX, int regY, int regZ, int deepH, int deepV)
	{
		final List<WorldRegion> neighbors = new ArrayList<>();
		deepH *= 2;
		deepV *= 2;
		int rx, ry, rz;
		for (int x = 0; x <= deepH; x++)
		{
			for (int y = 0; y <= deepH; y++)
			{
				for (int z = 0; z <= deepV; z++)
				{
					rx = regX + (x % 2 == 0 ? -x / 2 : x - x / 2);
					ry = regY + (y % 2 == 0 ? -y / 2 : y - y / 2);
					rz = 0;
					if (validRegion(rx, ry, rz))
					{
						if (_worldRegions[rx][ry].length > 1)
						{
							rz = regZ + (z % 2 == 0 ? -z / 2 : z - z / 2);
							if (!validRegion(rx, ry, rz))
							{
								continue;
							}
						}
						else
						{
							z = deepV + 1;
						}
						if (_worldRegions[rx][ry][rz] != null)
						{
							neighbors.add(_worldRegions[rx][ry][rz]);
						}
					}
				}
			}
		}
		return neighbors;
	}
	
	public List<Player> getAroundPlayers(GameObject object)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final List<Player> result = new ArrayList<>();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisiblePlayable())
			{
				if ((obj == null) || !obj.isPlayer() || obj.getObjectId() == oid || (obj.getReflectionId() != rid))
				{
					continue;
				}
				result.add((Player) obj);
			}
		}
		return result;
	}
	
	public List<Player> getAroundTraders(GameObject object)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final List<Player> result = new ArrayList<>();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisiblePlayable())
			{
				if ((obj == null) || !obj.isPlayer() || obj.getObjectId() == oid || (obj.getReflectionId() != rid))
				{
					continue;
				}
				
				if (obj.getActingPlayer().getPrivateStoreType() == Player.STORE_PRIVATE_NONE)
				{
					continue;
				}
				result.add((Player) obj);
			}
		}
		return result;
	}
	
	public List<Player> getAroundPlayers(GameObject object, int radius, int height)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		final int ox = object.getX();
		final int oy = object.getY();
		final int oz = object.getZ();
		final int sqrad = radius * radius;
		
		final List<Player> result = new ArrayList<>();
		
		for (final var regi : object.getWorldRegion().getNeighbors())
		{
			for (final var obj : regi.getVisiblePlayable())
			{
				if ((obj == null) || !obj.isPlayer() || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				if (Math.abs(obj.getZ() - oz) > height)
				{
					continue;
				}
				final int dx = Math.abs(obj.getX() - ox);
				if (dx > radius)
				{
					continue;
				}
				final int dy = Math.abs(obj.getY() - oy);
				if (dy > radius)
				{
					continue;
				}
				if (dx * dx + dy * dy > sqrad)
				{
					continue;
				}
				
				result.add((Player) obj);
			}
		}
		return result;
	}
	
	public List<GameObject> getAroundObjects(GameObject object)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final List<GameObject> result = new ArrayList<>();
		
		for (final var regi : object.getWorldRegion().getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if ((obj == null) || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				result.add(obj);
			}
		}
		return result;
	}
	
	public GameObject getAroundObjectById(GameObject object, int objId)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return null;
		}
		
		for (final var regi : object.getWorldRegion().getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if (obj != null && obj.getObjectId() == objId && obj.getReflectionId() == object.getReflectionId())
				{
					return obj;
				}
			}
		}
		return null;
	}
	
	public List<GameObject> getAroundObjects(GameObject object, int radius, int height)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		final int ox = object.getX();
		final int oy = object.getY();
		final int oz = object.getZ();
		final int sqrad = radius * radius;
		
		final List<GameObject> result = new ArrayList<>();
		for (final var regi : object.getWorldRegion().getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if ((obj == null) || obj.getObjectId() == oid || (obj.getReflectionId() != rid))
				{
					continue;
				}
				
				if (Math.abs(obj.getZ() - oz) > height)
				{
					continue;
				}
				final int dx = Math.abs(obj.getX() - ox);
				if (dx > radius)
				{
					continue;
				}
				final int dy = Math.abs(obj.getY() - oy);
				if (dy > radius)
				{
					continue;
				}
				if (dx * dx + dy * dy > sqrad)
				{
					continue;
				}
				
				result.add(obj);
			}
		}
		return result;
	}
	
	public List<Playable> getAroundPlayables(GameObject object)
	{
		final var reg = object.getWorldRegion();
		if (reg == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final List<Playable> result = new ArrayList<>();
		for (final var regi : reg.getNeighbors())
		{
			for (final var obj : regi.getVisiblePlayable())
			{
				if ((obj == null) || obj.getObjectId() == oid || (obj.getReflectionId() != rid))
				{
					continue;
				}
				result.add(obj);
			}
		}
		return result;
	}
	
	public List<Playable> getAroundPlayables(GameObject object, int radius, int height)
	{
		final var reg = object.getWorldRegion();
		if (reg == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final int ox = object.getX();
		final int oy = object.getY();
		final int oz = object.getZ();
		final int sqrad = radius * radius;
		
		final List<Playable> result = new ArrayList<>();
		for (final var regi : reg.getNeighbors())
		{
			for (final var obj : regi.getVisiblePlayable())
			{
				if ((obj == null) || !obj.isPlayable() || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				if (Math.abs(obj.getZ() - oz) > height)
				{
					continue;
				}
				final int dx = Math.abs(obj.getX() - ox);
				if (dx > radius)
				{
					continue;
				}
				final int dy = Math.abs(obj.getY() - oy);
				if (dy > radius)
				{
					continue;
				}
				if (dx * dx + dy * dy > sqrad)
				{
					continue;
				}
				result.add(obj);
			}
		}
		return result;
	}
	
	public List<Creature> getAroundCharacters(GameObject object)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final List<Creature> result = new ArrayList<>();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if ((obj == null) || !obj.isCreature() || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				result.add((Creature) obj);
			}
		}
		return result;
	}
	
	public List<Creature> getAroundCharacters(GameObject object, int radius, int height)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final int ox = object.getX();
		final int oy = object.getY();
		final int oz = object.getZ();
		final int sqrad = radius * radius;
		
		final List<Creature> result = new ArrayList<>();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if ((obj == null) || !obj.isCreature() || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				
				if (Math.abs(obj.getZ() - oz) > height)
				{
					continue;
				}
				
				final int dx = Math.abs(obj.getX() - ox);
				if (dx > radius)
				{
					continue;
				}
				
				final int dy = Math.abs(obj.getY() - oy);
				if (dy > radius)
				{
					continue;
				}
				
				if (dx * dx + dy * dy > sqrad)
				{
					continue;
				}
				result.add((Creature) obj);
			}
		}
		return result;
	}
	
	public List<Npc> getAroundNpc(GameObject object)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final List<Npc> result = new ArrayList<>();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if ((obj == null) || !obj.isNpc() || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				
				result.add((Npc) obj);
			}
		}
		return result;
	}
	
	public List<Npc> getAroundNpc(GameObject object, int radius, int height)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		final int ox = object.getX();
		final int oy = object.getY();
		final int oz = object.getZ();
		final int sqrad = radius * radius;
		
		final List<Npc> result = new ArrayList<>();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if ((obj == null) || !obj.isNpc() || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				if (Math.abs(obj.getZ() - oz) > height)
				{
					continue;
				}
				final int dx = Math.abs(obj.getX() - ox);
				if (dx > radius)
				{
					continue;
				}
				final int dy = Math.abs(obj.getY() - oy);
				if (dy > radius)
				{
					continue;
				}
				if (dx * dx + dy * dy > sqrad)
				{
					continue;
				}
				result.add((Npc) obj);
			}
		}
		return result;
	}
	
	public List<Attackable> getAroundFarmNpc(Player object, int radius, int height)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		final int ox = object.getX();
		final int oy = object.getY();
		final int oz = object.getZ();
		final int sqrad = radius * radius;
		
		final List<Attackable> result = new ArrayList<>();
		
		final var farmAttackChampion = object.getFarmSystem().isAttackChampion();
		final var farmAttackRaid = object.getFarmSystem().isAttackRaid();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if ((obj == null) || !obj.isNpc() || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				if (Math.abs(obj.getZ() - oz) > height)
				{
					continue;
				}
				final int dx = Math.abs(obj.getX() - ox);
				if (dx > radius)
				{
					continue;
				}
				final int dy = Math.abs(obj.getY() - oy);
				if (dy > radius)
				{
					continue;
				}
				if (dx * dx + dy * dy > sqrad)
				{
					continue;
				}
				
				if (obj instanceof Attackable)
				{
					final var npc = (Attackable) obj;
					
					if (!npc.hasAI() || (!farmAttackRaid && (npc.isRaid() || npc.isRaidMinion())) || (!farmAttackChampion && npc.getChampionTemplate() != null) || !npc.isMonster() || npc.isDead() || npc.isInvul() || !npc.isVisible() || (npc instanceof TreasureChestInstance))
					{
						continue;
					}
					result.add(npc);
				}
			}
		}
		
		if (!result.isEmpty())
		{
			Collections.sort(result, new DistanceComparator(object));
		}
		return result;
	}
	
	private static class DistanceComparator implements Comparator<Attackable>
	{
		private final Player _player;
		
		DistanceComparator(Player player)
		{
			_player = player;
		}
		
		@Override
		public int compare(Attackable o1, Attackable o2)
		{
			if (o1 == null || o2 == null)
			{
				return 0;
			}
			return Double.compare(Math.sqrt(_player.getDistanceSq(o1)), Math.sqrt(_player.getDistanceSq(o2)));
		}
	}
	
	public int getAroundTraders(Player object, int radius, int height)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return 0;
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		final int ox = object.getX();
		final int oy = object.getY();
		final int oz = object.getZ();
		final int sqrad = radius * radius;
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisibleObjects())
			{
				if ((obj == null) || (!obj.isNpc() && !obj.isPlayer()) || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				if (Math.abs(obj.getZ() - oz) > height)
				{
					continue;
				}
				final int dx = Math.abs(obj.getX() - ox);
				if (dx > radius)
				{
					continue;
				}
				final int dy = Math.abs(obj.getY() - oy);
				if (dy > radius)
				{
					continue;
				}
				if (dx * dx + dy * dy > sqrad)
				{
					continue;
				}
				
				if (obj.isNpc() || (obj.isPlayer() && obj.getActingPlayer().isInStoreMode()))
				{
					return 1;
				}
			}
		}
		return 0;
	}
	
	public List<DoorInstance> getAroundDoors(GameObject object)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		
		final List<DoorInstance> result = new ArrayList<>();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisibleDoors())
			{
				if (obj == null || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				result.add(obj);
			}
		}
		return result;
	}
	
	public List<DoorInstance> getAroundDoors(GameObject object, int radius)
	{
		final var currentRegion = object.getWorldRegion();
		if (currentRegion == null)
		{
			return Collections.emptyList();
		}
		
		final int oid = object.getObjectId();
		final int rid = object.getReflectionId();
		final int ox = object.getX();
		final int oy = object.getY();
		final int sqrad = radius * radius;
		
		final List<DoorInstance> result = new ArrayList<>();
		
		for (final var regi : currentRegion.getNeighbors())
		{
			for (final var obj : regi.getVisibleDoors())
			{
				if (obj == null || obj.getObjectId() == oid || obj.getReflectionId() != rid)
				{
					continue;
				}
				
				final int dx = Math.abs(obj.getX() - ox);
				if (dx > radius)
				{
					continue;
				}
				final int dy = Math.abs(obj.getY() - oy);
				if (dy > radius)
				{
					continue;
				}
				if (dx * dx + dy * dy > sqrad)
				{
					continue;
				}
				result.add(obj);
			}
		}
		return result;
	}
	
	public WorldRegion getRegion(Location loc)
	{
		return getRegion(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public WorldRegion getRegion(int x, int y, int z)
	{
		final int _x = (x >> SHIFT_BY) + OFFSET_X;
		final int _y = (y >> SHIFT_BY) + OFFSET_Y;
		int _z = 0;
		if (validRegion(_x, _y, _z))
		{
			if (_worldRegions[_x][_y].length > 1)
			{
				_z = (z >> SHIFT_BY_FOR_Z) + OFFSET_Z;
			}
			
			if (_worldRegions[_x][_y][_z] == null)
			{
				_worldRegions[_x][_y][_z] = new WorldRegion(_x, _y, _z);
			}
			return _worldRegions[_x][_y][_z];
		}
		return null;
	}
	
	public WorldRegion[][][] getAllWorldRegions()
	{
		return _worldRegions;
	}
	
	public boolean validRegion(int x, int y, int z)
	{
		return x >= 0 && x < REGIONS_X && y >= 0 && y < REGIONS_Y && z >= 0 && z < REGIONS_Z;
	}
	
	public void deleteVisibleNpcSpawns()
	{
		info("Deleting all visible NPC's.");
		for (int i = 0; i < REGIONS_X; i++)
		{
			for(int j = 0; j < REGIONS_Y; j++)
			{
				for(int k = 0; k < _worldRegions[i][j].length; k++)
				{
					if(_worldRegions[i][j][k] != null)
					{
						_worldRegions[i][j][k].deleteVisibleNpcSpawns();
					}
				}
			}
		}
		info("All visible NPC's deleted.");
	}
	
	private static class SingletonHolder
	{
		protected static final World _instance = new World();
	}
}