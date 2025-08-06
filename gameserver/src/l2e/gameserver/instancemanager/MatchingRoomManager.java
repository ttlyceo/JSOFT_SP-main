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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.CHashIntObjectMap;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.matching.MatchingRoom;

public class MatchingRoomManager
{
	private class RoomsHolder
	{
		private int _id = 1;
		
		private final IntObjectMap<MatchingRoom> _rooms = new CHashIntObjectMap<>();
		
		public int addRoom(MatchingRoom r)
		{
			final int val = _id++;
			_rooms.put(val, r);
			return val;
		}
	}
	
	private final RoomsHolder[] _holder = new RoomsHolder[2];
	private final Set<Player> _players = new CopyOnWriteArraySet<>();
	
	public MatchingRoomManager()
	{
		_holder[MatchingRoom.PARTY_MATCHING] = new RoomsHolder();
		_holder[MatchingRoom.CC_MATCHING] = new RoomsHolder();
	}
	
	public void addToWaitingList(Player player)
	{
		_players.add(player);
	}
	
	public void removeFromWaitingList(Player player)
	{
		_players.remove(player);
	}
	
	public List<Player> getWaitingList(int minLevel, int maxLevel, int[] classes)
	{
		final List<Player> res = new ArrayList<>();
		for (final Player member : _players)
		{
			if (member.getLevel() >= minLevel && member.getLevel() <= maxLevel)
			{
				if (classes.length == 0 || ArrayUtils.contains(classes, member.getClassId().getId()))
				{
					res.add(member);
				}
			}
		}
		
		return res;
	}
	
	public List<MatchingRoom> getMatchingRooms(int type, int region, boolean allLevels, Player activeChar)
	{
		final List<MatchingRoom> res = new ArrayList<>();
		for (final MatchingRoom room : _holder[type]._rooms.valueCollection())
		{
			if (region > 0 && room.getLocationId() != region)
			{
				continue;
			}
			else if (region == -2 && room.getLocationId() != MapRegionManager.getInstance().getBBs(activeChar.getLocation()))
			{
				continue;
			}
			if (!allLevels && (room.getMinLevel() > activeChar.getLevel() || room.getMaxLevel() < activeChar.getLevel()))
			{
				continue;
			}
			res.add(room);
		}
		return res;
	}
	
	public int addMatchingRoom(MatchingRoom r)
	{
		return _holder[r.getType()].addRoom(r);
	}
	
	public void removeMatchingRoom(MatchingRoom r)
	{
		_holder[r.getType()]._rooms.remove(r.getId());
	}
	
	public MatchingRoom getMatchingRoom(int type, int id)
	{
		return _holder[type]._rooms.get(id);
	}
	
	public static final MatchingRoomManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final MatchingRoomManager _instance = new MatchingRoomManager();
	}
}