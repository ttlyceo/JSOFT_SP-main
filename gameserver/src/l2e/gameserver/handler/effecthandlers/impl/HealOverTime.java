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
import l2e.gameserver.network.serverpackets.ExRegenMax;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public class HealOverTime extends Effect
{
	public HealOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	public HealOverTime(Env env, Effect effect)
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
		return EffectType.HEAL_OVER_TIME;
	}

	@Override
	public boolean onStart()
	{
		final Creature target = getEffected();

		if (target == null || target.isDead() || target.isHealBlocked() || target.isInvul())
		{
			return false;
		}

		if (getEffected().isPlayer() && (getEffectTemplate().getTotalTickCount() > 0))
		{
			getEffected().sendPacket(new ExRegenMax(calc(), getEffectTemplate().getTotalTickCount() * getEffectTemplate().getAbnormalTime(), getEffectTemplate().getTotalTickCount()));
		}
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead() || getEffected().isDoor())
		{
			return false;
		}

		double hp = getEffected().getCurrentHp();
		final double maxhp = getEffected().getMaxRecoverableHp();

		if (hp >= maxhp)
		{
			return false;
		}

		if (getSkill().isToggle())
		{
			hp += calc() * getEffectTemplate().getTotalTickCount();
		}
		else
		{
			hp += calc();
		}

		hp = Math.min(hp, maxhp);

		getEffected().setCurrentHp(hp);
		final var su = getEffected().makeStatusUpdate(StatusUpdate.CUR_HP);
		getEffected().sendPacket(su);

		return getSkill().isToggle();
	}
}