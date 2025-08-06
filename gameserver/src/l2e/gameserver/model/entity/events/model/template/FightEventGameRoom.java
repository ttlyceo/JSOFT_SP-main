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
package l2e.gameserver.model.entity.events.model.template;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.parser.FightEventMapParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.FightEventManager;

/**
 * Created by LordWinter
 */
public class FightEventGameRoom
{
	private final FightEventMap _map;
	private final AbstractFightEvent _event;
	private final int _roomMaxPlayers;
	private final int _teamsCount;
	private final Map<Integer, Player> _players = new ConcurrentHashMap<>();

	public FightEventGameRoom(AbstractFightEvent event)
	{
		_event = event;
		
		final String eventName = getChangedEventName(event);
		_map = Rnd.get(FightEventMapParser.getInstance().getMapsForEvent(eventName));
		_roomMaxPlayers = _map.getMaxAllPlayers();
		if (event.isTeamed())
		{
			_teamsCount = Rnd.get(_map.getTeamCount());
		}
		else
		{
			_teamsCount = 0;
		}
	}
	
	public void leaveRoom(Player player)
	{
		_players.remove(player.getObjectId());
		player.setFightEventGameRoom(null);
	}
	
	public int getMaxPlayers()
	{
		return _roomMaxPlayers;
	}
	
	public int getTeamsCount()
	{
		return _teamsCount;
	}
	
	public int getSlotsLeft()
	{
		return getMaxPlayers() - getPlayersCount();
	}
	
	public AbstractFightEvent getGame()
	{
		return _event;
	}
	
	public int getPlayersCount()
	{
		return _players.size();
	}
	
	public FightEventMap getMap()
	{
		return _map;
	}
	
	public Collection<Player> getAllPlayers()
	{
		return _players.values();
	}
	
	public void addAlonePlayer(Player player)
	{
		player.setFightEventGameRoom(this);
		addPlayerToTeam(player);
	}
	
	public boolean containsPlayer(Player player)
	{
		return _players.containsKey(player.getObjectId());
	}
	
	private void addPlayerToTeam(Player player)
	{
		_players.put(player.getObjectId(), player);
	}

	public void cleanUp()
	{
		_players.clear();
	}
	
	public static FightEventManager.CLASSES getPlayerClassGroup(Player player)
	{
		FightEventManager.CLASSES classType = null;
		for (final FightEventManager.CLASSES iClassType : FightEventManager.CLASSES.values())
		{
			for (final ClassId id : iClassType.getClasses())
			{
				if (id == player.getClassId())
				{
					classType = iClassType;
				}
			}
		}
		return classType;
	}

	private static String getChangedEventName(AbstractFightEvent event)
	{
		String eventName = event.getClass().getSimpleName();
		eventName = eventName.substring(0, eventName.length() - 5);
		return eventName;
	}
}