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

import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;

public class AbortCast extends Effect
{
	private final int _chance;
	
	public AbortCast(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_chance = template.getParameters().getInteger("chance", 100);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected() == null || getEffected() == getEffector())
		{
			return false;
		}

		if (getEffected().isRaid())
		{
			return false;
		}
		
		if (!Formulas.calcProbability(_chance, getEffector(), getEffected(), getSkill(), true))
		{
			return false;
		}
		getEffected().abortCast();
		return true;
	}
}