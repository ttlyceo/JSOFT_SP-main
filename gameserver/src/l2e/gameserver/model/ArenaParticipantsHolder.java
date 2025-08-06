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
import java.util.List;

import l2e.gameserver.instancemanager.HandysBlockCheckerManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.BlockCheckerEngine;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class ArenaParticipantsHolder
{
	private final int _arena;
	private final List<Player> _redPlayers;
	private final List<Player> _bluePlayers;
	private final BlockCheckerEngine _engine;
	
	public ArenaParticipantsHolder(int arena)
	{
		_arena = arena;
		_redPlayers = new ArrayList<>(6);
		_bluePlayers = new ArrayList<>(6);
		_engine = new BlockCheckerEngine(this, _arena);
	}

	public List<Player> getRedPlayers()
	{
		return _redPlayers;
	}

	public List<Player> getBluePlayers()
	{
		return _bluePlayers;
	}

	public List<Player> getAllPlayers()
	{
		final List<Player> all = new ArrayList<>(12);
		all.addAll(_redPlayers);
		all.addAll(_bluePlayers);
		return all;
	}

	public void addPlayer(Player player, int team)
	{
		if (team == 0)
		{
			_redPlayers.add(player);
		}
		else
		{
			_bluePlayers.add(player);
		}
	}

	public void removePlayer(Player player, int team)
	{
		if (team == 0)
		{
			_redPlayers.remove(player);
		}
		else
		{
			_bluePlayers.remove(player);
		}
	}

	public int getPlayerTeam(Player player)
	{
		if (_redPlayers.contains(player))
		{
			return 0;
		}
		else if (_bluePlayers.contains(player))
		{
			return 1;
		}
		else
		{
			return -1;
		}
	}

	public int getRedTeamSize()
	{
		return _redPlayers.size();
	}

	public int getBlueTeamSize()
	{
		return _bluePlayers.size();
	}

	public void broadCastPacketToTeam(GameServerPacket packet)
	{
		for (final Player p : _redPlayers)
		{
			p.sendPacket(packet);
		}
		for (final Player p : _bluePlayers)
		{
			p.sendPacket(packet);
		}
	}

	public void clearPlayers()
	{
		_redPlayers.clear();
		_bluePlayers.clear();
	}

	public BlockCheckerEngine getEvent()
	{
		return _engine;
	}

	public void updateEvent()
	{
		_engine.updatePlayersOnStart(this);
	}

	public void checkAndShuffle()
	{
		final int redSize = _redPlayers.size();
		final int blueSize = _bluePlayers.size();
		if (redSize > (blueSize + 1))
		{
			broadCastPacketToTeam(SystemMessage.getSystemMessage(SystemMessageId.TEAM_ADJUSTED_BECAUSE_WRONG_POPULATION_RATIO));
			final int needed = redSize - (blueSize + 1);
			for (int i = 0; i < (needed + 1); i++)
			{
				final Player plr = _redPlayers.get(i);
				if (plr == null)
				{
					continue;
				}
				HandysBlockCheckerManager.getInstance().changePlayerToTeam(plr, _arena, 1);
			}
		}
		else if (blueSize > (redSize + 1))
		{
			broadCastPacketToTeam(SystemMessage.getSystemMessage(SystemMessageId.TEAM_ADJUSTED_BECAUSE_WRONG_POPULATION_RATIO));
			final int needed = blueSize - (redSize + 1);
			for (int i = 0; i < (needed + 1); i++)
			{
				final Player plr = _bluePlayers.get(i);
				if (plr == null)
				{
					continue;
				}
				HandysBlockCheckerManager.getInstance().changePlayerToTeam(plr, _arena, 0);
			}
		}
	}
}