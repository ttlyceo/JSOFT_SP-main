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

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class Confusion extends Effect
{
	public Confusion(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public int getEffectFlags()
	{
		return EffectFlag.CONFUSED.getMask();
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}

	private Creature getRndTarget()
	{
		final List<Creature> targetList = new ArrayList<>();
		for (final Creature obj : World.getInstance().getAroundCharacters(getEffected(), 2000, 400))
		{
			if (((getEffected().isMonster() && obj.isAttackable()) || (obj.isCreature())) && (obj != getEffected()) && obj != getEffector())
			{
				targetList.add(obj);
			}
		}
		if (!targetList.isEmpty())
		{
			return targetList.get(Rnd.nextInt(targetList.size()));
		}
		return null;
	}
	
	@Override
	public boolean onStart()
	{
		getEffected().startConfused();
		final Creature target = getRndTarget();
		if (target != null)
		{
			if (getEffected().isMonster())
			{
				((Attackable) getEffected()).addDamageHate(target, 1, 99999);
			}
			getEffected().setTarget(target);
			getEffected().getAI().setIntention(CtrlIntention.ATTACK, target);
		}
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopConfused();
		if (getEffected().isMonster() && getEffected().getTarget() != null && getEffected().getTarget().isMonster())
		{
			((Attackable) getEffected()).getAggroList().stopHating((Creature) getEffected().getTarget());
			getEffected().breakAttack();
			getEffected().abortCast();
			((Attackable) getEffected().getTarget()).getAggroList().stopHating(getEffected());
			((Attackable) getEffected().getTarget()).breakAttack();
			((Attackable) getEffected().getTarget()).abortCast();
		}
	}
}