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
package l2e.gameserver.instancemanager.games;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import l2e.gameserver.data.holder.CharMiniGameHolder;
import l2e.gameserver.model.actor.Player;

public class MiniGameScoreManager
{
	public static class MiniGameScore
	{
		private final int _objectId;
		private final String _name;
		private int _score;

		public MiniGameScore(int objectId, String name, int score)
		{
			_objectId = objectId;
			_name = name;
			_score = score;
		}

		public int getObjectId()
		{
			return _objectId;
		}

		public String getName()
		{
			return _name;
		}

		public int getScore()
		{
			return _score;
		}

		public void setScore(int score)
		{
			_score = score;
		}

		@Override
		public boolean equals(Object o)
		{
			return !(o == null || o.getClass() != MiniGameScore.class) && ((MiniGameScore) o).getObjectId() == getObjectId();
		}
	}

	private final NavigableSet<MiniGameScore> _scores = new ConcurrentSkipListSet<>(new Comparator<MiniGameScore>()
	{
		@Override
		public int compare(MiniGameScore o1, MiniGameScore o2)
		{
			return o2.getScore() - o1.getScore();
		}
	});

	private MiniGameScoreManager()
	{
	}

	public void addScore(Player player, int score)
	{
		MiniGameScore miniGameScore = null;
		for (final MiniGameScore $miniGameScore : _scores)
		{
			if ($miniGameScore.getObjectId() == player.getObjectId())
			{
				miniGameScore = $miniGameScore;
			}
		}

		if (miniGameScore == null)
		{
			_scores.add(new MiniGameScore(player.getObjectId(), player.getName(null), score));
		}
		else
		{
			if (miniGameScore.getScore() > score)
			{
				return;
			}
			miniGameScore.setScore(score);
		}
		CharMiniGameHolder.getInstance().replace(player.getObjectId(), score);
	}

	public void addScore(int objectId, int score, String name)
	{
		_scores.add(new MiniGameScore(objectId, name, score));
	}

	public NavigableSet<MiniGameScore> getScores()
	{
		return _scores;
	}
	
	public static final MiniGameScoreManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final MiniGameScoreManager _instance = new MiniGameScoreManager();
	}
}