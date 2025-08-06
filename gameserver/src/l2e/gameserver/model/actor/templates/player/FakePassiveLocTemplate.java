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

import l2e.gameserver.model.spawn.SpawnTerritory;

public class FakePassiveLocTemplate
{
	private final int _id;
	private final int _amount;
	private final SpawnTerritory _territory;
	private final int _minLvl;
	private final int _maxLvl;
	private List<Integer> _classes = null;
	private final long _minDelay;
	private final long _maxDelay;
	private final long _minRespawn;
	private final long _maxRepsawn;
	private int _currectAmount;

	public FakePassiveLocTemplate(int id, int amount, SpawnTerritory territory, List<Integer> classes, int minLvl, int maxLvl, long minDelay, long maxDelay, long minRespawn, long maxRepsawn)
	{
		_id = id;
		_amount = amount;
		_territory = territory;
		_classes = classes;
		_minLvl = minLvl;
		_maxLvl = maxLvl;
		_minDelay = minDelay;
		_maxDelay = maxDelay;
		_minRespawn = minRespawn;
		_maxRepsawn = maxRepsawn;
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
	
	public SpawnTerritory getTerritory()
	{
		return _territory;
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
	
	public long getMinDelay()
	{
		return _minDelay;
	}
	
	public long getMaxDelay()
	{
		return _maxDelay;
	}
	
	public long getMinRespawn()
	{
		return _minRespawn;
	}
	
	public long getMaxRespawn()
	{
		return _maxRepsawn;
	}
}