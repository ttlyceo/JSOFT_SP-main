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

import l2e.commons.util.Util;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;

public class TransferHate extends Effect
{
	private final int _chance;

	public TransferHate(Env env, EffectTemplate template)
	{
		super(env, template);
		_chance = template.getParameters().getInteger("chance", 100);
	}

	@Override
	public boolean calcSuccess()
	{
		return Formulas.calcProbability(_chance, getEffector(), getEffected(), getSkill(), false);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}

	@Override
	public boolean onStart()
	{
		if (Util.checkIfInRange(getSkill().getEffectRange(), getEffector(), getEffected(), true))
		{
			for (final Creature obj : World.getInstance().getAroundCharacters(getEffector(), getSkill().getAffectRange(), 200))
			{
				if ((obj == null) || !obj.isAttackable() || obj.isDead())
				{
					continue;
				}

				final Attackable hater = ((Attackable) obj);
				final int hate = hater.getAggroList().getHating(getEffector());
				if (hate <= 0)
				{
					continue;
				}

				hater.getAggroList().reduceHate(getEffector(), -hate, false);
				hater.addDamageHate(getEffected(), 0, hate);
			}
			return true;
		}
		return false;
	}
}