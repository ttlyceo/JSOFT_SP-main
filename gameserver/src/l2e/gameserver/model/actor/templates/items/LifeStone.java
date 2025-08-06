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

public class LifeStone
{
	private final int _grade;
	private final int _level;
	private final int _playerLevel;
	private final boolean _isWeaponAugment;
	private final boolean _isArmorAugment;
	
	public LifeStone(int grade, int level, int playerLevel, boolean isWeaponAugment, boolean isArmorAugment)
	{
		_grade = grade;
		_level = level;
		_playerLevel = playerLevel;
		_isWeaponAugment = isWeaponAugment;
		_isArmorAugment = isArmorAugment;
	}
	
	public final int getLevel()
	{
		return _level;
	}
	
	public final int getGrade()
	{
		return _grade;
	}
	
	public final int getPlayerLevel()
	{
		return _playerLevel;
	}
	
	public boolean isWeaponAugment()
	{
		return _isWeaponAugment;
	}
	
	public boolean isArmorAugment()
	{
		return _isArmorAugment;
	}
}