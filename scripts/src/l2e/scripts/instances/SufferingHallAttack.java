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
package l2e.scripts.instances;


import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.EffectType;

/**
 * Rework by LordWinter 29.09.2020
 */
public class SufferingHallAttack extends AbstractReflection
{
	private class SHAWorld extends ReflectionWorld
	{
		public long[] storeTime =
		{
		        0, 0
		};

		protected void calcRewardItemId()
		{
			final Long finishDiff = storeTime[1] - storeTime[0];
			if (finishDiff < 1260000)
			{
				setTag(13777);
			}
			else if (finishDiff < 1380000)
			{
				setTag(13778);
			}
			else if (finishDiff < 1500000)
			{
				setTag(13779);
			}
			else if (finishDiff < 1620000)
			{
				setTag(13780);
			}
			else if (finishDiff < 1740000)
			{
				setTag(13781);
			}
			else if (finishDiff < 1860000)
			{
				setTag(13782);
			}
			else if (finishDiff < 1980000)
			{
				setTag(13783);
			}
			else if (finishDiff < 2100000)
			{
				setTag(13784);
			}
			else if (finishDiff < 2220000)
			{
				setTag(13785);
			}
			else
			{
				setTag(13786);
			}
		}

		public SHAWorld()
		{
			setTag(-1);
		}
	}

	public SufferingHallAttack(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32530, 32537);
		addTalkId(32530, 32537);
		addAttackId(25665, 25666);
		addSkillSeeId(22509, 22510, 22511, 22512, 22513, 22514, 22515);
		addKillId(18704, 25665);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new SHAWorld(), 115))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((SHAWorld) world).storeTime[0] = System.currentTimeMillis();
			spawnRoom((SHAWorld) world, 1);
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
		else
		{
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		if (npc.getId() == 32537)
		{
			enterInstance(player, npc);
		}
		return null;
	}
	
	private void spawnRoom(SHAWorld world, int id)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			switch (id)
			{
				case 1 :
					inst.spawnByGroup("soi_hos_attack_1");
					getActivatedZone(inst, 20029, true);
					break;
				case 2 :
					inst.spawnByGroup("soi_hos_attack_2");
					getActivatedZone(inst, 20030, true);
					break;
				case 3 :
					inst.spawnByGroup("soi_hos_attack_3");
					getActivatedZone(inst, 20031, true);
					break;
				case 4 :
					inst.spawnByGroup("soi_hos_attack_4");
					getActivatedZone(inst, 20032, true);
					break;
				case 5 :
					inst.spawnByGroup("soi_hos_attack_5");
					getActivatedZone(inst, 20033, true);
					break;
				case 6 :
					inst.spawnByGroup("soi_hos_attack_6");
					getActivatedZone(inst, 20034, true);
					break;
				case 7 :
					inst.spawnByGroup("soi_hos_attack_7");
					getActivatedZone(inst, 20034, false);
					break;
				default :
					break;
			}
		}
	}

	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		if (skill.hasEffectType(EffectType.REBALANCE_HP, EffectType.HEAL, EffectType.HEAL_PERCENT))
		{
			int hate = 2 * skill.getAggroPoints();
			if (hate < 2)
			{
				hate = 1000;
			}
			((Attackable) npc).addDamageHate(caster, 0, hate);
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof SHAWorld)
		{
			final SHAWorld world = (SHAWorld) tmpworld;
			if (npc.getId() == 18704)
			{
				npc.deleteMe();
				addSpawn(32531, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, false, npc.getReflection());
				final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
				if (inst != null)
				{
					if (ZoneManager.getInstance().isInsideZone(20029, npc))
					{
						getActivatedZone(inst, 20029, false);
						spawnRoom(world, 2);
					}
					else if (ZoneManager.getInstance().isInsideZone(20030, npc))
					{
						getActivatedZone(inst, 20030, false);
						spawnRoom(world, 3);
					}
					else if (ZoneManager.getInstance().isInsideZone(20031, npc))
					{
						getActivatedZone(inst, 20031, false);
						spawnRoom(world, 4);
					}
					else if (ZoneManager.getInstance().isInsideZone(20032, npc))
					{
						getActivatedZone(inst, 20032, false);
						spawnRoom(world, 5);
					}
					else if (ZoneManager.getInstance().isInsideZone(20033, npc))
					{
						getActivatedZone(inst, 20033, false);
						spawnRoom(world, 6);
					}
				}
			}
			else if (npc.getId() == 25665)
			{
				ThreadPoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						spawnRoom(world, 7);
						world.storeTime[1] = System.currentTimeMillis();
						world.calcRewardItemId();
						finishInstance(world, 300000, true);
					}
				}, 10000L);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new SufferingHallAttack(SufferingHallAttack.class.getSimpleName(), "instances");
	}
}