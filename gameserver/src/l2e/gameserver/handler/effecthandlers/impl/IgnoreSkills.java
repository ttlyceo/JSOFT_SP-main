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
package l2e.gameserver.handler.effecthandlers.impl;

import static l2e.gameserver.data.parser.SkillsParser.getId;
import static l2e.gameserver.data.parser.SkillsParser.getLvl;
import static l2e.gameserver.data.parser.SkillsParser.getSkillHashCode;

import l2e.commons.util.ExArrays;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public final class IgnoreSkills extends Effect
{
	private final int[] _skills;

	public IgnoreSkills(Env env, EffectTemplate template)
	{
		super(env, template);

		int[] skills = null;
		for (int i = 1;; i++)
		{
			final int skillId = template.getParameters().getInteger("skillId" + i, 0);
			final int skillLvl = template.getParameters().getInteger("skillLvl" + i, 0);
			if (skillId == 0)
			{
				break;
			}
			skills = ExArrays.push(skills, getSkillHashCode(skillId, skillLvl));
		}

		if (skills == null)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": Without parameters!");
		}
		_skills = skills;
	}

	@Override
	public boolean onStart()
	{
		final Creature effected = getEffected();
		for (final int skillHashCode : _skills)
		{
			effected.addInvulAgainst(getId(skillHashCode), getLvl(skillHashCode));
		}
		return true;
	}

	@Override
	public void onExit()
	{
		final Creature effected = getEffected();
		for (final int skillHashCode : _skills)
		{
			effected.removeInvulAgainst(getId(skillHashCode), getLvl(skillHashCode));
		}
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}
}
