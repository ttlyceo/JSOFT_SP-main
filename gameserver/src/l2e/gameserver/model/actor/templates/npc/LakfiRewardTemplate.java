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
package l2e.gameserver.model.actor.templates.npc;

/**
 * Created by LordWinter 12.09.2020
 */
public class LakfiRewardTemplate
{
	private final int _itemId;
	private final long _minCount;
	private final long _maxCount;
	private final double _chance;
	
	public LakfiRewardTemplate(int itemId, long minCount, long maxCount, double chance)
	{
		_itemId = itemId;
		_minCount = minCount;
		_maxCount = maxCount;
		_chance = chance;
	}
	
	public int getId()
	{
		return _itemId;
	}
	
	public long getMinCount()
	{
		return _minCount;
	}

	public long getMaxCount()
	{
		return _maxCount;
	}
	
	public double getChance()
	{
		return _chance;
	}
}