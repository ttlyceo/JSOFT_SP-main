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

import java.util.List;

import l2e.gameserver.model.actor.Player;

public class ExBlockUpSetList extends GameServerPacket
{
	private List<Player> _bluePlayers;
	private List<Player> _redPlayers;
	private int _roomNumber;
	private Player _player;
	private boolean _isRedTeam;
	private int _seconds;
	private final int _type;
	
	public ExBlockUpSetList(List<Player> redPlayers, List<Player> bluePlayers, int roomNumber)
	{
		_redPlayers = redPlayers;
		_bluePlayers = bluePlayers;
		_roomNumber = roomNumber - 1;
		_type = 0;
	}
	
	public ExBlockUpSetList(Player player, boolean isRedTeam, boolean remove)
	{
		_player = player;
		_isRedTeam = isRedTeam;
		
		_type = !remove ? 1 : 2;
	}
	
	public ExBlockUpSetList(int seconds)
	{
		_seconds = seconds;
		_type = 3;
	}
	
	public ExBlockUpSetList(boolean isExCubeGameCloseUI)
	{
		_type = isExCubeGameCloseUI ? -1 : 4;
	}
	
	public ExBlockUpSetList(Player player, boolean fromRedTeam)
	{
		_player = player;
		_isRedTeam = fromRedTeam;
		_type = 5;
	}

	@Override
	protected void writeImpl()
	{
		writeD(_type);
		switch (_type)
		{
			case 0 :
				writeD(0xffffffff);
				writeD(_roomNumber);
				writeD(_bluePlayers.size());
				for (final Player player : _bluePlayers)
				{
					writeD(player.getObjectId());
					writeS(player.getName(null));
				}
				writeD(_redPlayers.size());
				for (final Player player : _redPlayers)
				{
					writeD(player.getObjectId());
					writeS(player.getName(null));
				}
				break;
			case 1 :
				writeD(0xffffffff);
				writeD(_isRedTeam ? 0x01 : 0x00);
				writeD(_player.getObjectId());
				writeS(_player.getName(null));
				break;
			case 2 :
				writeD(0xffffffff);
				writeD(_isRedTeam ? 0x01 : 0x00);
				writeD(_player.getObjectId());
				break;
			case 3 :
				writeD(_seconds);
				break;
			case 4 :
				break;
			case 5 :
				writeD(_player.getObjectId());
				writeD(_isRedTeam ? 0x01 : 0x00);
				writeD(_isRedTeam ? 0x00 : 0x01);
				break;
			case -1 :
				writeD(0xffffffff);
				break;
		}
	}
}