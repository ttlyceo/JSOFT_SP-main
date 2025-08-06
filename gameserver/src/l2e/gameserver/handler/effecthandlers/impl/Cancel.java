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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.instance.player.impl.BuffsBackTask;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;

public class Cancel extends Effect
{
	private final String _slot;
	private final int _rate;
	private final int _min;
	private final int _max;
	private final boolean _randomEffects;
	private final boolean _checkResists;
	
	public Cancel(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_slot = template.getParameters().getString("slot", null);
		_rate = template.getParameters().getInteger("rate", 0);
		_min = template.getParameters().getInteger("min", 0);
		_max = template.getParameters().getInteger("max", 0);
		_randomEffects = template.getParameters().getBool("randomEffects", false);
		_checkResists = template.getParameters().getBool("checkResists", false);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.CANCEL;
	}
	
	@Override
	public boolean onStart()
	{
		final var effector = getEffector();
		final var effected = getEffected();
		if (effector == null || effected == null || effected.isDead())
		{
			return false;
		}
		
		final boolean isBuffSlot = _slot.equalsIgnoreCase("buff");
		
		final var canceled = Formulas.calcCancelStealEffects(effector, effected, getSkill(), _slot, _rate, _min, _max, _randomEffects, _checkResists, false);
		if (canceled.isEmpty())
		{
			return false;
		}
		
		final List<Effect> effects = new ArrayList<>(canceled.size());
		final Map<Skill, Effect> skillIds = new ConcurrentHashMap<>();
		for (final var eff : canceled)
		{
			final var skill = eff.getSkill();
			if (!skillIds.containsKey(skill))
			{
				skillIds.put(skill, eff);
			}
		}
		
		for (final var stats : skillIds.entrySet())
		{
			final var skill = stats.getKey();
			final var effect = stats.getValue();
			
			if (skill.hasEffects() && Config.RESTORE_DISPEL_SKILLS && isBuffSlot)
			{
				final Env env = new Env();
				env.setCharacter(effected);
				env.setTarget(effected);
				env.setSkill(skill);
				Effect ef;
				for (final var et : skill.getEffectTemplates())
				{
					ef = et.getEffect(env);
					if (ef != null)
					{
						if (skill.hasEffectType(EffectType.HEAL_OVER_TIME) || skill.hasEffectType(EffectType.CPHEAL_OVER_TIME) || skill.hasEffectType(EffectType.MANA_HEAL_OVER_TIME))
						{
							continue;
						}
						final var efR = ef.getEffectTemplate().getEffect(new Env(effect.getEffector(), effect.getEffected(), effect.getSkill()));
						efR.setCount(effect.getTickCount());
						efR.setAbnormalTime(effect.getAbnormalTime());
						efR.setFirstTime(effect.getTime());
						effects.add(efR);
					}
				}
			}
			
			if (!isBuffSlot && effect.triggersChanceSkill())
			{
				effected.removeChanceEffect(effect);
			}
			effected.stopSkillEffects(skill.getId());
		}
		
		if (Config.RESTORE_DISPEL_SKILLS && !effects.isEmpty() && effected.isPlayer())
		{
			effected.getActingPlayer().getPersonalTasks().addTask(new BuffsBackTask(Config.RESTORE_DISPEL_SKILLS_TIME * 1000, effects));
		}
		return true;
	}
}