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

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.CHashIntObjectMap;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.items.instance.ItemInstance;

public class GameObjectsStorage
{
	private static IntObjectMap<GameObject> _objects = new CHashIntObjectMap<>(60000 * (int) Config.SPAWN_MULTIPLIER + Config.MAXIMUM_ONLINE_USERS);
	private static IntObjectMap<Npc> _npcs = new CHashIntObjectMap<>(60000 * (int) Config.SPAWN_MULTIPLIER);
	private static IntObjectMap<Player> _players = new CHashIntObjectMap<>(Config.MAXIMUM_ONLINE_USERS);
	private static IntObjectMap<Summon> _summons = new CHashIntObjectMap<>(Config.MAXIMUM_ONLINE_USERS);
	private static IntObjectMap<ItemInstance> _items = new CHashIntObjectMap<>();
	
	public static Player getPlayer(String name)
	{
		for (final var player : getPlayers())
		{
			if (player.getName(null).equalsIgnoreCase(name))
			{
				return player;
			}
		}
		return null;
	}
	
	public static Player getPlayer(int objId)
	{
		return _players.get(objId);
	}
	
	public static Collection<Player> getPlayers()
	{
		return _players.valueCollection();
	}
	
	public static GameObject findObject(int objId)
	{
		return _objects.get(objId);
	}
	
	public static Collection<GameObject> getObjects()
	{
		return _objects.valueCollection();
	}
	
	public static Collection<Npc> getNpcs()
	{
		return _npcs.valueCollection();
	}
	
	public static Summon getSummon(int objId)
	{
		return _summons.get(objId);
	}
	
	public static ItemInstance getItem(int objId)
	{
		return _items.get(objId);
	}
	
	public static void addItem(ItemInstance item)
	{
		_items.put(item.getObjectId(), item);
	}
	
	public static void removeItem(ItemInstance item)
	{
		_items.remove(item.getObjectId());
	}
	
	public static Npc getByNpcId(int npcId)
	{
		final Npc result = null;
		for (final var temp : getNpcs())
		{
			if (temp.getId() == npcId)
			{
				if (!temp.isDead() && temp.isVisible())
				{
					return temp;
				}
			}
		}
		return result;
	}
	
	public static Npc getNpc(int objId)
	{
		return _npcs.get(objId);
	}
	
	public static <T extends GameObject> void put(T o)
	{
		final IntObjectMap<T> map = getMapForObject(o);
		if (map != null)
		{
			map.put(o.isSummon() ? ((Summon) o).getOwner().getObjectId() : o.getObjectId(), o);
		}
		_objects.put(o.getObjectId(), o);
	}
	
	public static <T extends GameObject> void remove(T o)
	{
		final IntObjectMap<T> map = getMapForObject(o);
		if (map != null)
		{
			map.remove(o.isSummon() ? ((Summon) o).getOwner().getObjectId() : o.getObjectId());
		}
		_objects.remove(o.getObjectId());
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends GameObject> IntObjectMap<T> getMapForObject(T o)
	{
		if (o.isNpc())
		{
			return (IntObjectMap<T>) _npcs;
		}
		
		if (o.isSummon())
		{
			return (IntObjectMap<T>) _summons;
		}
		
		if (o.isPlayer())
		{
			return (IntObjectMap<T>) _players;
		}
		return null;
	}
	
	public static int getAllPlayersCount()
	{
		return getPlayers().size();
	}
	
	public static void clear()
	{
		_objects.clear();
		_npcs.clear();
		_summons.clear();
		_players.clear();
		_items.clear();
	}
}