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

import l2e.commons.util.NpcUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.type.NoRestartZone;

/**
 * Created by LordWinter 10.01.2024
 */
public class Antharas extends Fighter
{
	final Skill s_fear = getSkill(4108, 1), s_fear2 = getSkill(5092, 1), s_curse = getSkill(4109, 1), s_paralyze = getSkill(4111, 1);
	final Skill s_shock = getSkill(4106, 1), s_shock2 = getSkill(4107, 1), s_antharas_ordinary_attack = getSkill(4112, 1), s_antharas_ordinary_attack2 = getSkill(4113, 1), s_meteor = getSkill(5093, 1), s_breath = getSkill(4110, 1);
	final Skill s_regen1 = getSkill(4239, 1), s_regen2 = getSkill(4240, 1), s_regen3 = getSkill(4241, 1);

	private int _hpStage = 0;
	private static long _minionsSpawnDelay = 0;
	private final List<MonsterInstance> _minions = new ArrayList<>();
	private final NoRestartZone _zone = ZoneManager.getInstance().getZoneById(70050, NoRestartZone.class);

	public Antharas(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final var actor = getActiveChar();
		if (actor == null || actor.isDead())
		{
			return;
		}
		
		if (actor.isScriptValue(0))
		{
			actor.setScriptValue(1);
			for (final var p : actor.getReflectionId() == 0 ? actor.getReflection().getPlayers() : _zone.getPlayersInside())
			{
				notifyEvent(CtrlEvent.EVT_AGGRESSION, p, 1);
			}
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		_minionsSpawnDelay = System.currentTimeMillis() + 180000;
	}

	@Override
	protected boolean createNewTask()
	{
		final var actor = getActiveChar();
		if (actor == null || actor.isDead())
		{
			return false;
		}
		
		var target = actor.getTarget();
		if (target == null)
		{
			return false;
		}
		
		if (actor.isMoving())
		{
			return true;
		}

		final double distance = actor.getDistance(target);

		final double chp = actor.getCurrentHpPercents();
		if(_hpStage == 0)
		{
			actor.makeTriggerCast(s_regen1, actor);
			_hpStage = 1;
		}
		else if(chp < 75 && _hpStage == 1)
		{
			actor.makeTriggerCast(s_regen2, actor);
			_hpStage = 2;
		}
		else if(chp < 50 && _hpStage == 2)
		{
			actor.makeTriggerCast(s_regen3, actor);
			_hpStage = 3;
		}
		else if(chp < 30 && _hpStage == 3)
		{
			actor.makeTriggerCast(s_regen3, actor);
			_hpStage = 4;
		}

		if(_minionsSpawnDelay < System.currentTimeMillis() && getAliveMinionsCount() < 30 && Rnd.chance(5))
		{
			_minionsSpawnDelay = System.currentTimeMillis() + 180000;
			final var minion = NpcUtils.spawnSingle(Rnd.chance(50) ? 29190 : 29069, Location.findPointToStay(actor.getLocation(), 400, 700, false), actor.getReflection(), 0);
			_minions.add(minion);
		}

		if (Rnd.chance(30))
		{
			return cast(Rnd.chance(50) ? s_antharas_ordinary_attack : s_antharas_ordinary_attack2);
		}

		final Map<Skill, Integer> skills = new HashMap<>();
		switch(_hpStage)
		{
			case 1:
				addDesiredSkill(skills, (Creature) target, distance, s_curse);
				addDesiredSkill(skills, (Creature) target, distance, s_paralyze);
				addDesiredSkill(skills, (Creature) target, distance, s_meteor);
				break;
			case 2:
				addDesiredSkill(skills, (Creature) target, distance, s_curse);
				addDesiredSkill(skills, (Creature) target, distance, s_paralyze);
				addDesiredSkill(skills, (Creature) target, distance, s_meteor);
				addDesiredSkill(skills, (Creature) target, distance, s_fear2);
				break;
			case 3:
				addDesiredSkill(skills, (Creature) target, distance, s_curse);
				addDesiredSkill(skills, (Creature) target, distance, s_paralyze);
				addDesiredSkill(skills, (Creature) target, distance, s_meteor);
				addDesiredSkill(skills, (Creature) target, distance, s_fear2);
				addDesiredSkill(skills, (Creature) target, distance, s_shock2);
				addDesiredSkill(skills, (Creature) target, distance, s_breath);
				break;
			case 4:
				addDesiredSkill(skills, (Creature) target, distance, s_curse);
				addDesiredSkill(skills, (Creature) target, distance, s_paralyze);
				addDesiredSkill(skills, (Creature) target, distance, s_meteor);
				addDesiredSkill(skills, (Creature) target, distance, s_fear2);
				addDesiredSkill(skills, (Creature) target, distance, s_shock2);
				addDesiredSkill(skills, (Creature) target, distance, s_fear);
				addDesiredSkill(skills, (Creature) target, distance, s_shock);
				addDesiredSkill(skills, (Creature) target, distance, s_breath);
				break;
			default:
				break;
		}

		if (Rnd.chance(20))
		{
			final var sk = selectTopSkill(skills);
			if (sk != null)
			{
				if (!sk.isOffensive())
				{
					target = actor;
				}
				return cast(sk);
			}
		}
		skills.clear();
		return false;
	}

	private long getAliveMinionsCount()
	{
		if (_minions != null && !_minions.isEmpty())
		{
			return _minions.stream().filter(m -> (m != null && !m.isDead())).count();
		}
		return 0;
	}

	private Skill getSkill(int id, int level)
	{
		return SkillsParser.getInstance().getInfo(id, level);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		if (_minions != null && !_minions.isEmpty())
		{
			_minions.stream().filter(n -> (n != null)).forEach(n -> n.deleteMe());
			_minions.clear();
		}
		super.onEvtDead(killer);
	}

}