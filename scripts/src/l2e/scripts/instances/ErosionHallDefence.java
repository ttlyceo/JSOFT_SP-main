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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.instance.QuestGuardInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 25.09.2020
 */
public class ErosionHallDefence extends AbstractReflection
{
	protected class HEDWorld extends ReflectionWorld
	{
		public List<Npc> alivetumor = new ArrayList<>();
		public List<Npc> deadTumors = new ArrayList<>();
		public long startTime = 0;
		public ScheduledFuture<?> timerTask, agressionTask, coffinSpawnTask, aliveTumorSpawnTask, failureTask;
		public int tumorKillCount = 0;
		protected boolean conquestBegun = false;
		protected boolean conquestEnded = false;
		private boolean soulwagonSpawned = false;
		private int seedKills = 0;
		private long tumorRespawnTime = 180000;
		
		public synchronized void addTag(int value)
		{
			setTag(getTag() + value);
		}

		public HEDWorld()
		{
			setTag(-1);
		}
	}

	public ErosionHallDefence(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32535, 32537);
		addTalkId(32535, 32537);
		addSpawnId(32535, 32541);
		addEnterZoneId(20014);
		addKillId(18708, 18711, 32541);
	}
	
	@Override
	public final String onEnterZone(Creature character, ZoneType zone)
	{
		if (character.isPlayer())
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(character.getReflectionId());
			if (tmpworld instanceof HEDWorld)
			{
				final HEDWorld world = (HEDWorld) tmpworld;
				if (!world.conquestBegun)
				{
					world.conquestBegun = true;
					runTumors(world);
					world.startTime = System.currentTimeMillis();
					world.timerTask = ThreadPoolManager.getInstance().schedule(new TimerTask(world), 20 * 60000);
				}
			}
		}
		return super.onEnterZone(character, zone);
	}

	private final synchronized void enterInstance(Player player, Npc npc)
	{
		enterInstance(player, npc, new HEDWorld(), 120);
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

	protected void runTumors(final HEDWorld world)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			inst.spawnByGroup("soi_hoe_defence_lifeseed");
			inst.spawnByGroup("soi_hoe_defence_tumor");
			inst.spawnByGroup("soi_hoe_defence_wards");
			inst.spawnByGroup("soi_hoe_defence_mob_1");
			inst.spawnByGroup("soi_hoe_defence_mob_2");
			inst.spawnByGroup("soi_hoe_defence_mob_3");
			inst.spawnByGroup("soi_hoe_defence_mob_4");
			inst.spawnByGroup("soi_hoe_defence_mob_5");
			inst.spawnByGroup("soi_hoe_defence_mob_6");
			inst.spawnByGroup("soi_hoe_defence_mob_7");
			inst.spawnByGroup("soi_hoe_defence_mob_8");
			
			for (int zoneId = 20008; zoneId < 20029; zoneId++)
			{
				getActivatedZone(inst, zoneId, true);
			}
			
			world.agressionTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					if (!world.conquestEnded)
					{
						for (final Npc npc : inst.getNpcs())
						{
							final Npc seed = getNearestSeed(npc);
							if (seed != null)
							{
								if (npc.getAI().getIntention() == CtrlIntention.ACTIVE)
								{
									npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, seed, 100);
									ThreadPoolManager.getInstance().schedule(new Runnable()
			                        {
				                        @Override
				                        public void run()
				                        {
					                        if (npc instanceof Attackable)
					                        {
												((Attackable) npc).clearAggroList(false);
						                        npc.getAI().setIntention(CtrlIntention.ACTIVE);
												npc.getAI().setIntention(CtrlIntention.MOVING, Location.findPointToStay(npc, 400, true), 0);
					                        }
				                        }
			                        }, 7000L);
								}
							}
						}
					}
				}
			}, 15000L, 25000L);
			world.coffinSpawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					if (!world.conquestEnded)
					{
						for(final Npc npc : world.deadTumors)
						{
							if (npc != null)
							{
								spawnNpc(18709, npc.getLocation(), 0, world.getReflection());
							}
						}
					}
				}
			}, 1000L, 60000L);
			world.aliveTumorSpawnTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					if (!world.conquestEnded)
					{
						inst.despawnByGroup("soi_hoe_defence_tumor");
						inst.spawnByGroup("soi_hoe_defence_alivetumor");
						for (final Npc npc : inst.getNpcs())
						{
							if (npc != null && npc.getId() == 18708)
							{
								npc.setCurrentHp(npc.getMaxHp() * 0.5);
							}
						}
						final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_COMPLETELY_REVIVED_NRECOVERED_NEARBY_UNDEAD_ARE_SWARMING_TOWARD_SEED_OF_LIFE, 2, 1, 8000);
						msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
						broadCastPacket(world, msg);
					}
				}
			}, world.tumorRespawnTime);
		}
		broadCastPacket(world, new ExShowScreenMessage(NpcStringId.YOU_CAN_HEAR_THE_UNDEAD_OF_EKIMUS_RUSHING_TOWARD_YOU_S1_S2_IT_HAS_NOW_BEGUN, 2, 1, 8000));
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if (tmpworld instanceof HEDWorld)
		{
			final HEDWorld world = (HEDWorld) tmpworld;

			if (event.startsWith("warp"))
			{
				if (!world.deadTumors.isEmpty())
				{
					player.destroyItemByItemId("SOI", 13797, 1, player, true);
					final Location loc = world.deadTumors.get(getRandom(world.deadTumors.size())).getLocation();
					if (loc != null)
					{
						final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.S1S_PARTY_HAS_MOVED_TO_A_DIFFERENT_LOCATION_THROUGH_THE_CRACK_IN_THE_TUMOR, 2, 1, 8000);
						msg.addStringParameter(player.getParty() != null ? player.getParty().getLeader().getName(null) : player.getName(null));
						broadCastPacket(world, msg);
						for (final Player partyMember : player.getParty().getMembers())
						{
							if (partyMember.isInsideRadius(player, 500, true, false))
							{
								partyMember.teleToLocation(loc, true, partyMember.getReflection());
							}
						}
					}
				}
			}
		}
		return "";
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
	public final String onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case 32541 :
				((QuestGuardInstance) npc).setPassive(true);
				npc.setCurrentHp(500000);
				break;
			case 32535 :
				final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
				if (tmpworld != null && tmpworld instanceof HEDWorld)
				{
					((HEDWorld) tmpworld).deadTumors.add(npc);
					((HEDWorld) tmpworld).addTag(1);
				}
				break;
		}
		return super.onSpawn(npc);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof HEDWorld)
		{
			final HEDWorld world = (HEDWorld) tmpworld;
			if (npc.getId() == 18708)
			{
				((MonsterInstance) npc).dropSingleItem(player, 13797, getRandom(2, 5));
				world.alivetumor.remove(npc);
				npc.deleteMe();
				notifyTumorDeath(world);
				final Npc n = spawnNpc(32535, npc.getLocation(), 0, world.getReflection());
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_BEEN_DESTROYED_NTHE_NEARBY_UNDEAD_THAT_WERE_ATTACKING_SEED_OF_LIFE_START_LOSING_THEIR_ENERGY_AND_RUN_AWAY, 2, 1, 8000);
				msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
				broadCastPacket(world, msg);
				ThreadPoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						world.deadTumors.remove(n);
						n.deleteMe();
						final Npc tumor = spawnNpc(18708, n.getLocation(), 0, world.getReflection());
						tumor.setCurrentHp(tumor.getMaxHp() * 0.25);
						world.alivetumor.add(tumor);
						world.tumorKillCount--;
						final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_COMPLETELY_REVIVED_NRECOVERED_NEARBY_UNDEAD_ARE_SWARMING_TOWARD_SEED_OF_LIFE, 2, 1, 8000);
						msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
						broadCastPacket(world, msg);
					}
				}, world.tumorRespawnTime);
			}
			else if (npc.getId() == 25636)
			{
				conquestConclusion(world, true);
			}
			if (npc.getId() == 18711)
			{
				world.tumorRespawnTime -= 5 * 1000;
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	private void notifyTumorDeath(HEDWorld world)
	{
		world.tumorKillCount++;
		if (world.tumorKillCount >= 4 && !world.soulwagonSpawned)
		{
			world.soulwagonSpawned = true;
			final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (inst != null)
			{
				inst.spawnByGroup("soi_hoe_defence_soulwagon");
				for (final Npc npc : inst.getNpcs())
				{
					if (npc != null && npc.getId() == 25636)
					{
						final NpcSay cs = new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.HA_HA_HA);
						npc.broadcastPacketToOthers(cs);
						
						final Npc seed = getNearestSeed(npc);
						if (seed != null)
						{
							npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, seed, 100);
						}
						rescheduleFailureTask(world, 180000L);
					}
				}
			}
		}
	}

	@Override
	public String onKillByMob(Npc npc, Npc killer)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof HEDWorld)
		{
			final HEDWorld world = (HEDWorld) tmpworld;

			world.seedKills++;
			if (world.seedKills >= 4)
			{
				conquestConclusion(world, false);
			}
		}
		return null;
	}

	private void conquestConclusion(HEDWorld world, boolean win)
	{
		if (world.conquestEnded)
		{
			return;
		}
		
		if (world.timerTask != null)
		{
			world.timerTask.cancel(false);
		}
		if (world.agressionTask != null)
		{
			world.agressionTask.cancel(false);
		}
		if (world.coffinSpawnTask != null)
		{
			world.coffinSpawnTask.cancel(false);
		}
		if (world.aliveTumorSpawnTask != null)
		{
			world.aliveTumorSpawnTask.cancel(false);
		}
		if (world.failureTask != null)
		{
			world.failureTask.cancel(false);
		}
		
		world.conquestEnded = true;
		
		finishInstance(world, 900000, false);
		
		if (win)
		{
			final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (inst != null)
			{
				for (final int objId : world.getAllowed())
				{
					final Player player = GameObjectsStorage.getPlayer(objId);
					final QuestState st = player.getQuestState("_697_DefendtheHallofErosion");
					if (st != null && st.isCond(1))
					{
						st.set("defenceDone", 1);
					}
				}
				inst.cleanupNpcs();
			}
			final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.CONGRATULATIONS_YOU_HAVE_SUCCEEDED_AT_S1_S2_THE_INSTANCE_WILL_SHORTLY_EXPIRE, 2, 1, 8000);
			msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
			msg.addStringParameter("#" + NpcStringId.DEFEND.getId());
			broadCastPacket(world, msg);
			handleReenterTime(world);
		}
		else
		{
			final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.YOU_HAVE_FAILED_AT_S1_S2_THE_INSTANCE_WILL_SHORTLY_EXPIRE, 2, 1, 8000);
			msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
			msg.addStringParameter("#" + NpcStringId.DEFEND.getId());
			broadCastPacket(world, msg);
		}
		
	}

	private class TimerTask implements Runnable
	{
		private final HEDWorld _world;
		
		public TimerTask(HEDWorld world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final long time = (_world.startTime + 25 * 60 * 1000L - System.currentTimeMillis()) / 60000;
			if (time == 0)
			{
				conquestConclusion(_world, false);
			}
			else
			{
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.S1_MINUTES_REMAINING, 2, 1, 8000);
				msg.addStringParameter(Integer.toString((int) (_world.startTime + 25 * 60 * 1000L - System.currentTimeMillis()) / 60000));
				broadCastPacket(_world, msg);
			}
		}
	}
	
	private void rescheduleFailureTask(HEDWorld world, long time)
	{
		if (world.failureTask != null)
		{
			world.failureTask.cancel(false);
			world.failureTask = null;
		}
		world.failureTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				conquestConclusion(world, false);
			}
		}, time);
	}
	
	private static Npc getNearestSeed(Npc mob)
	{
		for (final Npc npc : World.getInstance().getAroundNpc(mob, 900, 300))
		{
			if (npc.getId() == 32541)
			{
				return npc;
			}
		}
		return null;
	}
	
	protected void broadCastPacket(HEDWorld world, GameServerPacket packet)
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
		new ErosionHallDefence(ErosionHallDefence.class.getSimpleName(), "instances");
	}
}
