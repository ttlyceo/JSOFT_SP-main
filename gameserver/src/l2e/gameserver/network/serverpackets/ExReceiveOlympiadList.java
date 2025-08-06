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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.olympiad.AbstractOlympiadGame;
import l2e.gameserver.model.olympiad.OlympiadGameClassed;
import l2e.gameserver.model.olympiad.OlympiadGameManager;
import l2e.gameserver.model.olympiad.OlympiadGameNonClassed;
import l2e.gameserver.model.olympiad.OlympiadGameTask;
import l2e.gameserver.model.olympiad.OlympiadGameTeams;
import l2e.gameserver.model.olympiad.OlympiadInfo;
import l2e.gameserver.network.ServerPacketOpcodes;

public abstract class ExReceiveOlympiadList extends GameServerPacket
{
	@Override
	protected ServerPacketOpcodes getOpcodes()
	{
		return ServerPacketOpcodes.ExReceiveOlympiadList;
	}
	
	public static class OlympiadList extends ExReceiveOlympiadList
	{
		List<OlympiadGameTask> _games = new ArrayList<>();
		
		public OlympiadList()
		{
			OlympiadGameTask task;
			for (int i = 0; i < OlympiadGameManager.getInstance().getNumberOfStadiums(); i++)
			{
				task = OlympiadGameManager.getInstance().getOlympiadTask(i);
				if (task != null)
				{
					if (!task.isGameStarted() || task.isBattleFinished())
					{
						continue;
					}
					_games.add(task);
				}
			}
		}
	
		@Override
		protected final void writeImpl()
		{
			writeD(0x00);
			writeD(_games.size());
			writeD(0x00);
			for (final OlympiadGameTask curGame : _games)
			{
				final AbstractOlympiadGame game = curGame.getGame();
				if (game != null)
				{
					writeD(game.getStadiumId());
				
					if (game instanceof OlympiadGameNonClassed)
					{
						writeD(0x01);
					}
					else if (game instanceof OlympiadGameClassed)
					{
						writeD(0x02);
					}
					else if (game instanceof OlympiadGameTeams)
					{
						writeD(-1);
					}
					else
					{
						writeD(0x00);
					}
					writeD(curGame.isRunning() ? 0x02 : 0x01);
					writeS(game.getPlayerNames()[0]);
					writeS(game.getPlayerNames()[1]);
				}
			}
		}
	}
	
	public static class OlympiadResult extends ExReceiveOlympiadList
	{
		private final boolean _tie;
		private int _winTeam;
		private int _loseTeam = 2;
		private final List<OlympiadInfo> _winnerList;
		private final List<OlympiadInfo> _loserList;
		private Player _player;
		private boolean _isWinner = false;
		private boolean _isLoser = false;
		
		public OlympiadResult(boolean tie, int winTeam, List<OlympiadInfo> winnerList, List<OlympiadInfo> loserList)
		{
			_tie = tie;
			_winTeam = winTeam;
			_winnerList = winnerList;
			_loserList = loserList;
			
			if (_winTeam == 2)
			{
				_loseTeam = 1;
			}
			else if (_winTeam == 0)
			{
				_winTeam = 1;
			}
			
			if (Config.ALLOW_HIDE_OLY_POINTS)
			{
				_player = getClient().getActiveChar();
				for (final var winner : winnerList)
				{
					if (winner != null && winner.getName().equals(_player.getName(null)))
					{
						_isWinner = true;
						break;
					}
				}
				
				if (!_isWinner)
				{
					_isLoser = true;
				}
			}
		}
		
		@Override
		protected void writeImpl()
		{
			writeD(0x01);
			writeD(_tie ? 0x01 : 0x00);
			writeS(_winnerList.get(0).getName());
			writeD(_winTeam);
			writeD(_winnerList.size());
			for (final var info : _winnerList)
			{
				writeS(info.getName());
				writeS(info.getClanName());
				writeD(info.getClanId());
				writeD(info.getClassId());
				writeD(info.getDamage());
				writeD(Config.ALLOW_HIDE_OLY_POINTS ? _isWinner ? info.getCurrentPoints() : -1 : info.getCurrentPoints());
				writeD(info.getDiffPoints());
			}
			writeD(_loseTeam);
			writeD(_loserList.size());
			for (final var info : _loserList)
			{
				writeS(info.getName());
				writeS(info.getClanName());
				writeD(info.getClanId());
				writeD(info.getClassId());
				writeD(info.getDamage());
				writeD(Config.ALLOW_HIDE_OLY_POINTS ? _isLoser ? info.getCurrentPoints() : -1 : info.getCurrentPoints());
				writeD(info.getDiffPoints());
			}
		}
	}
}