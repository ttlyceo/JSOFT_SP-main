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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class FakeDeath extends Effect
{
	private final int _rate;
	
	public FakeDeath(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_rate = template.getParameters().getInteger("rate", 0);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.FAKE_DEATH;
	}

	@Override
	public boolean onStart()
	{
		getEffected().startFakeDeath();
		if (getEffected().isPlayer() && (Rnd.get(100) <= _rate))
		{
			getEffected().getActingPlayer().setIsFakeDeath(true);
		}
		return true;
	}

	@Override
	public void onExit()
	{
		getEffected().stopFakeDeath(true);
	}
	
	@Override
	public double getMpReduce()
	{
		if (getEffected().isDead())
		{
			return 0;
		}
		return Math.floor((calc() * getEffectTemplate().getTotalTickCount()) / 2);
	}
	
	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
		{
			return false;
		}
		return true;
	}
}