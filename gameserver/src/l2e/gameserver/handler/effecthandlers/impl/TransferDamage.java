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

import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class TransferDamage extends Effect
{
	public TransferDamage(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	public TransferDamage(Env env, Effect effect)
	{
		super(env, effect);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.DAMAGE_TRANSFER;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected().isPlayable() && getEffector().isPlayer())
		{
			((Playable) getEffected()).setTransferDamageTo(getEffector().getActingPlayer());
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if (getEffected().isPlayable() && getEffector().isPlayer())
		{
			((Playable) getEffected()).setTransferDamageTo(null);
		}
	}
}