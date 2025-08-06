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

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class UnAggro extends Effect
{
	public UnAggro(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.UNAGGRO;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected() == null)
		{
			return false;
		}
		if (getEffected().isNpc())
		{
			((Npc) getEffected()).setUnAggred(true);
		}
		return super.onStart();
	}
	
	@Override
	public void onExit()
	{
		if (getEffected() == null)
		{
			return;
		}
		
		if (getEffected().isNpc())
		{
			((Npc) getEffected()).setUnAggred(false);
		}
		super.onExit();
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}