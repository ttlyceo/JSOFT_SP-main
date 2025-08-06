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
import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.Location;

/**
 * Created by LordWinter
 */
public class FightEventTeam implements Serializable
{
	private static final long serialVersionUID = 2265683963045484182L;

	public static enum TEAM_NAMES
	{
		Blue(0xb53e41), Red(0x162ee1), Green(0x3eb541), Yellow(0x2efdff), Gray(0x808080), Orange(0x0087f9), Black(0x161616), White(0xffffff), Violet(0xba2785), Cyan(0xe3e136), Pink(0xde6def);
		
		public int _nameColor;
		
		private TEAM_NAMES(int nameColor)
		{
			_nameColor = nameColor;
		}
	}
	private final int _index;
	private String _name;
	private final List<FightEventPlayer> _players = new ArrayList<>();
	private Location _spawnLoc;
	private int _score;
	
	public FightEventTeam(int index)
	{
		_index = index;
		chooseName();
	}
	
	public int getIndex()
	{
		return _index;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String chooseName()
	{
		_name = TEAM_NAMES.values()[_index - 1].toString();
		return _name;
	}
	
	public int getNickColor()
	{
		return TEAM_NAMES.values()[_index - 1]._nameColor;
	}
	
	public List<FightEventPlayer> getPlayers()
	{
		return _players;
	}
	
	public void addPlayer(FightEventPlayer player)
	{
		_players.add(player);
	}
	
	public void removePlayer(FightEventPlayer player)
	{
		_players.remove(player);
	}
	
	public void setSpawnLoc(Location loc)
	{
		_spawnLoc = loc;
	}
	
	public Location getSpawnLoc()
	{
		return _spawnLoc;
	}
	
	public void setScore(int newScore)
	{
		_score = newScore;
	}
	
	public void incScore(int by)
	{
		_score += by;
	}
	
	public int getScore()
	{
		return _score;
	}
}