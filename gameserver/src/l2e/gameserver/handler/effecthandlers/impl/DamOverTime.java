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
import l2e.gameserver.network.SystemMessageId;

public class DamOverTime extends Effect
{
	private final boolean _canKill;
	private int _doubleTick;
	
	public DamOverTime(Env env, EffectTemplate template)
	{
		super(env, template);

		_canKill = template.getParameters().getBool("canKill", false);
		_doubleTick = 1;
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.DMG_OVER_TIME;
	}

	@Override
	public double getHpReduce()
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
		if (getEffected().isDead())
		{
			return false;
		}

		if (!getSkill().isToggle())
		{
			double damage = calc();
			if (getSkill().getId() == 4082 && getTickCount() > 3600)
			{
				damage = (damage * getTickCount() / 100) / 2 * _doubleTick;
				_doubleTick++;
			}
			
			if (damage >= (getEffected().getCurrentHp() - 1))
			{
				if (getSkill().isToggle())
				{
					getEffected().sendPacket(SystemMessageId.SKILL_REMOVED_DUE_LACK_HP);
					return false;
				}
				
				if (!_canKill)
				{
					if (getEffected().getCurrentHp() <= 1)
					{
						return true;
					}
					damage = getEffected().getCurrentHp() - 1;
				}
			}
			getEffected().reduceCurrentHpByDOT(damage, getEffector(), getSkill());
		}
		return getSkill().isToggle();
	}
}