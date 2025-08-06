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
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class Petrification extends Effect
{
	public Petrification(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.PETRIFICATION;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected().isRaid() || getEffected().isRaidMinion())
		{
			return false;
		}
		getEffected().startAbnormalEffect(AbnormalEffect.HOLD_2);
		getEffected().getAI().setIntention(CtrlIntention.IDLE, getEffector());
		getEffected().startParalyze();
		getEffected().setIsDamageBlock(true);
		return super.onStart();
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(AbnormalEffect.HOLD_2);
		getEffected().setIsDamageBlock(false);
		if (!getEffected().isPlayer())
		{
			getEffected().getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
	}

	@Override
	public int getEffectFlags()
	{
		return EffectFlag.PARALYZED.getMask() | EffectFlag.INVUL.getMask();
	}
}