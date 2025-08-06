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

public class PetSkillLearnHolder extends SkillHolder
{
	private final int _minLevel;
	private final double _hpPercent;

	public PetSkillLearnHolder(int id, int lvl, int minLvl, double hpPercent)
	{
		super(id, lvl);
		
		_minLevel = minLvl;
		_hpPercent = hpPercent;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public double getHpPercent()
	{
		return _hpPercent;
	}
}