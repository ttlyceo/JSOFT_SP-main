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

import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter
 */
public class FightEventLastPlayerStats
{
	private final String _playerNickName;
	private final int _classId;
	private final String _clanName;
	private final String _allyName;
	private final String _typeName;
	private int _score;

	public FightEventLastPlayerStats(Player player, String typeName, int score)
	{
		_playerNickName = player.getName(null);
		_clanName = player.getClan() != null ? player.getClan().getName() : "<br>";
		_allyName = player.getClan() != null && player.getClan().getAllyId() > 0 ? player.getClan().getAllyName() : "<br>";
		_classId = player.getClassId().getId();
		_typeName = typeName;
		_score = score;
	}
	
	public FightEventLastPlayerStats(String playerName, String clanName, String allyName, int classId, int score)
	{
		_playerNickName = playerName;
		_clanName = clanName;
		_allyName = allyName;
		_classId = classId;
		_typeName = "Kill Player";
		_score = score;
	}

	public boolean isMyStat(Player player)
	{
		return _playerNickName.equals(player.getName(null));
	}

	public String getPlayerName()
	{
		return _playerNickName;
	}

	public String getClanName()
	{
		return _clanName;
	}

	public String getAllyName()
	{
		return _allyName;
	}

	public int getClassId()
	{
		return _classId;
	}

	public String getTypeName()
	{
		return _typeName;
	}

	public int getScore()
	{
		return _score;
	}

	public void setScore(int i)
	{
		_score = i;
	}
}