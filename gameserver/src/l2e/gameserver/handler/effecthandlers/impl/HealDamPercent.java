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

public class HealDamPercent extends Effect
{
	public HealDamPercent(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.HPDAMPERCENT;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected().isPlayer() && getEffected().isFakeDeathNow())
		{
			getEffected().stopFakeDeath(true);
		}
		
		final int damage = (int) ((getEffected().getCurrentHp() * calc()) / 100);
		if (!getEffected().isRaid() && Formulas.calcAtkBreak(getEffected(), false))
		{
			getEffected().breakAttack();
			getEffected().breakCast();
		}
		
		if (damage > 0)
		{
			getEffected().setCurrentHp(getEffected().getCurrentHp() - damage);
			if (getEffected() != getEffector())
			{
				getEffector().sendDamageMessage(getEffected(), damage, getSkill(), false, false, false);
			}
		}
		Formulas.calcDamageReflected(getEffector(), getEffected(), getSkill(), damage, false);
		return true;
	}
}