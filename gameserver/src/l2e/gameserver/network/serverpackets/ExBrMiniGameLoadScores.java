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
import java.util.NavigableSet;

import l2e.gameserver.instancemanager.games.MiniGameScoreManager;
import l2e.gameserver.model.actor.Player;

public class ExBrMiniGameLoadScores extends GameServerPacket
{
	private int _place;
	private int _score;
	private int _lastScore;
	
	private final List<MiniGameScoreManager.MiniGameScore> _entries;
	
	public ExBrMiniGameLoadScores(Player player)
	{
		int i = 1;
		
		final NavigableSet<MiniGameScoreManager.MiniGameScore> score = MiniGameScoreManager.getInstance().getScores();
		_entries = new ArrayList<MiniGameScoreManager.MiniGameScore>(score.size() >= 100 ? 100 : score.size());
		
		final MiniGameScoreManager.MiniGameScore last = score.isEmpty() ? null : score.last();
		if (last != null)
		{
			_lastScore = last.getScore();
		}
		
		for (final MiniGameScoreManager.MiniGameScore entry : score)
		{
			if (i > 100)
			{
				break;
			}
			
			if (entry.getObjectId() == player.getObjectId())
			{
				_place = i;
				_score = entry.getScore();
			}
			_entries.add(entry);
			i++;
		}
	}

	@Override
	protected void writeImpl()
	{
		writeD(_place);
		writeD(_score);
		writeD(_entries.size());
		writeD(_lastScore);
		for (int i = 0; i < _entries.size(); i++)
		{
			final MiniGameScoreManager.MiniGameScore pair = _entries.get(i);
			writeD(i + 1);
			writeS(pair.getName());
			writeD(pair.getScore());
		}
	}
}