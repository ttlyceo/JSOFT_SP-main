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

public class ExtractableProductTemplate
{
	private final int _id;
	private final long _min;
	private final long _max;
	private final double _chance;
	private final int _enchant;
	
	public ExtractableProductTemplate(int id, long min, long max, double chance, int enchant)
	{
		_id = id;
		_min = min;
		_max = max;
		_chance = chance * 1000;
		_enchant = enchant;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public long getMin()
	{
		return _min;
	}
	
	public long getMax()
	{
		return _max;
	}
	
	public double getChance()
	{
		return _chance;
	}
	
	public int getEnchantLevel()
	{
		return _enchant;
	}
}