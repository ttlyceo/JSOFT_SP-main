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

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class Sleep extends Effect
{
	public Sleep(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public int getEffectFlags()
	{
		return EffectFlag.SLEEP.getMask();
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.SLEEP;
	}
	
	@Override
	public boolean onStart()
	{
		getEffected().abortAttack();
		getEffected().abortCast();
		getEffected().stopMove(null);
		getEffected().getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		return super.onStart();
	}
	
	@Override
	public void onExit()
	{
		if (getEffected().isPlayer())
		{
			getEffected().getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
		}
		else
		{
			getEffected().getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		super.onExit();
	}
}