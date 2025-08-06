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
package l2e.gameserver.model.holders;

public class RangeChanceHolder
{
	private final int _min;
	private final int _max;
	private final double _chance;
	private final boolean _enchantAnnounce;
	
	public RangeChanceHolder(int min, int max, double chance, boolean enchantAnnounce)
	{
		_min = min;
		_max = max;
		_chance = chance;
		_enchantAnnounce = enchantAnnounce;
	}
	
	public int getMin()
	{
		return _min;
	}

	public int getMax()
	{
		return _max;
	}
	
	public double getChance()
	{
		return _chance;
	}
	
	public boolean isEnchantAnnounce()
	{
		return _enchantAnnounce;
	}
}