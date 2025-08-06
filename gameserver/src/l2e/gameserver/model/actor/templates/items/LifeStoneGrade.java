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
package l2e.gameserver.model.actor.templates.items;

public class LifeStoneGrade
{
	private final int _grade;
	private final int _skillChance;
	private final int _growChance;
	private final int[] _retailChance;
	
	public LifeStoneGrade(int grade, int skillChance, int growChance, int[] retailChance)
	{
		_grade = grade;
		_skillChance = skillChance;
		_growChance = growChance;
		_retailChance = retailChance;
	}
	
	public final int getGrade()
	{
		return _grade;
	}
	
	public final int getSkillChance()
	{
		return _skillChance;
	}
	
	public final int getGrowChance()
	{
		return _growChance;
	}
	
	public final int[] getRetailChance()
	{
		return _retailChance;
	}
}