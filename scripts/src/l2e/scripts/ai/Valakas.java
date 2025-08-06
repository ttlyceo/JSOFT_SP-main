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
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.NoRestartZone;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Created by LordWinter 23.10.2019
 */
public class Valakas extends Fighter
{
	private final Skill _lavaSkill = getSkill(4680, 1), _fearSkill = getSkill(4689, 1), _defSkill = getSkill(5864, 1), _berserkSkill = getSkill(5865, 1);
	private final Skill _trempleSkill = getSkill(4681, 1), _tremplerSkill = getSkill(4682, 1), _tailSkill = getSkill(4685, 1), _taillSkill = getSkill(4688, 1), _meteorSkill = getSkill(4690, 1), _breathlSkill = getSkill(4683, 1), _breathhSkill = getSkill(4684, 1);
	private final Skill _destroySkill1 = getSkill(5860, 1), _destroySkill2 = getSkill(5861, 1), _destroySkill3 = getSkill(5862, 1), _destroySkill4 = getSkill(5863, 1);

	private long _defenceDownTimer = Long.MAX_VALUE;
	private final long _defenceDownReuse = 120000L;

	private double _rangedAttacksIndex, _counterAttackIndex, _attacksIndex;
	private int _hpStage = 0;
	private final List<MonsterInstance> _minions = new ArrayList<>();
	private final NoRestartZone _zone = ZoneManager.getInstance().getZoneById(70052, NoRestartZone.class);
	
	public Valakas(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (actor == null)
		{
			return;
		}
		
		for (final var p : actor.getReflectionId() == 0 ? actor.getReflection().getPlayers() : _zone.getPlayersInside())
		{
			notifyEvent(CtrlEvent.EVT_AGGRESSION, p, 1);
		}

		if(damage > 100)
		{
			if(attacker.getDistance(actor) > 400)
			{
				_rangedAttacksIndex += damage / 1000D;
			}
			else
			{
				_counterAttackIndex += damage / 1000D;
			}
		}
		_attacksIndex += damage / 1000D;
		
		super.onEvtAttacked(attacker, damage);
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
			actor.makeTriggerCast(getSkill(4691, 1), actor);
			_hpStage = 1;
		}
		else if(chp < 80 && _hpStage == 1)
		{
			actor.makeTriggerCast(getSkill(4691, 2), actor);
			_defenceDownTimer = System.currentTimeMillis();
			_hpStage = 2;
		}
		else if(chp < 50 && _hpStage == 2)
		{
			actor.makeTriggerCast(getSkill(4691, 3), actor);
			_hpStage = 3;
		}
		else if(chp < 30 && _hpStage == 3)
		{
			actor.makeTriggerCast(getSkill(4691, 4), actor);
			_hpStage = 4;
		}
		else if(chp < 10 && _hpStage == 4)
		{
			actor.makeTriggerCast(getSkill(4691, 5), actor);
			_hpStage = 5;
		}
		
		if (getAliveMinionsCount() < 12 && Rnd.chance(2))
		{
			final MonsterInstance minion = NpcUtils.spawnSingle(29029, Location.findPointToStay(actor.getLocation(), 400, 700, false), actor.getReflection(), 0);
			_minions.add(minion);
		}

		if(_counterAttackIndex > 2000)
		{
			_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(new ExShowScreenMessage(NpcStringId.BECAUSE_THE_COWARDLY_COUNTERATTACKS_CONTINUED_VALAKASS_FURY_HAS_REACHED_ITS_MAXIMUMNVALAKASS_P_ATK_IS_GREATLY_INCREASED, 2, 8000)));
			_counterAttackIndex = 0;
			return cast(_berserkSkill);
		}
		else if(_rangedAttacksIndex > 2000)
		{
			if(Rnd.chance(60))
			{
				aggroReconsider();
				_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(new ExShowScreenMessage(NpcStringId.VALAKAS_HAS_BEEN_ENRAGED_BY_THE_LONG_RANGE_CONCENTRATION_ATTACKS_AND_IS_COMING_TOWARD_ITS_TARGET_WITH_EVEN_GREATER_ZEAL, 2, 8000)));
				_rangedAttacksIndex = 0;
			}
			else
			{
				_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(new ExShowScreenMessage(NpcStringId.LONG_RANGE_CONCENTRATION_ATTACKS_HAVE_ENRAGED_VALAKASNIF_YOU_CONTINUE_IT_MAY_BECOME_A_DANGEROUS_SITUATION, 2, 8000)));
				_rangedAttacksIndex = 0;
				return cast(_berserkSkill);
			}
		}
		else if(_attacksIndex > 3000)
		{
			_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(new ExShowScreenMessage(NpcStringId.SOME_WARRIORS_BLOW_HAS_LEFT_A_HUGE_GASH_BETWEEN_THE_GREAT_SCALES_OF_VALAKASNVALAKASS_P_DEF_IS_GREATLY_DECREASED, 2, 8000)));
			_attacksIndex = 0;
			return cast(_defSkill);
		}
		else if (_defenceDownTimer < System.currentTimeMillis())
		{
			_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(new ExShowScreenMessage(NpcStringId.ANNOYING_CONCENTRATION_ATTACKS_ARE_DISRUPTING_VALAKASS_CONCENTRATIONNIF_IT_CONTINUES_YOU_MAY_GET_A_GREAT_OPPORTUNITY, 2, 8000)));
			_defenceDownTimer = System.currentTimeMillis() + _defenceDownReuse + Rnd.get(60) * 1000L;
			return cast(_fearSkill);
		}

		if (Rnd.chance(30))
		{
			return cast(Rnd.chance(50) ? _trempleSkill : _tremplerSkill);
		}

		final Map<Skill, Integer> d_skill = new HashMap<>();
		switch(_hpStage)
		{
			case 1:
				addDesiredSkill(d_skill, (Creature) target, distance, _breathlSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _tailSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _meteorSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _defSkill);
				break;
			case 2:
			case 3:
				addDesiredSkill(d_skill, (Creature) target, distance, _breathlSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _tailSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _breathhSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _taillSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _destroySkill1);
				addDesiredSkill(d_skill, (Creature) target, distance, _destroySkill2);
				addDesiredSkill(d_skill, (Creature) target, distance, _meteorSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _fearSkill);
				break;
			case 4:
			case 5:
				addDesiredSkill(d_skill, (Creature) target, distance, _breathlSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _tailSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _breathhSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _taillSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _destroySkill1);
				addDesiredSkill(d_skill, (Creature) target, distance, _destroySkill2);
				addDesiredSkill(d_skill, (Creature) target, distance, _meteorSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, _fearSkill);
				addDesiredSkill(d_skill, (Creature) target, distance, Rnd.chance(60) ? _destroySkill3 : _destroySkill4);
				break;
		}

		if (Rnd.chance(20))
		{
			final Skill r_skill = selectTopSkill(d_skill);
			if (r_skill != null)
			{
				if (!r_skill.isOffensive())
				{
					target = actor;
				}
				return cast(r_skill);
			}
		}
		return false;
	}

	@Override
	protected void thinkAttack()
	{
		final var actor = getActiveChar();
		if (actor == null)
		{
			return;
		}
		
		if (actor.isInsideZone(ZoneId.DANGER_AREA))
		{
			if (actor.getFirstEffect(_lavaSkill) == null)
			{
				actor.makeTriggerCast(_lavaSkill, actor);
			}
		}
		super.thinkAttack();
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

	private Skill getSkill(int id, int level)
	{
		return SkillsParser.getInstance().getInfo(id, level);
	}
	
	private int getAliveMinionsCount()
	{
		int i = 0;
		for (final MonsterInstance n : _minions)
		{
			if (n != null && !n.isDead() && n.getDistance(_actor) < 8000)
			{
				i++;
			}
		}
		return i;
	}
}