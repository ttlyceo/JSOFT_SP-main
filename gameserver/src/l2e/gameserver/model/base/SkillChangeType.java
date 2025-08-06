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

public enum SkillChangeType
{
	Power(0, true, false), CastTime(1, true, true), Reuse(2, true, true), Chance(3, true, false), SkillBlow(4, true, false), MCrit(5, true, false), PCrit(6, true, false);
	
	private final boolean _forceCheck;
	private final boolean _isOlyVsAll;
	private final int _changeId;
	
	public static final SkillChangeType[] VALUES = values();
	
	private SkillChangeType(int attackId, boolean ForceCheck, boolean IsOnlyVsAll)
	{
		_changeId = attackId;
		_forceCheck = ForceCheck;
		_isOlyVsAll = IsOnlyVsAll;
	}
	
	public boolean isForceCheck()
	{
		return _forceCheck;
	}
	
	public boolean isOnlyVsAll()
	{
		return _isOlyVsAll;
	}
	
	public int getId()
	{
		return _changeId;
	}
}