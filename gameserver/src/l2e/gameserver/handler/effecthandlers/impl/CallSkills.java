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


import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class CallSkills extends Effect
{
	private final String _skills;
	private final List<Skill> _skillList;
	
	public CallSkills(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_skills = template.getParameters().getString("skills", null);
		if ((_skills != null) && !_skills.isEmpty())
		{
			_skillList = new ArrayList<>();
			for (final String ngtStack : _skills.split(";"))
			{
				final String[] ngt = ngtStack.split(",");
				final Skill skill = SkillsParser.getInstance().getInfo(Integer.parseInt(ngt[0]), Integer.parseInt(ngt[1]));
				if (skill != null)
				{
					_skillList.add(skill);
				}
			}
		}
		else
		{
			_skillList = null;
		}
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected() == null || getEffected().isDead() || _skillList == null || _skillList.isEmpty())
		{
			return false;
		}

		for (final Skill skill : _skillList)
		{
			if (skill != null)
			{
				skill.getEffects(getEffected(), getEffected(), false);
			}
		}
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}