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
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class SilentMove extends Effect
{
	public SilentMove(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	public SilentMove(Env env, Effect effect)
	{
		super(env, effect);
	}

	@Override
	public boolean canBeStolen()
	{
		return !getSkill().isToggle();
	}

	@Override
	public int getEffectFlags()
	{
		return EffectFlag.SILENT_MOVE.getMask();
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	@Override
	public double getMpReduce()
	{
		if (getEffected().isDead())
		{
			return 0;
		}
		return Math.floor(((calc() * getEffectTemplate().getTotalTickCount()) * 0.57));
	}
	
	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
		{
			return false;
		}
		return getSkill().isToggle();
	}
}