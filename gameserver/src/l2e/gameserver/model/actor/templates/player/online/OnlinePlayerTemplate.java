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
package l2e.gameserver.model.actor.templates.player.online;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter
 */
public class OnlinePlayerTemplate
{
	private final String _hwid;
	private final String _ip;
	private final List<Integer> _players = new ArrayList<>();
	private final Map<Integer, Integer> _playerRewards = new HashMap<>();
	private final Map<Integer, Long> _playerTimers = new HashMap<>();
	
	public OnlinePlayerTemplate(String hwid, String ip)
	{
		_hwid = hwid;
		_ip = ip;
	}
	
	public String getHWID()
	{
		return _hwid;
	}
	
	public String getIP()
	{
		return _ip;
	}
	
	public List<Integer> getPlayers()
	{
		return _players;
	}
	
	public void addPlayer(Player player)
	{
		if (!_players.contains(player.getObjectId()))
		{
			_players.add(player.getObjectId());
		}
	}
	
	public void updatePlayerTimer(Player player, long time)
	{
		_playerTimers.put(player.getObjectId(), time);
	}
	
	public void updatePlayerTimer(int objId, long time)
	{
		_playerTimers.put(objId, time);
	}
	
	public long getPlayerTimer(Player player)
	{
		if (_playerTimers.containsKey(player.getObjectId()))
		{
			return _playerTimers.get(player.getObjectId());
		}
		return 0;
	}
	
	public long getPlayerTimer(int objId)
	{
		if (_playerTimers.containsKey(objId))
		{
			return _playerTimers.get(objId);
		}
		return 0;
	}
	
	public Map<Integer, Long> getPlayerTimers()
	{
		return _playerTimers;
	}
	
	public int getPlayerRewardId(Player player)
	{
		if (_playerRewards.containsKey(player.getObjectId()))
		{
			return _playerRewards.get(player.getObjectId());
		}
		return 0;
	}
	
	public int getPlayerRewardId(int objId)
	{
		if (_playerRewards.containsKey(objId))
		{
			return _playerRewards.get(objId);
		}
		return 0;
	}
	
	public Map<Integer, Integer> getPlayerRewards()
	{
		return _playerRewards;
	}
	
	public void updatePlayerRewardId(Player player, int nextId)
	{
		_playerRewards.put(player.getObjectId(), nextId);
	}
	
	public void updatePlayerRewardId(int objId, int nextId)
	{
		_playerRewards.put(objId, nextId);
	}
	
	public void removePlayer(Player player)
	{
		if (_players.contains(player.getObjectId()))
		{
			_players.remove(_players.indexOf(Integer.valueOf(player.getObjectId())));
		}
	}
	
	public boolean getPlayer(Player player)
	{
		return _players.contains(player.getObjectId());
	}
}