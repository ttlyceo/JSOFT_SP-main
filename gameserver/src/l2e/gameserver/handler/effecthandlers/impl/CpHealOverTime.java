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

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class CpHealOverTime extends Effect
{
	public CpHealOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	public CpHealOverTime(Env env, Effect effect)
	{
		super(env, effect);
	}

	@Override
	public boolean canBeStolen()
	{
		return true;
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.CPHEAL_OVER_TIME;
	}

	@Override
	public boolean onActionTime()
	{
		final Creature target = getEffected();
		if (target == null)
		{
			return false;
		}
		
		if (target.isHealBlocked() || target.isDead())
		{
			return false;
		}

		double cp = getEffected().getCurrentCp();
		final double maxcp = getEffected().getMaxRecoverableCp();

		if (cp >= maxcp)
		{
			return false;
		}
		
		if (getSkill().isToggle())
		{
			cp += calc() * getEffectTemplate().getTotalTickCount();
		}
		else
		{
			cp += calc();
		}
		
		cp = Math.min(cp, maxcp);
		getEffected().setCurrentCp(cp);
		return getSkill().isToggle();
	}
}