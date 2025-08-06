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
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class RandomizeHate extends Effect
{
	public RandomizeHate(Env env, EffectTemplate template)
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
		if ((getEffected() == null) || (getEffected() == getEffector()) || !getEffected().isAttackable())
		{
			return false;
		}

		final Attackable effectedMob = (Attackable) getEffected();
		final List<Creature> targetList = new ArrayList<>();
		for (final Creature cha : World.getInstance().getAroundCharacters(getEffected()))
		{
			if ((cha != null) && (cha != effectedMob) && (cha != getEffector()))
			{
				if (cha.isAttackable() && (!((Attackable) cha).getFaction().isNone()) && ((Attackable) cha).isInFaction(effectedMob))
				{
					continue;
				}

				targetList.add(cha);
			}
		}
		
		if (targetList.isEmpty())
		{
			return true;
		}

		final Creature target = targetList.get(Rnd.get(targetList.size()));
		final int hate = effectedMob.getAggroList().getHating(getEffector());
		effectedMob.getAggroList().stopHating(getEffector());
		effectedMob.addDamageHate(target, 0, hate);

		return true;
	}
}