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
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
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
 * Rework by LordWinter 30.09.2020
 */
public class HeartInfinityDefence extends AbstractReflection
{
	private class HIDWorld extends ReflectionWorld
	{
		private long tumorRespawnTime = 0;
		private long wagonRespawnTime = 0;
		private int coffinsCreated = 0;
		protected boolean conquestEnded = false;
		public List<Npc> deadTumors = new ArrayList<>();
		public long startTime = 0;
		private Npc preawakenedEchmus = null;
		private ScheduledFuture<?> timerTask = null, wagonSpawnTask = null, coffinSpawnTask = null, aliveTumorSpawnTask = null;

		public HIDWorld()
		{
		}
	}

	public HeartInfinityDefence(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32535, 32536, 32539);
		addTalkId(32535, 32536, 32539);
		addEnterZoneId(200010);
		addKillId(18708, 18711);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new HIDWorld(), 122))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((HIDWorld) world).tumorRespawnTime = 3 * 60 * 1000L;
			((HIDWorld) world).wagonRespawnTime = 60 * 1000L;
			((HIDWorld) world).coffinsCreated = 0;
			ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					conquestBegins((HIDWorld) world);
				}
			}, 20000L);
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

	private void conquestBegins(final HIDWorld world)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			broadCastPacket(world, new ExShowScreenMessage(NpcStringId.YOU_CAN_HEAR_THE_UNDEAD_OF_EKIMUS_RUSHING_TOWARD_YOU_S1_S2_IT_HAS_NOW_BEGUN, 2, 1, 8000));
			inst.spawnByGroup("soi_hoi_defence_mob_1");
			inst.spawnByGroup("soi_hoi_defence_mob_2");
			inst.spawnByGroup("soi_hoi_defence_mob_3");
			inst.spawnByGroup("soi_hoi_defence_mob_4");
			inst.spawnByGroup("soi_hoi_defence_mob_5");
			inst.spawnByGroup("soi_hoi_defence_mob_6");
			inst.spawnByGroup("soi_hoi_defence_tumors");
			inst.spawnByGroup("soi_hoi_defence_wards");
			inst.getDoor(14240102).openMe();
			for (int zoneId = 20040; zoneId < 20046; zoneId++)
			{
				getActivatedZone(inst, zoneId, true);
			}
			world.preawakenedEchmus = addSpawn(29161, -179534, 208510, -15496, 16342, false, 0, false, world.getReflection());
			world.coffinSpawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					if (!world.conquestEnded)
					{
						for (final Npc n : inst.getNpcs())
						{
							if (n != null && n.getId() == 32535 && n.getReflectionId() == world.getReflectionId())
							{
								spawnNpc(18709, n.getLocation(), 0, world.getReflection());
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
						inst.despawnByGroup("soi_hoi_defence_tumors");
						inst.spawnByGroup("soi_hoi_defence_alivetumors");
						for (final Npc n : inst.getNpcs())
						{
							if (n != null && n.getId() == 18708 && n.getReflectionId() == world.getReflectionId())
							{
								n.setCurrentHp(n.getMaxHp() * 0.5);
							}
						}
						final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_COMPLETELY_REVIVED_NEKIMUS_STARTED_TO_REGAIN_HIS_ENERGY_AND_IS_DESPERATELY_LOOKING_FOR_HIS_PREY, 2, 1, 8000);
						msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
						broadCastPacket(world, msg);
					}
				}
			}, world.tumorRespawnTime);
			world.wagonSpawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					addSpawn(22523, -179544, 207400, -15496, 0, false, 0, false, world.getReflection());
				}
			}, 1000L, world.wagonRespawnTime);
			world.startTime = System.currentTimeMillis();
			world.timerTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new TimerTask(world), 298 * 1000L, 5 * 60 * 1000L);
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if (tmpworld instanceof HIDWorld)
		{
			final HIDWorld world = (HIDWorld) tmpworld;

			if (event.startsWith("warpechmus"))
			{
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.S1S_PARTY_HAS_MOVED_TO_A_DIFFERENT_LOCATION_THROUGH_THE_CRACK_IN_THE_TUMOR, 2, 1, 8000);
				msg.addStringParameter(player.getParty() != null ? player.getParty().getLeader().getName(null) : player.getName(null));
				broadCastPacket(world, msg);
				for (final Player partyMember : player.getParty().getMembers())
				{
					if (partyMember.isInsideRadius(player, 800, true, false))
					{
						partyMember.teleToLocation(-179548, 209584, -15504, true, partyMember.getReflection());
					}
				}
			}
			else if (event.startsWith("reenterechmus"))
			{
				player.destroyItemByItemId("SOI", 13797, 3, player, true);
				for (final Player partyMember : player.getParty().getMembers())
				{
					if (partyMember.isInsideRadius(player, 400, true, false))
					{
						partyMember.teleToLocation(-179548, 209584, -15504, true, partyMember.getReflection());
					}
				}
			}
			else if (event.startsWith("warp"))
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

		if (npc.getId() == 32539)
		{
			enterInstance(player, npc);
		}
		return "";
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof HIDWorld)
		{
			final HIDWorld world = (HIDWorld) tmpworld;
			final Location loc = npc.getLocation();
			if (npc.getId() == 18708)
			{
				((MonsterInstance) npc).dropSingleItem(player, 13797, getRandom(2, 5));
				npc.deleteMe();
				final Npc deadTumor = spawnNpc(32535, loc, 0, world.getReflection());
				world.deadTumors.add(deadTumor);
				world.wagonRespawnTime += 10000;
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_BEEN_DESTROYED_NTHE_SPEED_THAT_EKIMUS_CALLS_OUT_HIS_PREY_HAS_SLOWED_DOWN, 2, 1, 8000);
				msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
				broadCastPacket(world, msg);
				
				ThreadPoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						world.deadTumors.remove(deadTumor);
						deadTumor.deleteMe();
						final Npc alivetumor = spawnNpc(18708, loc, 0, world.getReflection());
						if (alivetumor != null)
						{
							alivetumor.setCurrentHp(alivetumor.getMaxHp() * .25);
						}
						world.wagonRespawnTime -= 10000;
						final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_COMPLETELY_REVIVED_NEKIMUS_STARTED_TO_REGAIN_HIS_ENERGY_AND_IS_DESPERATELY_LOOKING_FOR_HIS_PREY, 2, 1, 8000);
						msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
						broadCastPacket(world, msg);
					}
				}, world.tumorRespawnTime);
			}

			if (npc.getId() == 18711)
			{
				world.tumorRespawnTime += 5 * 1000;
			}
		}
		return "";
	}

	protected void notifyWagonArrived(Npc npc, HIDWorld world)
	{
		world.coffinsCreated++;
		if (world.coffinsCreated == 20)
		{
			conquestConclusion(world, false);
		}
		else
		{
			final NpcSay cs = new NpcSay(world.preawakenedEchmus.getObjectId(), Say2.SHOUT, world.preawakenedEchmus.getId(), NpcStringId.BRING_MORE_MORE_SOULS);
			world.preawakenedEchmus.broadcastPacketToOthers(cs);
			final ExShowScreenMessage message = new ExShowScreenMessage(NpcStringId.THE_SOUL_COFFIN_HAS_AWAKENED_EKIMUS, 2, 1, 8000);
			message.addStringParameter(Integer.toString(20 - world.coffinsCreated));
			broadCastPacket(world, message);
			final int[] spawn = ZoneManager.getInstance().getZoneById(200032).getZone().getRandomPoint();
			addSpawn(18713, spawn[0], spawn[1], spawn[2], 0, false, 0, false, world.getReflection());
		}
	}

	private class TimerTask implements Runnable
	{
		private final HIDWorld _world;

		TimerTask(HIDWorld world)
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
			
			final long time = ((_world.startTime + (25 * 60 * 1000L)) - System.currentTimeMillis()) / 60000;
			if (time == 0)
			{
				conquestConclusion(_world, true);
			}
			else
			{
				if (time == 15)
				{
					final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
					if (inst != null)
					{
						inst.spawnByGroup("soi_hoi_defence_bosses");
					}
					final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.S1_MINUTES_REMAINING, 2, 1, 8000);
					msg.addStringParameter(Integer.toString((int) (_world.startTime + 25 * 60 * 1000L - System.currentTimeMillis()) / 60000));
					broadCastPacket(_world, msg);
				}
			}
		}
	}

	protected void conquestConclusion(HIDWorld world, boolean win)
	{
		if (world.conquestEnded)
		{
			return;
		}
		
		if (world.timerTask != null)
		{
			world.timerTask.cancel(false);
		}

		if (world.coffinSpawnTask != null)
		{
			world.coffinSpawnTask.cancel(false);
		}
		if (world.aliveTumorSpawnTask != null)
		{
			world.aliveTumorSpawnTask.cancel(false);
		}
		
		if (world.wagonSpawnTask != null)
		{
			world.wagonSpawnTask.cancel(false);
		}
		
		world.conquestEnded = true;
		
		if (win)
		{
			finishInstance(world, 900000, true);
			final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.CONGRATULATIONS_YOU_HAVE_SUCCEEDED_AT_S1_S2_THE_INSTANCE_WILL_SHORTLY_EXPIRE, 2, 1, 8000);
			msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
			msg.addStringParameter("#" + NpcStringId.ATTACK.getId());
			broadCastPacket(world, msg);
			handleReenterTime(world);
			for (final int objId : world.getAllowed())
			{
				final Player player = GameObjectsStorage.getPlayer(objId);
				if (player != null)
				{
					final QuestState st = player.getQuestState("_697_DefendtheHallofErosion");
					if (st != null && (st.isCond(1)))
					{
						st.set("defenceDone", 1);
					}
				}
			}
		}
		else
		{
			finishInstance(world, 900000, false);
			final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.YOU_HAVE_FAILED_AT_S1_S2_THE_INSTANCE_WILL_SHORTLY_EXPIRE, 2, 1, 8000);
			msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
			msg.addStringParameter("#" + NpcStringId.ATTACK.getId());
			broadCastPacket(world, msg);
		}
	}

	@Override
	public final String onEnterZone(Creature character, ZoneType zone)
	{
		if (character instanceof Attackable)
		{
			final Attackable npc = (Attackable) character;
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpworld instanceof HIDWorld)
			{
				final HIDWorld world = (HIDWorld) tmpworld;
				if (npc.getId() == 22523)
				{
					notifyWagonArrived(npc, world);
					npc.deleteMe();
				}
			}
		}
		return null;
	}

	protected void broadCastPacket(HIDWorld world, GameServerPacket packet)
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
		new HeartInfinityDefence(HeartInfinityDefence.class.getSimpleName(), "instances");
	}
}