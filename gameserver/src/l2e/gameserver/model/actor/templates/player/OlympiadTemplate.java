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
package l2e.gameserver.model.actor.templates.player;

/**
 * Created by LordWinter
 */
public class OlympiadTemplate
{
	private final int _rank;
	private final String _name;
	private final long _points;
	private final int _win;
	private final int _lose;
	private final int _wr;
	
	public OlympiadTemplate(int rank, String name, long points, int win, int lose, int wr)
	{
		_rank = rank;
		_name = name;
		_points = points;
		_win = win;
		_lose = lose;
		_wr = wr;
	}
	
	public int getRank()
	{
		return _rank;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public long getPoints()
	{
		return _points;
	}
	
	public int getWin()
	{
		return _win;
	}
	
	public int getLose()
	{
		return _lose;
	}
	
	public int getWr()
	{
		return _wr;
	}
}