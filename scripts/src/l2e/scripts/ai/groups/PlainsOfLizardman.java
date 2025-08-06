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
package l2e.scripts.ai.groups;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.scripts.ai.AbstractNpcAI;

public final class PlainsOfLizardman extends AbstractNpcAI
{
	private static final int INVISIBLE_NPC = 18919;
	private static final int TANTA_GUARD = 18862;
	private static final int FANTASY_MUSHROOM = 18864;
	private static final int STICKY_MUSHROOM = 18865;
	private static final int RAINBOW_FROG = 18866;
	private static final int ENERGY_PLANT = 18868;
	private static final int TANTA_SCOUT = 22768;
	private static final int TANTA_MAGICIAN = 22773;
	private static final int TANTA_SUMMONER = 22774;
	private static final int[] MOBS =
	{
	        22768, 22769, 22770, 22771, 22772, 22773, 22774,
	};
	
	private static final SkillHolder STUN_EFFECT = new SkillHolder(6622, 1);
	private static final SkillHolder DEMOTIVATION_HEX = new SkillHolder(6425, 1);
	private static final SkillHolder FANTASY_MUSHROOM_SKILL = new SkillHolder(6427, 1);
	private static final SkillHolder RAINBOW_FROG_SKILL = new SkillHolder(6429, 1);
	private static final SkillHolder STICKY_MUSHROOM_SKILL = new SkillHolder(6428, 1);
	private static final SkillHolder ENERGY_PLANT_SKILL = new SkillHolder(6430, 1);
	
	private static final SkillHolder[] BUFFS =
	{
	        new SkillHolder(6625, 1), new SkillHolder(6626, 2), new SkillHolder(6627, 3), new SkillHolder(6628, 1), new SkillHolder(6629, 2), new SkillHolder(6630, 3), new SkillHolder(6631, 1), new SkillHolder(6633, 1), new SkillHolder(6635, 1), new SkillHolder(6636, 1), new SkillHolder(6638, 1), new SkillHolder(6639, 1), new SkillHolder(6640, 1), new SkillHolder(6674, 1),
	};
	
	private static final int[] BUFF_LIST =
	{
	        6, 7, 8, 11, 13
	};
	
	private PlainsOfLizardman(String name, String descr)
	{
		super(name, descr);
		
		addAttackId(FANTASY_MUSHROOM, RAINBOW_FROG, STICKY_MUSHROOM, ENERGY_PLANT, TANTA_SUMMONER);
		addKillId(MOBS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equals("fantasy_mushroom") && (npc != null) && (player != null))
		{
			npc.doCast(FANTASY_MUSHROOM_SKILL.getSkill());
			for (final Npc target : World.getInstance().getAroundNpc(npc, 200, 200))
			{
				if ((target != null) && target.isAttackable())
				{
					final Attackable monster = (Attackable) target;
					npc.setTarget(monster);
					npc.doCast(STUN_EFFECT.getSkill());
					attackPlayer(monster, player);
				}
			}
			npc.setIsInvul(false);
			npc.doDie(player);
		}
		return null;
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		switch (npc.getId())
		{
			case TANTA_SUMMONER :
				if (npc.getFirstEffect(DEMOTIVATION_HEX.getId()) == null)
				{
					npc.doCast(DEMOTIVATION_HEX.getSkill());
				}
				break;
			case RAINBOW_FROG :
				castSkill(npc, attacker, RAINBOW_FROG_SKILL);
				break;
			case ENERGY_PLANT :
				castSkill(npc, attacker, ENERGY_PLANT_SKILL);
				break;
			case STICKY_MUSHROOM :
				castSkill(npc, attacker, STICKY_MUSHROOM_SKILL);
				break;
			case FANTASY_MUSHROOM :
				if (npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					npc.setIsInvul(true);
					for (final Npc target : World.getInstance().getAroundNpc(npc, 1000, 200))
					{
						if ((target != null) && target.isAttackable())
						{
							final Attackable monster = (Attackable) target;
							if ((monster.getId() == TANTA_MAGICIAN) || (monster.getId() == TANTA_SCOUT))
							{
								target.setIsRunning(true);
								target.getAI().setIntention(CtrlIntention.MOVING, new Location(npc.getX(), npc.getY(), npc.getZ(), 0), 0);
							}
						}
					}
					startQuestTimer("fantasy_mushroom", 4000, npc, attacker);
				}
				break;
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(1000) == 0)
		{
			final Npc guard = addSpawn(TANTA_GUARD, npc);
			attackPlayer((Attackable) guard, killer);
		}
		
		final int random = getRandom(100);
		final Npc buffer = addSpawn(INVISIBLE_NPC, npc.getLocation(), false, 6000);
		buffer.setTarget(killer);
		
		if (random <= 42)
		{
			castRandomBuff(buffer, 7, 45, BUFFS[0], BUFFS[1], BUFFS[2]);
		}
		if (random <= 11)
		{
			castRandomBuff(buffer, 8, 60, BUFFS[3], BUFFS[4], BUFFS[5]);
			castRandomBuff(buffer, 3, 6, BUFFS[9], BUFFS[10], BUFFS[12]);
		}
		if (random <= 25)
		{
			buffer.doCast(BUFFS[BUFF_LIST[getRandom(BUFF_LIST.length)]].getSkill());
		}
		if (random <= 10)
		{
			buffer.doCast(BUFFS[13].getSkill());
		}
		if (random <= 1)
		{
			final int i = getRandom(100);
			if (i <= 34)
			{
				buffer.doCast(BUFFS[6].getSkill());
				buffer.doCast(BUFFS[7].getSkill());
				buffer.doCast(BUFFS[8].getSkill());
			}
			else if (i < 67)
			{
				buffer.doCast(BUFFS[13].getSkill());
			}
			else
			{
				buffer.doCast(BUFFS[2].getSkill());
				buffer.doCast(BUFFS[5].getSkill());
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private void castRandomBuff(Npc npc, int chance1, int chance2, SkillHolder... buffs)
	{
		final int rand = getRandom(100);
		if (rand <= chance1)
		{
			npc.doCast(buffs[2].getSkill());
		}
		else if (rand <= chance2)
		{
			npc.doCast(buffs[1].getSkill());
		}
		else
		{
			npc.doCast(buffs[0].getSkill());
		}
	}
	
	private void castSkill(Npc npc, Creature target, SkillHolder skill)
	{
		npc.doDie(target);
		
		final Npc buffer = addSpawn(INVISIBLE_NPC, npc.getLocation(), false, 6000);
		buffer.setTarget(target);
		buffer.doCast(skill.getSkill());
	}
	
	public static void main(String[] args)
	{
		new PlainsOfLizardman(PlainsOfLizardman.class.getSimpleName(), "ai");
	}
}
