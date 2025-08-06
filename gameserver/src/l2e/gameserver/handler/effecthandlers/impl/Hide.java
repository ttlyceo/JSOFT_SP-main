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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class Hide extends Effect
{
	public Hide(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	public Hide(Env env, Effect effect)
	{
		super(env, effect);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HIDE;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected().isPlayer())
		{
			final Player activeChar = getEffected().getActingPlayer();
			activeChar.setInvisible(true);
			activeChar.startAbnormalEffect(AbnormalEffect.STEALTH);

			if (((activeChar.getAI().getNextIntention() != null)) && (activeChar.getAI().getNextIntention().getCtrlIntention() == CtrlIntention.ATTACK))
			{
				activeChar.getAI().setIntention(CtrlIntention.IDLE);
			}
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if (getEffected().isPlayer())
		{
			final Player activeChar = getEffected().getActingPlayer();
			if (!activeChar.inObserverMode())
			{
				activeChar.setInvisible(false);
			}
			activeChar.stopAbnormalEffect(AbnormalEffect.STEALTH);
		}
	}
}