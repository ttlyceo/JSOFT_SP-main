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
package l2e.gameserver.model.actor.templates;

public class HistoryInfoTemplate
{
	private final int _raceId;
	private int _first;
	private int _second;
	private double _oddRate;
	
	public HistoryInfoTemplate(int raceId, int first, int second, double oddRate)
	{
		_raceId = raceId;
		_first = first;
		_second = second;
		_oddRate = oddRate;
	}
	
	public int getRaceId()
	{
		return _raceId;
	}
	
	public int getFirst()
	{
		return _first;
	}
	
	public int getSecond()
	{
		return _second;
	}
	
	public double getOddRate()
	{
		return _oddRate;
	}
	
	public void setFirst(int first)
	{
		_first = first;
	}
	
	public void setSecond(int second)
	{
		_second = second;
	}
	
	public void setOddRate(double oddRate)
	{
		_oddRate = oddRate;
	}
}