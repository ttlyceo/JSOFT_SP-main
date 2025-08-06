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

import java.util.List;

import l2e.gameserver.model.Location;

public class FakeLocTemplate
{
	protected int _id;
	protected int _amount;
	protected Location _loc;
	protected int _minLvl;
	protected int _maxLvl;
	protected List<Integer> _classes = null;
	protected int _distance;
	private int _currectAmount;

	public FakeLocTemplate(int id, int amount, Location loc, List<Integer> classes, int minLvl, int maxLvl, int distance)
	{
		_id = id;
		_amount = amount;
		_loc = loc;
		_classes = classes;
		_minLvl = minLvl;
		_maxLvl = maxLvl;
		_distance = distance;
		_currectAmount = 0;
	}

	public int getId()
	{
		return _id;
	}
	
	public int getAmount()
	{
		return _amount;
	}
	
	public Location getLocation()
	{
		return _loc;
	}
	
	public List<Integer> getClasses()
	{
		return _classes;
	}
	
	public int getMinLvl()
	{
		return _minLvl;
	}
	
	public int getMaxLvl()
	{
		return _maxLvl;
	}
	
	public void setCurrentAmount(int val)
	{
		_currectAmount = val;
	}
	
	public int getCurrentAmount()
	{
		return _currectAmount;
	}
	
	public int getDistance()
	{
		return _distance;
	}
}