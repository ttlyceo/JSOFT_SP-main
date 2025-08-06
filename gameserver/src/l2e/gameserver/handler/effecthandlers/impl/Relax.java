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

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class Relax extends Effect
{
	public Relax(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.RELAXING;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected().isPlayer())
		{
			getEffected().getActingPlayer().sitDown(false);
		}
		else
		{
			getEffected().getAI().setIntention(CtrlIntention.REST);
		}
		return super.onStart();
	}
	
	@Override
	public double getMpReduce()
	{
		return Math.floor(((calc() * getEffectTemplate().getTotalTickCount()) * Config.TOGGLE_MOD_MP));
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.RELAXING.getMask();
	}
	
	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
		{
			return false;
		}
		
		if (getEffected().isPlayer())
		{
			if (!getEffected().getActingPlayer().isSitting())
			{
				return false;
			}
		}
		return true;
	}
}