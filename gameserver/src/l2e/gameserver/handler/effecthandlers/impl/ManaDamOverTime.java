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

public class ManaDamOverTime extends Effect
{
	public ManaDamOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.MANA_DMG_OVER_TIME;
	}
	
	@Override
	public double getMpReduce()
	{
		if (getEffected().isDead())
		{
			return 0;
		}
		return Math.floor(((calc() * getEffectTemplate().getTotalTickCount()) * 0.56));
	}

	@Override
	public boolean onActionTime()
	{
		final var target = getEffected();
		if (target == null)
		{
			return false;
		}
		
		final var isToggle = getSkill().isToggle();
		
		if ((target.isHealBlocked() && !isToggle) || target.isDead())
		{
			return false;
		}
		
		if (!isToggle)
		{
			final double manaDam = calc();
			getEffected().reduceCurrentMp(manaDam);
		}
		return isToggle;
	}
}