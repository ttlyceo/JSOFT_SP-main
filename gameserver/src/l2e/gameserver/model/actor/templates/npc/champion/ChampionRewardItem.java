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
package l2e.gameserver.model.actor.templates.npc.champion;

public class ChampionRewardItem
{
	private final int _itemId;
	private final long _minCount;
	private final long _maxCount;
	private final double _dropChance;
	
	public ChampionRewardItem(int itemId, long minCount, long maxCount, double dropChance)
	{
		_itemId = itemId;
		_minCount = minCount;
		_maxCount = maxCount;
		_dropChance = dropChance;
	}
	
	public int getItemId()
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

	public double getDropChance()
	{
		return _dropChance;
	}
}