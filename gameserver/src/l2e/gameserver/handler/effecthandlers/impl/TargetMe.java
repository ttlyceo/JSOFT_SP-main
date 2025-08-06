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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.SiegeSummonInstance;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class TargetMe extends Effect
{
	public TargetMe(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected().isPlayable())
		{
			if (getEffected() instanceof SiegeSummonInstance)
			{
				return false;
			}
			
			if (getEffected().getTarget() != getEffector())
			{
				final Player effector = getEffector().getActingPlayer();
				if ((effector == null) || effector.checkPvpSkill(getEffected(), getSkill()))
				{
					getEffected().setTarget(getEffector());
				}
			}
			((Playable) getEffected()).setLockedTarget(getEffector());
			return true;
		}
		else if (getEffected().isAttackable() && !getEffected().isRaid())
		{
			return true;
		}
		return false;
	}
	
	@Override
	public void onExit()
	{
		if (getEffected().isPlayable())
		{
			((Playable) getEffected()).setLockedTarget(null);
		}
	}
}