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
package l2e.gameserver.model.entity.events.model.template;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter
 */
public class FightEventPlayer implements Serializable
{
	private static final long serialVersionUID = 5788010638258042361L;
	private Player _player;
	private FightEventTeam _team;
	private Party _myParty = null;

	private int _score;
	private int _playerKills;
	private int _partPoints;
	private final Map<String, Integer> _otherCreaturesScores = new ConcurrentHashMap<>();
	private int _deaths;
	private double _damage;
	private long _lastDamageTime;
	private boolean _invisible = false;
	private boolean _isShowRank = false;
	private boolean _isShowTutorial = false;
	private int _secondsOutsideZone = 0;

	private boolean _afk = false;
	private long _afkStartTime = 0;
	private int _totalAfkSeconds = 0;

	public FightEventPlayer(Player player)
	{
		_player = player;
	}

	public void setPlayer(Player player)
	{
		_player = player;
	}

	public Player getPlayer()
	{
		return _player;
	}

	public void setTeam(FightEventTeam team)
	{
		_team = team;
	}

	public FightEventTeam getTeam()
	{
		return _team;
	}

	public Party getParty()
	{
		return _myParty;
	}

	public void setParty(Party party)
	{
		_myParty = party;
	}

	public void increaseScore(int byHowMany)
	{
		_score += byHowMany;
	}

	public void decreaseScore(int byHowMany)
	{
		_score -= byHowMany;
	}

	public void setScore(int value)
	{
		_score = value;
	}

	public int getScore()
	{
		return _score;
	}

	public void increaseKills()
	{
		_playerKills++;
	}

	public void setKills(int value)
	{
		_playerKills = value;
	}

	public int getKills()
	{
		return _playerKills;
	}

	public void increaseEventSpecificScore(String scoreKey)
	{
		if (!_otherCreaturesScores.containsKey(scoreKey))
		{
			_otherCreaturesScores.put(scoreKey, 0);
		}

		final int value = _otherCreaturesScores.get(scoreKey);

		_otherCreaturesScores.put(scoreKey, value + 1);
	}

	public void setEventSpecificScore(String scoreKey, int value)
	{
		_otherCreaturesScores.put(scoreKey, value);
	}

	public int getEventSpecificScore(String scoreKey)
	{
		if (!_otherCreaturesScores.containsKey(scoreKey))
		{
			return 0;
		}
		return _otherCreaturesScores.get(scoreKey);
	}

	public void increaseDeaths()
	{
		_deaths++;
	}

	public void setDeaths(int value)
	{
		_deaths = value;
	}

	public int getDeaths()
	{
		return _deaths;
	}

	public void increaseDamage(double damage)
	{
		_damage += damage;

		setLastDamageTime();
	}

	public void setDamage(double damage)
	{
		_damage = damage;

		if (damage == 0)
		{
			_lastDamageTime = 0;
		}
	}

	public double getDamage()
	{
		return _damage;
	}

	public void setLastDamageTime()
	{
		_lastDamageTime = System.currentTimeMillis();
	}

	public long getLastDamageTime()
	{
		return _lastDamageTime;
	}

	public void setInvisible(boolean val)
	{
		_invisible = val;
	}

	public boolean isInvisible()
	{
		return _invisible;
	}

	public void setAfk(boolean val)
	{
		_afk = val;
	}

	public boolean isAfk()
	{
		return _afk;
	}

	public void setAfkStartTime(long startTime)
	{
		_afkStartTime = startTime;
	}

	public long getAfkStartTime()
	{
		return _afkStartTime;
	}

	public void addTotalAfkSeconds(int secsAfk)
	{
		_totalAfkSeconds += secsAfk;
	}

	public int getTotalAfkSeconds()
	{
		return _totalAfkSeconds;
	}

	public void setShowRank(boolean b)
	{
		_isShowRank = b;
	}

	public boolean isShowRank()
	{
		return _isShowRank;
	}

	public void setShowTutorial(boolean b)
	{
		_isShowTutorial = b;
	}

	public boolean isShowTutorial()
	{
		return _isShowTutorial;
	}

	public void increaseSecondsOutsideZone()
	{
		_secondsOutsideZone++;
	}

	public int getSecondsOutsideZone()
	{
		return _secondsOutsideZone;
	}

	public void clearSecondsOutsideZone()
	{
		_secondsOutsideZone = 0;
	}
	
	public void increasePartPoints()
	{
		_partPoints++;
	}
	
	public int getPartPoints()
	{
		return _partPoints;
	}
}