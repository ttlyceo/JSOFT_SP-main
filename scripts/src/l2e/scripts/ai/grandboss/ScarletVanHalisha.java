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
package l2e.scripts.ai.grandboss;

import static l2e.gameserver.ai.model.CtrlIntention.ATTACK;
import static l2e.gameserver.ai.model.CtrlIntention.FOLLOW;
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;

import java.util.ArrayList;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DecoyInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.scripts.ai.AbstractNpcAI;

public final class ScarletVanHalisha extends AbstractNpcAI
{
	private Creature _target;
	private Skill _skill;
	private long _lastRangedSkillTime;
	private final int _rangedSkillMinCoolTime = 60000;
	
	private static final int HALISHA2 = 29046;
	private static final int HALISHA3 = 29047;
	
	public ScarletVanHalisha(String name, String descr)
	{
		super(name, descr);
		
		addAttackId(HALISHA2, HALISHA3);
		addKillId(HALISHA2, HALISHA3);
		addSpellFinishedId(HALISHA2, HALISHA3);
		registerMobs(HALISHA2, HALISHA3);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "attack" :
			{
				if (npc != null)
				{
					getSkillAI(npc);
				}
				break;
			}
			case "random_target" :
			{
				_target = getRandomTarget(npc, null);
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		getSkillAI(npc);
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		startQuestTimer("random_Target", 5000, npc, null, true);
		startQuestTimer("attack", 500, npc, null, true);
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		cancelQuestTimers("attack");
		cancelQuestTimers("random_Target");
		return super.onKill(npc, killer, isSummon);
	}
	
	private Skill getRndSkills(Npc npc)
	{
		switch (npc.getId())
		{
			case HALISHA2 :
			{
				if (Rnd.get(100) < 10)
				{
					return SkillsParser.getInstance().getInfo(5015, 2);
				}
				else if (Rnd.get(100) < 10)
				{
					return SkillsParser.getInstance().getInfo(5015, 5);
				}
				else if (Rnd.get(100) < 2)
				{
					return SkillsParser.getInstance().getInfo(5016, 1);
				}
				else
				{
					return SkillsParser.getInstance().getInfo(5014, 2);
				}
			}
			case HALISHA3 :
			{
				if (Rnd.get(100) < 10)
				{
					return SkillsParser.getInstance().getInfo(5015, 3);
				}
				else if (Rnd.get(100) < 10)
				{
					return SkillsParser.getInstance().getInfo(5015, 6);
				}
				else if (Rnd.get(100) < 10)
				{
					return SkillsParser.getInstance().getInfo(5015, 2);
				}
				else if (((_lastRangedSkillTime + _rangedSkillMinCoolTime) < System.currentTimeMillis()) && (Rnd.get(100) < 10))
				{
					return SkillsParser.getInstance().getInfo(5019, 1);
				}
				else if (((_lastRangedSkillTime + _rangedSkillMinCoolTime) < System.currentTimeMillis()) && (Rnd.get(100) < 10))
				{
					return SkillsParser.getInstance().getInfo(5018, 1);
				}
				else if (Rnd.get(100) < 2)
				{
					return SkillsParser.getInstance().getInfo(5016, 1);
				}
				else
				{
					return SkillsParser.getInstance().getInfo(5014, 3);
				}
			}
		}
		return SkillsParser.getInstance().getInfo(5014, 1);
	}
	
	private synchronized void getSkillAI(Npc npc)
	{
		if (npc.isInvul() || npc.isCastingNow())
		{
			return;
		}
		if ((Rnd.get(100) < 30) || (_target == null) || _target.isDead())
		{
			_skill = getRndSkills(npc);
			_target = getRandomTarget(npc, _skill);
		}
		final Creature target = _target;
		Skill skill = _skill;
		if (skill == null)
		{
			skill = getRndSkills(npc);
		}
		
		if (npc.isPhysicalMuted())
		{
			return;
		}
		
		if ((target == null) || target.isDead())
		{
			npc.setIsCastingNow(false);
			return;
		}
		
		if (Util.checkIfInRange(skill.getCastRange(), npc, target, true))
		{
			npc.getAI().setIntention(IDLE);
			npc.setTarget(target);
			npc.setIsCastingNow(true);
			_target = null;
			npc.doCast(skill);
		}
		else
		{
			npc.getAI().setIntention(FOLLOW, target, null);
			npc.getAI().setIntention(ATTACK, target, null);
			npc.setIsCastingNow(false);
		}
	}
	
	private Creature getRandomTarget(Npc npc, Skill skill)
	{
		final ArrayList<Creature> result = new ArrayList<>();
		for (final Creature obj : World.getInstance().getAroundCharacters(npc, 600, 200))
		{
			if (obj.isPlayable() || (obj instanceof DecoyInstance))
			{
				if (obj.isPlayer() && obj.getActingPlayer().isInvisible())
				{
					continue;
				}
				
				if (((obj.getZ() < (npc.getZ() - 100)) && (obj.getZ() > (npc.getZ() + 100))) || !GeoEngine.getInstance().canSeeTarget(obj, npc))
				{
					continue;
				}
			}
			if (obj.isPlayable() || (obj instanceof DecoyInstance))
			{
				int skillRange = 150;
				if (skill != null)
				{
					switch (skill.getId())
					{
						case 5014 :
						{
							skillRange = 150;
							break;
						}
						case 5015 :
						{
							skillRange = 400;
							break;
						}
						case 5016 :
						{
							skillRange = 200;
							break;
						}
						case 5018 :
						case 5019 :
						{
							_lastRangedSkillTime = System.currentTimeMillis();
							skillRange = 550;
							break;
						}
					}
				}
				if (Util.checkIfInRange(skillRange, npc, obj, true) && !obj.isDead())
				{
					result.add(obj);
				}
			}
		}
		if (!result.isEmpty() && (result.size() != 0))
		{
			final Object[] characters = result.toArray();
			return (Creature) characters[Rnd.get(characters.length)];
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new ScarletVanHalisha(ScarletVanHalisha.class.getSimpleName(), "ai");
	}
}
