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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftTeam;

/**
 * Created by LordWinter 03.12.2018
 */
public class ExCleftList extends GameServerPacket
{
	public static final ExCleftList STATIC_CLOSE = new ExCleftList(CleftType.CLOSE);
	
	private final CleftType _cleftType;
	private AerialCleftTeam _redTeam;
	private AerialCleftTeam _blueTeam;
	private int _newTeamId;
	private int _oldTeamId;
	private Player _player;
	private int _playerObjectId;

	public ExCleftList(final CleftType cleftType, final Player player, int teamId)
	{
		_cleftType = cleftType;
		_newTeamId = teamId;
		_player = player;
	}

	public ExCleftList(final CleftType cleftType, int playerObjectId, int teamId)
	{
		_cleftType = cleftType;
		_playerObjectId = playerObjectId;
		_newTeamId = teamId;
	}

	public ExCleftList(final CleftType cleftType, int playerObjectId, int oldTeamId, int newTeamId)
	{
		_cleftType = cleftType;
		_playerObjectId = playerObjectId;
		_oldTeamId = oldTeamId;
		_newTeamId = newTeamId;
	}

	public ExCleftList(final CleftType cleftType, AerialCleftTeam redTeam, AerialCleftTeam blueTeam)
	{
		_cleftType = cleftType;
		_redTeam = redTeam;
		_blueTeam = blueTeam;
	}

	public ExCleftList(final CleftType cleftType)
	{
		_cleftType = cleftType;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_cleftType.getType());
		switch (_cleftType)
		{
			case TOTAL :
				writeD(Config.CLEFT_MIN_TEAM_PLAYERS);
				writeD(0xffffffff);
				
				writeD(_blueTeam.getParticipatedPlayerCount());
				for (final Player player : _blueTeam.getParticipatedPlayers().values())
				{
					writeD(player.getObjectId());
					writeS(player.getName(null));
				}
				
				writeD(_redTeam.getParticipatedPlayerCount());
				for (final Player player : _redTeam.getParticipatedPlayers().values())
				{
					writeD(player.getObjectId());
					writeS(player.getName(null));
				}
				break;
			case ADD :
				writeD(_newTeamId);
				writeD(_player.getObjectId());
				writeS(_player.getName(null));
				break;
			case REMOVE :
				writeD(_newTeamId);
				writeD(_playerObjectId);
				break;
			case TEAM_CHANGE :
				writeD(_playerObjectId);
				writeD(_oldTeamId);
				writeD(_newTeamId);
				break;
			case CLOSE :
				break;
		}
	}

	public static enum CleftType
	{
		CLOSE(-1), TOTAL(0), ADD(1), REMOVE(2), TEAM_CHANGE(3);

		private int _type;

		CleftType(final int type)
		{
			_type = type;
		}

		public int getType()
		{
			return _type;
		}
	}
}