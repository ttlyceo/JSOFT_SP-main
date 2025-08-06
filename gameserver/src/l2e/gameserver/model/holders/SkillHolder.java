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

import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.skills.Skill;

public class SkillHolder
{
	private final int _skillId;
	private final int _skillLvl;

	public SkillHolder(int skillId, int skillLvl)
	{
		_skillId = skillId;
		_skillLvl = skillLvl;
	}

	public SkillHolder(Skill skill)
	{
		_skillId = skill.getId();
		_skillLvl = skill.getLevel();
	}
	
	public final int getId()
	{
		return _skillId;
	}

	public final int getLvl()
	{
		return _skillLvl;
	}

	public final Skill getSkill()
	{
		return SkillsParser.getInstance().getInfo(_skillId, _skillLvl);
	}

	@Override
	public String toString()
	{
		return "[SkillId: " + _skillId + " Level: " + _skillLvl + "]";
	}
}