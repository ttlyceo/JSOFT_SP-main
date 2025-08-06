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

import java.util.concurrent.ScheduledFuture;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
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
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;

/**
 * Rework by LordWinter 29.09.2020
 */
public class SufferingHallDefence extends AbstractReflection
{
	private class SHDWorld extends ReflectionWorld
	{
		private int stage = 1;
		private ScheduledFuture<?> coffinSpawnTask;
		private ScheduledFuture<?> monstersSpawnTask;
		private boolean doCountCoffinNotifications = false;
		public int tumorIndex = 300;
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

		public SHDWorld()
		{
			setTag(-1);
		}
	}
	
	private static final int[] monsters =
	{
	        22509, 22510, 22511, 22512, 22513, 22514, 22515, 18704
	};

	public SufferingHallDefence(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32530, 32537);
		addTalkId(32530, 32537);
		addAttackId(25665, 25666);
		addSkillSeeId(22509, 22510, 22511, 22512, 22513, 22514, 22515);
		addKillId(18704, 22509, 22510, 22511, 22512, 22513, 22514, 22515, 25665, 25666);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new SHDWorld(), 116))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((SHDWorld) world).storeTime[0] = System.currentTimeMillis();
			startDefence((SHDWorld) world);
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
		if (tmpworld instanceof SHDWorld)
		{
			final SHDWorld world = (SHDWorld) tmpworld;
			if (ArrayUtils.contains(monsters, npc.getId()) && !checkAliveMonsters(world))
			{
				if (world.monstersSpawnTask != null)
				{
					world.monstersSpawnTask.cancel(false);
				}
				world.monstersSpawnTask = ThreadPoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						spawnMonsters(world);
					}
				}, 40000L);
			}
			
			if (npc.getId() == 18704)
			{
				npc.deleteMe();
				notifyCoffinActivity(npc, world);
				addSpawn(18705, -173704, 218092, -9562, 0, false, 0, false, npc.getReflection());
				world.tumorIndex = 300;
				world.doCountCoffinNotifications = true;
			}
			else if (npc.getId() == 25665)
			{
				ThreadPoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						world.storeTime[1] = System.currentTimeMillis();
						world.calcRewardItemId();
						if (world.monstersSpawnTask != null)
						{
							world.monstersSpawnTask.cancel(false);
						}
						if (world.coffinSpawnTask != null)
						{
							world.coffinSpawnTask.cancel(false);
						}
						final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
						if (inst != null)
						{
							inst.spawnByGroup("soi_hos_defence_tepios");
						}
						finishInstance(world, 300000, true);

					}
				}, 10000L);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public void notifyCoffinActivity(Npc npc, SHDWorld world)
	{
		if (!world.doCountCoffinNotifications)
		{
			return;
		}
		
		world.tumorIndex -= 5;

		if (world.tumorIndex == 100)
		{
			broadCastPacket(world, new ExShowScreenMessage(NpcStringId.THE_AREA_NEAR_THE_TUMOR_IS_FULL_OF_OMINOUS_ENERGY, 2, 1, 8000));
		}
		else if (world.tumorIndex == 30)
		{
			broadCastPacket(world, new ExShowScreenMessage(NpcStringId.YOU_CAN_FEEL_THE_SURGING_ENERGY_OF_DEATH_FROM_THE_TUMOR, 2, 1, 8000));
		}
		if (world.tumorIndex <= 0)
		{
			if (getTumor(world, 18705) != null)
			{
				getTumor(world, 18705).deleteMe();
			}
			final Npc aliveTumor = spawnNpc(18704, new Location(-173704, 218092, -9562), 0, world.getReflection());
			aliveTumor.setCurrentHp(aliveTumor.getMaxHp() * .4);
			world.doCountCoffinNotifications = false;
		}
	}
	
	private void startDefence(SHDWorld world)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			inst.spawnByGroup("soi_hos_defence_tumor");
			world.doCountCoffinNotifications = true;
			world.coffinSpawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					addSpawn(18706, -173704, 218092, -9562, 0, false, 0, false, world.getReflection());
				}
			}, 1000L, 10000L);
			world.monstersSpawnTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					spawnMonsters(world);
				}
			}, 60000L);
		}
	}
	
	private void spawnMonsters(SHDWorld world)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			if (world.stage > 6)
			{
				return;
			}
			String group = null;
			switch (world.stage)
			{
				case 1 :
					group = "soi_hos_defence_mobs_1";
					getActivatedZone(inst, 20035, true);
					break;
				case 2 :
					group = "soi_hos_defence_mobs_2";
					getActivatedZone(inst, 20035, false);
					getActivatedZone(inst, 20036, true);
					break;
				case 3 :
					group = "soi_hos_defence_mobs_3";
					getActivatedZone(inst, 20036, false);
					getActivatedZone(inst, 20037, true);
					break;
				case 4 :
					group = "soi_hos_defence_mobs_4";
					getActivatedZone(inst, 20037, false);
					getActivatedZone(inst, 20038, true);
					break;
				case 5 :
					group = "soi_hos_defence_mobs_5";
					getActivatedZone(inst, 20038, false);
					getActivatedZone(inst, 20039, true);
					break;
				case 6 :
					world.doCountCoffinNotifications = false;
					group = "soi_hos_defence_brothers";
					getActivatedZone(inst, 20039, false);
					break;
				default :
					break;
			}
			world.stage++;
			if (group != null)
			{
				inst.spawnByGroup(group);
			}
			for (final Npc n : inst.getNpcs())
			{
				if ((n != null) && !n.isDead() && (n.getReflectionId() == world.getReflectionId()))
				{
					if (n.isMonster() && ArrayUtils.contains(monsters, n.getId()))
					{
						n.setRunning();
						n.getAI().setIntention(CtrlIntention.MOVING, Location.findPointToStay(new Location(-173704, 218092, -9562), 200, true), 0);
					}
				}
			}
		}
	}
	
	private boolean checkAliveMonsters(SHDWorld world)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			for (final Npc n : inst.getNpcs())
			{
				if (ArrayUtils.contains(monsters, n.getId()) && !n.isDead() && (n.getReflectionId() == world.getReflectionId()))
				{
					return true;
				}
			}
			return false;
		}
		return false;
	}
	
	private Npc getTumor(SHDWorld world, int id)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			for (final Npc npc : inst.getNpcs())
			{
				if (npc != null && npc.getId() == id && !npc.isDead() && (npc.getReflectionId() == world.getReflectionId()))
				{
					return npc;
				}
			}
		}
		return null;
	}

	protected void broadCastPacket(SHDWorld world, GameServerPacket packet)
	{
		for (final int objId : world.getAllowed())
		{
			final Player player = GameObjectsStorage.getPlayer(objId);
			if ((player != null) && player.isOnline() && (player.getReflectionId() == world.getReflectionId()))
			{
				player.sendPacket(packet);
			}
		}
	}

	public static void main(String[] args)
	{
		new SufferingHallDefence(SufferingHallDefence.class.getSimpleName(), "instances");
	}
}