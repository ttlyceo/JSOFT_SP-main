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
package l2e.gameserver.model.actor.templates.quest;

public class QuestDropItem
{
	private final int _itemId;
	private final double _rate;
	private final long _minCount;
	private final long _maxCount;
	private final double _chance;
	private final boolean _rateable;
	
	public QuestDropItem(int itemId, double rate, long minCount, long maxCount, double chance, boolean rateable)
	{
		_itemId = itemId;
		_rate = rate;
		_minCount = minCount;
		_maxCount = maxCount;
		_chance = chance;
		_rateable = rateable;
	}
	
	public int getId()
	{
		return _itemId;
	}
	
	public double getRate()
	{
		return _rate;
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

	public boolean isRateable()
	{
		return _rateable;
	}
}