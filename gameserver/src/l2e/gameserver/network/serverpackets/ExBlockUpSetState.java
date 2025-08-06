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

public class ExBlockUpSetState extends GameServerPacket
{
	private final int _type;
	private boolean _isRedTeamWin;
	private int _timeLeft;
	private int _bluePoints;
	private int _redPoints;
	private boolean _isRedTeam;
	private Player _player;
	private int _playerPoints;
	
	public ExBlockUpSetState(int timeLeft, int bluePoints, int redPoints, boolean isRedTeam, Player player, int playerPoints)
	{
		_timeLeft = timeLeft;
		_bluePoints = bluePoints;
		_redPoints = redPoints;
		_isRedTeam = isRedTeam;
		_player = player;
		_playerPoints = playerPoints;
		_type = 0;
	}

	public ExBlockUpSetState(boolean isRedTeamWin)
	{
		_isRedTeamWin = isRedTeamWin;
		_type = 1;
	}

	public ExBlockUpSetState(int timeLeft, int bluePoints, int redPoints)
	{
		_timeLeft = timeLeft;
		_bluePoints = bluePoints;
		_redPoints = redPoints;
		_type = 2;
	}
	
	@Override
	protected void writeImpl()
	{
		writeD(_type);
		switch (_type)
		{
			case 0 :
				writeD(_timeLeft);
				writeD(_bluePoints);
				writeD(_redPoints);
				writeD(_isRedTeam ? 0x01 : 0x00);
				writeD(_player.getObjectId());
				writeD(_playerPoints);
				break;
			case 1 :
				writeD(_isRedTeamWin ? 0x01 : 0x00);
				break;
			case 2 :
				writeD(_timeLeft);
				writeD(_bluePoints);
				writeD(_redPoints);
				break;
		}
	}
}