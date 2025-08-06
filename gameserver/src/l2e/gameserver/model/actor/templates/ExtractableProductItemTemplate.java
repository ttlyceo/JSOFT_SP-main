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

import java.util.List;

import l2e.gameserver.model.holders.ItemHolder;

public class ExtractableProductItemTemplate
{
	private final List<ItemHolder> _items;
	private final double _chance;
	private final int _enchantLevel;
	
	public ExtractableProductItemTemplate(List<ItemHolder> items, double chance, int enchantLevel)
	{
		_items = items;
		_chance = chance;
		_enchantLevel = enchantLevel;
	}
	
	public List<ItemHolder> getItems()
	{
		return _items;
	}
	
	public double getChance()
	{
		return _chance;
	}
	
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
}