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
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.network.SystemMessageId;

public class Spoil extends Effect
{
	public Spoil(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.SPOIL;
	}
	
	@Override
	public boolean onStart()
	{
		if (!getEffected().isMonster() || getEffected().isDead())
		{
			getEffector().sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}

		final MonsterInstance target = (MonsterInstance) getEffected();
		if (target.isSpoil())
		{
			getEffector().sendPacket(SystemMessageId.ALREADY_SPOILED);
			return false;
		}

		if (Formulas.calcMagicSuccess(getEffector(), target, getSkill(), false))
		{
			target.setSpoil(true);
			target.setIsSpoiledBy(getEffector().getObjectId());
			getEffector().sendPacket(SystemMessageId.SPOIL_SUCCESS);
		}
		target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getEffector(), 0);
		return true;
	}
}