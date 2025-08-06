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
package l2e.gameserver.model.base;

public enum AttackType
{
	Normal(0, false), Magic(1, false), Crit(2, false), MCrit(3, false), Blow(4, false), PSkillDamage(5, false), PSkillCritical(6, false);
	
	private int _attackId;
	public static final AttackType[] VALUES = values();
	private final boolean _isOlyVsAll;
	
	private AttackType(int attackId, boolean IsOnlyVsAll)
	{
		_attackId = attackId;
		_isOlyVsAll = IsOnlyVsAll;
	}
	
	public int getId()
	{
		return _attackId;
	}
	
	public boolean isOnlyVsAll()
	{
		return _isOlyVsAll;
	}
}