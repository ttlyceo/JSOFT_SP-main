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
package l2e.gameserver.model.service.buffer;

public class SchemeBuff
{
	private final int _skillId;
	private final int _skillLevel;
	private final int _premiumSkillLevel;
	private final boolean _isDanceSlot;
	
	public SchemeBuff(int skillId, int skillLevel, int premiumSkillLevel, boolean isDanceSlot)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
		_premiumSkillLevel = premiumSkillLevel;
		_isDanceSlot = isDanceSlot;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public int getLevel()
	{
		return _skillLevel;
	}
	
	public int getPremiumLevel()
	{
		return _premiumSkillLevel;
	}
	
	public boolean isDanceSlot()
	{
		return _isDanceSlot;
	}
}