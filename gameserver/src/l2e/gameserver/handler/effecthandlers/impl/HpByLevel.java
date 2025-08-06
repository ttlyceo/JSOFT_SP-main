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
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class HpByLevel extends Effect
{
	public HpByLevel(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean onStart()
	{
		if ((getEffector() == null) || (getEffected() == null))
		{
			return false;
		}
		
		if (getEffector().isHealBlocked())
		{
			return false;
		}
		
		final double abs = calc();
		final double absorb = ((getEffector().getCurrentHp() + abs) > getEffector().getMaxHp() ? getEffector().getMaxHp() : (getEffector().getCurrentHp() + abs));
		final int restored = (int) (absorb - getEffector().getCurrentHp());
		getEffector().setCurrentHp(absorb);
		
		final var su = getEffector().makeStatusUpdate(StatusUpdate.CUR_HP);
		getEffector().sendPacket(su);
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HP_RESTORED);
		sm.addNumber(restored);
		getEffector().sendPacket(sm);
		return true;
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}
}