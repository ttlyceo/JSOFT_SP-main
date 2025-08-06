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
package l2e.scripts.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter 16.10.2019
 */
public class Baium extends DefaultAI
{
	private long _newTarget = 0L;
	private final Skill _normal_attack, _energy_wave, _earth_quake, _thunderbolt, _group_hold;

	public Baium(Attackable actor)
	{
		super(actor);
		
		_normal_attack = SkillsParser.getInstance().getInfo(4127, 1);
		_energy_wave = SkillsParser.getInstance().getInfo(4128, 1);
		_earth_quake = SkillsParser.getInstance().getInfo(4129, 1);
		_thunderbolt = SkillsParser.getInstance().getInfo(4130, 1);
		_group_hold = SkillsParser.getInstance().getInfo(4131, 1);
		actor.setIsGlobalAI(true);
	}

	@Override
	protected void onEvtSpawn()
	{
		_newTarget = System.currentTimeMillis();
		super.onEvtSpawn();
	}
	
	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		if (actor == null)
		{
			return;
		}
		
		if ((_newTarget + 20000L) < System.currentTimeMillis())
		{
			final List<Creature> alive = new ArrayList<>();
			for (final Creature target : World.getInstance().getAroundCharacters(actor, 4000, 200))
			{
				if (target != null)
				{
					if (!target.isDead() && !target.isInvisible() && GeoEngine.getInstance().canSeeTarget(actor, target))
					{
						if (target.getId() == 29021 && Rnd.get(100) <= 50)
						{
							continue;
						}
						alive.add(target);
					}
				}
			}
			
			if (alive == null || alive.isEmpty())
			{
				super.thinkAttack();
				return;
			}
			
			final Creature rndTarget = alive.get(Rnd.get(alive.size()));
			if (rndTarget != null)
			{
				if (rndTarget.getId() == 29021 || rndTarget.isPlayer())
				{
					final Creature mostHate = actor.getAggroList().getMostHated();
					if (mostHate != null)
					{
						actor.addDamageHate(rndTarget, 0, (actor.getAggroList().getHating(mostHate) + 500));
					}
					else
					{
						actor.addDamageHate(rndTarget, 0, 2000);
					}
					actor.setTarget(rndTarget);
					setAttackTarget(rndTarget);
				}
			}
			_newTarget = System.currentTimeMillis();
		}
		super.thinkAttack();
	}

	@Override
	protected boolean createNewTask()
	{
		final Attackable actor = getActiveChar();
		if(actor == null)
		{
			return true;
		}

		final GameObject target = actor.getTarget();
		if (target == null)
		{
			return false;
		}
		
		if (actor.isMoving())
		{
			return true;
		}

		final int energy_wave = 20;
		final int earth_quake = 20;
		final int group_hold = actor.getCurrentHpPercents() > 50 ? 0 : 20;
		final int thunderbolt = actor.getCurrentHpPercents() > 25 ? 0 : 20;

		Skill select = null;

		if(actor.isMovementDisabled())
		{
			select = _thunderbolt;
			return cast(select);
		}
		else if (!Rnd.chance(100 - thunderbolt - group_hold - energy_wave - earth_quake))
		{
			final Map<Skill, Integer> skillList = new HashMap<>();
			final double distance = actor.getDistance(target);

			addDesiredSkill(skillList, (Creature) target, distance, _energy_wave);
			addDesiredSkill(skillList, (Creature) target, distance, _earth_quake);
			if (group_hold > 0)
			{
				addDesiredSkill(skillList, (Creature) target, distance, _group_hold);
			}
			if (thunderbolt > 0)
			{
				addDesiredSkill(skillList, (Creature) target, distance, _thunderbolt);
			}
			select = selectTopSkill(skillList);
		}
		
		if (Rnd.chance(20))
		{
			if (select == null)
			{
				select = _normal_attack;
			}
			return cast(select);
		}
		return false;
	}
}