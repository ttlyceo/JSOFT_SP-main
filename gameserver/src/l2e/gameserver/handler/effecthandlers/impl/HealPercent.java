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
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class HealPercent extends Effect
{
	public HealPercent(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HEAL_PERCENT;
	}
	
	@Override
	public boolean onStart()
	{
		final Creature target = getEffected();
		if (target == null || target.isDead() || target.isHealBlocked() || target.isInvul())
		{
			return false;
		}
		
		double amount = 0;
		final double power = calc();
		final boolean full = (power == 100.0);
		
		amount = full ? target.getMaxHp() : (target.getMaxHp() * power) / 100.0;
		amount = Math.max(Math.min(amount, target.getMaxRecoverableHp() - target.getCurrentHp()), 0);
		if (amount != 0)
		{
			target.setCurrentHp(amount + target.getCurrentHp());
			final var su = target.makeStatusUpdate(StatusUpdate.CUR_HP);
			target.sendPacket(su);
		}
		SystemMessage sm;
		if (getEffector().getObjectId() != target.getObjectId())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HP_RESTORED_BY_C1);
			sm.addCharName(getEffector());
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HP_RESTORED);
		}
		sm.addNumber((int) amount);
		target.sendPacket(sm);
		return true;
	}
}