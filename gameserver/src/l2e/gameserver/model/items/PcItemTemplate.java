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
package l2e.gameserver.model.items;

import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.stats.StatsSet;

public final class PcItemTemplate extends ItemHolder
{
	private final boolean _equipped;
	private final int _enchant;
	private final int _augmentId;
	private final String _elementals;
	private final int _durability;
	
	public PcItemTemplate(StatsSet set)
	{
		super(set.getInteger("id"), set.getLong("count"));
		_equipped = set.getBool("equipped", false);
		_enchant = set.getInteger("enchant", 0);
		_augmentId = set.getInteger("augmentId", -1);
		_elementals = set.getString("elementals", null);
		_durability = set.getInteger("durability", 0);
	}
	
	public boolean isEquipped()
	{
		return _equipped;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
	
	public int getAugmentId()
	{
		return _augmentId;
	}
	
	public String getElementals()
	{
		return _elementals;
	}
	
	public int getDurability()
	{
		return _durability;
	}
}