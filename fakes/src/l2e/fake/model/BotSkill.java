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

public abstract class BotSkill
{
	protected int _skillId;
	protected SpellUsageCondition _condition;
	protected int _conditionValue;
	protected int _priority;
	
	public BotSkill(int skillId, SpellUsageCondition condition, int conditionValue, int priority)
	{
		_skillId = skillId;
		_condition = condition;
		_conditionValue = conditionValue;
	}

	public BotSkill(int skillId)
	{
		_skillId = skillId;
		_condition = SpellUsageCondition.NONE;
		_conditionValue = 0;
		_priority = 0;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}

	public SpellUsageCondition getCondition()
	{
		return _condition;
	}
	
	public int getConditionValue()
	{
		return _conditionValue;
	}
	
	public int getPriority()
	{
		return _priority;
	}
}