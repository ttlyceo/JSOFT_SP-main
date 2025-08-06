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

import l2e.gameserver.model.entity.events.tournaments.Tournament;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.enums.TeamType;
import l2e.gameserver.model.olympiad.*;
import l2e.gameserver.network.ServerPacketOpcodes;

import java.util.ArrayList;
import java.util.List;

public abstract class ExReceiveOlympiadForTournamentList extends GameServerPacket
{
	@Override
	protected ServerPacketOpcodes getOpcodes()
	{
		return ServerPacketOpcodes.ExReceiveOlympiadList;
	}
	
	public static class OlympiadList extends ExReceiveOlympiadForTournamentList
	{
		List<Tournament> _games = new ArrayList<>();
		
		public OlympiadList()
		{
			Tournament task;
			for (int i = 0; i < TournamentData.getInstance().getTournaments().size(); i++)
			{
				task = TournamentData.getInstance().getTournaments().get(i);
				if (task != null)
				{
					if (!task.isPrepare() || task.isFinish())
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
			for (final Tournament game : _games)
			{
				if (game != null)
				{
					writeD(game.getInstanceId().getId());

					writeD(-1); //TEAMS

					writeD(game.isRunning() ? 0x02 : 0x01);
					writeS(game.getTeamOfType(TeamType.BLUE).getName());
					writeS(game.getTeamOfType(TeamType.RED).getName());
				}
			}
		}
	}
	
	public static class OlympiadResult extends ExReceiveOlympiadForTournamentList
	{
		private final boolean _tie;
		private int _winTeam;
		private int _loseTeam = 2;
		private final List<OlympiadInfo> _winnerList;
		private final List<OlympiadInfo> _loserList;
		
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
		}
		
		@Override
		protected void writeImpl()
		{
			writeD(0x01);
			writeD(_tie ? 0x01 : 0x00);
			writeS(_winnerList.get(0).getName());
			writeD(_winTeam);
			writeD(_winnerList.size());
			for (final OlympiadInfo info : _winnerList)
			{
				writeS(info.getName());
				writeS(info.getClanName());
				writeD(info.getClanId());
				writeD(info.getClassId());
				writeD(info.getDamage());
				writeD(info.getCurrentPoints());
				writeD(info.getDiffPoints());
			}
			writeD(_loseTeam);
			writeD(_loserList.size());
			for (final OlympiadInfo info : _loserList)
			{
				writeS(info.getName());
				writeS(info.getClanName());
				writeD(info.getClanId());
				writeD(info.getClassId());
				writeD(info.getDamage());
				writeD(info.getCurrentPoints());
				writeD(info.getDiffPoints());
			}
		}
	}
}