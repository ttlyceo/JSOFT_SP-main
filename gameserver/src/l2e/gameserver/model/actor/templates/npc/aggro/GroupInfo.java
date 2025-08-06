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
package l2e.gameserver.model.actor.templates.npc.aggro;

import java.util.HashSet;

import l2e.gameserver.model.actor.Player;

public class GroupInfo
{
	private final HashSet<Player> _players;
	private long _reward;
	
	public GroupInfo()
	{
		_players = new HashSet<>();
		_reward = 0;
	}
	
	public HashSet<Player> getPlayer()
	{
		return _players;
	}
	
	public void addReward(long value)
	{
		_reward += value;
	}
	
	public long getReward()
	{
		return _reward;
	}
}