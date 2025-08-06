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
package l2e.fake.model;

public class OffensiveSpell extends BotSkill
{
	public OffensiveSpell(int skillId, SpellUsageCondition condition, int conditionValue, int priority)
	{
		super(skillId, condition, conditionValue, priority);
	}
	
	public OffensiveSpell(int skillId, int priority)
	{
		super(skillId, SpellUsageCondition.NONE, 0, priority);
	}
	
	public OffensiveSpell(int skillId)
	{
		super(skillId);
	}
}