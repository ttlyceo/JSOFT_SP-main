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

import l2e.commons.util.PositionUtils;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.actor.instance.DefenderInstance;
import l2e.gameserver.model.actor.instance.FortCommanderInstance;
import l2e.gameserver.model.actor.instance.NpcInstance;
import l2e.gameserver.model.actor.instance.SiegeFlagInstance;
import l2e.gameserver.model.actor.instance.SiegeSummonInstance;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class Fear extends Effect
{
	private final int _range;
	
	public Fear(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_range = template.getParameters().getInteger("range", 500);
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.FEAR.getMask();
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.FEAR;
	}
	
	@Override
	public boolean onStart()
	{
		final var effected = getEffected();
		if (effected == null || effected.isRaid() || effected.isEpicRaid() || effected.isAfraid() || (effected instanceof NpcInstance) || (effected instanceof DefenderInstance) || (effected instanceof FortCommanderInstance) || (effected instanceof SiegeFlagInstance) || (effected instanceof SiegeSummonInstance))
		{
			return false;
		}

		effected.startFear();
		effectRunTask();
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopFear(false);
		getEffected().sendActionFailed();
		super.onExit();
	}
	
	private void effectRunTask()
	{
		final double angle = Math.toRadians(PositionUtils.calculateAngleFrom(getEffector(), getEffected()));
		final int oldX = getEffected().getX();
		final int oldY = getEffected().getY();
		final int x = oldX + (int) (_range * Math.cos(angle));
		final int y = oldY + (int) (_range * Math.sin(angle));
		if (!getEffected().isPet() && !getEffected().isRunning())
		{
			getEffected().setRunning();
		}
		getEffected().getAI().setIntention(CtrlIntention.MOVING, GeoEngine.getInstance().moveCheck(getEffected(), getEffected().getX(), getEffected().getY(), getEffected().getZ(), x, y, getEffected().getZ(), getEffected().getReflection()), 0);
	}
}