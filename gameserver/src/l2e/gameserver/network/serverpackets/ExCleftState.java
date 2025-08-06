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
package l2e.gameserver.network.serverpackets;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.entity.events.cleft.AerialCleftTeam;

/**
 * Created by LordWinter 03.12.2018
 */
public class ExCleftState extends GameServerPacket
{
	private final CleftState _cleftState;
	private AerialCleftTeam _redTeam;
	private AerialCleftTeam _blueTeam;
	private AerialCleftTeam _catTeamUpdate;
	private AerialCleftTeam _towerDestroyTeam;
	private AerialCleftEvent _event;
	private Player _towerKiller;
	private Player _killer;
	private Player _killed;
	private int _towerId;
	private int _killerTeamId;
	private int _killedTeamId;

	private AerialCleftTeam _winTeam;
	private AerialCleftTeam _loseTeam;
	
	public ExCleftState(final CleftState cleftState)
	{
		_cleftState = cleftState;
	}

	public ExCleftState(final CleftState cleftState, AerialCleftEvent event, AerialCleftTeam blueTeam, AerialCleftTeam redTeam)
	{
		_cleftState = cleftState;
		_event = event;
		_blueTeam = blueTeam;
		_redTeam = redTeam;
	}

	public ExCleftState(final CleftState cleftState, AerialCleftEvent event, AerialCleftTeam catTeamUpdate)
	{
		_cleftState = cleftState;
		_event = event;
		_catTeamUpdate = catTeamUpdate;
	}
	
	public ExCleftState(final CleftState cleftState, AerialCleftEvent event, AerialCleftTeam towerDestroyTeam, AerialCleftTeam blueTeam, AerialCleftTeam redTeam, int towerId, Player killer)
	{
		_cleftState = cleftState;
		_event = event;
		_towerDestroyTeam = towerDestroyTeam;
		_blueTeam = blueTeam;
		_redTeam = redTeam;
		_towerId = towerId;
		_towerKiller = killer;
	}
	
	public ExCleftState(final CleftState cleftState, AerialCleftEvent event, AerialCleftTeam blueTeam, AerialCleftTeam redTeam, Player killer, Player killed, int killerTeamId, int killedTeamId)
	{
		_cleftState = cleftState;
		_blueTeam = blueTeam;
		_redTeam = redTeam;
		_event = event;
		_killer = killer;
		_killed = killed;
		_killerTeamId = killerTeamId;
		_killedTeamId = killedTeamId;
	}

	public ExCleftState(final CleftState cleftState, AerialCleftTeam winTeam, AerialCleftTeam loseTeam)
	{
		_cleftState = cleftState;
		_winTeam = winTeam;
		_loseTeam = loseTeam;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_cleftState.ordinal());
		switch (_cleftState)
		{
			case TOTAL :
				writeD(_event.getEventTimeEnd());
				writeD(_blueTeam.getPoints());
				writeD(_redTeam.getPoints());
				writeD(_blueTeam.getTeamCat().getObjectId());
				writeD(_redTeam.getTeamCat().getObjectId());
				writeS(_blueTeam.getTeamCat().getName(null));
				writeS(_redTeam.getTeamCat().getName(null));
				writeD(_blueTeam.getParticipatedPlayerCount());
				for (final Player player : _blueTeam.getParticipatedPlayers().values())
				{
					writeD(player.getObjectId());
					writeD(player.getCleftKills());
					writeD(player.getCleftDeaths());
					writeD(player.getCleftKillTowers());
				}
				writeD(_redTeam.getParticipatedPlayerCount());
				for (final Player player : _redTeam.getParticipatedPlayers().values())
				{
					writeD(player.getObjectId());
					writeD(player.getCleftKills());
					writeD(player.getCleftDeaths());
					writeD(player.getCleftKillTowers());
				}
				break;
			case TOWER_DESTROY :
				writeD(_event.getEventTimeEnd());
				writeD(_blueTeam.getPoints());
				writeD(_redTeam.getPoints());
				writeD(_towerDestroyTeam.getId());
				writeD(_towerId);
				writeD(_towerKiller.getObjectId());
				writeD(_towerKiller.getCleftKillTowers());
				writeD(_towerKiller.getCleftKills());
				writeD(_towerKiller.getCleftDeaths());
				break;
			case CAT_UPDATE :
				writeD(_event.getEventTimeEnd());
				writeD(_catTeamUpdate.getId());
				writeD(_catTeamUpdate.getTeamCat().getObjectId());
				writeS(_catTeamUpdate.getTeamCat().getName(null));
				break;
			case RESULT :
				writeD(_winTeam.getId());
				writeD(_loseTeam.getId());
				break;
			case PVP_KILL :
				writeD(_event.getEventTimeEnd());
				writeD(_blueTeam.getPoints());
				writeD(_redTeam.getPoints());
				writeD(_killerTeamId);
				writeD(_killer.getObjectId());
				writeD(_killer.getCleftKillTowers());
				writeD(_killer.getCleftKills());
				writeD(_killer.getCleftDeaths());
				writeD(_killedTeamId);
				writeD(_killed.getObjectId());
				writeD(_killed.getCleftKillTowers());
				writeD(_killed.getCleftKills());
				writeD(_killed.getCleftDeaths());
				break;
		}
	}
	
	public enum CleftState
	{
		TOTAL(0), TOWER_DESTROY(1), CAT_UPDATE(2), RESULT(3), PVP_KILL(4);
		
		private int _cleftState;
		
		CleftState(final int cleftState)
		{
			_cleftState = cleftState;
		}
		
		public int getState()
		{
			return _cleftState;
		}
	}
}