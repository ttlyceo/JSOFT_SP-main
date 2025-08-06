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
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.SoIManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 25.09.2020
 */
public class ErosionHallAttack extends AbstractReflection
{
	protected class HEWorld extends ReflectionWorld
	{
		public List<Npc> deadTumors = new ArrayList<>();
		public int tumorCount = 4;
		public Npc cohemenes = null;
		public boolean isBossAttacked = false;
		public long startTime = 0;
		private ScheduledFuture<?> timerTask;
		private boolean conquestBegun = false;
		private boolean conquestEnded = false;
		private long tumorRespawnTime = 180000;
		public synchronized void addTag(int value)
		{
			setTag(getTag() + value);
		}

		public HEWorld()
		{
			setTag(-1);
		}
	}

	private static int[][] COHEMENES_SPAWN =
	{
	        {
	                25634, -178472, 211823, -12025, 0, 0, -1
			},
			{
			        25634, -180926, 211887, -12029, 0, 0, -1
			},
			{
			        25634, -180906, 206635, -12032, 0, 0, -1
			},
			{
			        25634, -178492, 206426, -12023, 0, 0, -1
			}
	};

	public ErosionHallAttack(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32535, 32537);
		addTalkId(32535, 32537);
		addAttackId(25634);
		addEnterZoneId(20014);
		addKillId(18708, 18711, 25634);
	}
	
	@Override
	public final String onEnterZone(Creature character, ZoneType zone)
	{
		if (character.isPlayer())
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(character.getReflectionId());
			if (tmpworld instanceof HEWorld)
			{
				final HEWorld world = (HEWorld) tmpworld;
				if (!world.conquestBegun)
				{
					world.conquestBegun = true;
					runTumors(world);
					world.startTime = System.currentTimeMillis();
					world.timerTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new TimerTask(world), 298 * 1000L, 5 * 60 * 1000L);
				}
			}
		}
		return super.onEnterZone(character, zone);
	}

	private final synchronized void enterInstance(Player player, Npc npc)
	{
		enterInstance(player, npc, new HEWorld(), 119);
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

	protected void runTumors(HEWorld world)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			inst.spawnByGroup("soi_hoe_attack_tumors");
			inst.spawnByGroup("soi_hoe_attack_symbols");
			inst.spawnByGroup("soi_hoe_attack_wards");
			inst.spawnByGroup("soi_hoe_attack_mob_1");
			inst.spawnByGroup("soi_hoe_attack_mob_2");
			inst.spawnByGroup("soi_hoe_attack_mob_3");
			inst.spawnByGroup("soi_hoe_attack_mob_4");
			inst.spawnByGroup("soi_hoe_attack_mob_5");
			inst.spawnByGroup("soi_hoe_attack_mob_6");
			inst.spawnByGroup("soi_hoe_attack_mob_7");
			inst.spawnByGroup("soi_hoe_attack_mob_8");
			for (final Npc n : inst.getNpcs())
			{
				if (n != null && n.getId() == 18708 && n.getReflectionId() == world.getReflectionId())
				{
					n.setCurrentHp(n.getMaxHp() * .5);
				}
			}
		}
		
		for (int zoneId = 20008; zoneId < 20029; zoneId++)
		{
			getActivatedZone(inst, zoneId, true);
		}
		
		broadCastPacket(world, new ExShowScreenMessage(NpcStringId.YOU_CAN_HEAR_THE_UNDEAD_OF_EKIMUS_RUSHING_TOWARD_YOU_S1_S2_IT_HAS_NOW_BEGUN, 2, 1, 8000));
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if (tmpworld instanceof HEWorld)
		{
			final HEWorld world = (HEWorld) tmpworld;

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
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof HEWorld)
		{
			final HEWorld world = (HEWorld) tmpworld;
			if (!world.isBossAttacked)
			{
				world.isBossAttacked = true;
				final Calendar reenter = Calendar.getInstance();
				reenter.add(Calendar.HOUR, 24);
				setReenterTime(world, reenter.getTimeInMillis(), world.getReflection().isHwidCheck());
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof HEWorld)
		{
			final HEWorld world = (HEWorld) tmpworld;
			final Location loc = npc.getLocation();
			if (npc.getId() == 18708)
			{
				world.tumorCount--;
				((MonsterInstance) npc).dropSingleItem(player, 13797, getRandom(2, 5));
				npc.deleteMe();
				final Npc n = spawnNpc(32535, loc, 0, world.getReflection());
				world.deadTumors.add(n);
				world.addTag(1);
				notifyTumorDeath(world, n);
				ThreadPoolManager.getInstance().schedule(new TumorRevival(n, world), world.tumorRespawnTime);
				ThreadPoolManager.getInstance().schedule(new RegenerationCoffinSpawn(n, world), 20000);
			}
			else if (npc.getId() == 25634)
			{
				npc.broadcastPacketToOthers(new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.KEU_I_WILL_LEAVE_FOR_NOW_BUT_DONT_THINK_THIS_IS_OVER_THE_SEED_OF_INFINITY_CAN_NEVER_DIE));
				for (final int objId : world.getAllowed())
				{
					final Player pl = GameObjectsStorage.getPlayer(objId);
					final QuestState st = pl.getQuestState("_696_ConquertheHallofErosion");
					if (st != null && st.isCond(1))
					{
						st.set("cohemenes", "1");
					}
				}
				conquestConclusion(world, true);
				SoIManager.getInstance().notifyCohemenesKill();
			}

			if (npc.getId() == 18711)
			{
				world.tumorRespawnTime += 10000;
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	private void conquestConclusion(HEWorld world, boolean win)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			if (world.timerTask != null)
			{
				world.timerTask.cancel(false);
			}
			
			world.conquestEnded = true;
			inst.despawnByGroup("soi_hoe_attack_symbols");
			inst.despawnByGroup("soi_hoe_attack_wards");
			if (world.cohemenes != null)
			{
				if (!world.cohemenes.isDead())
				{
					world.cohemenes.getMinionList().onMasterDelete();
					world.cohemenes.deleteMe();
				}
				world.cohemenes = null;
			}
			finishInstance(world, 900000, false);
			
			if (win)
			{
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.CONGRATULATIONS_YOU_HAVE_SUCCEEDED_AT_S1_S2_THE_INSTANCE_WILL_SHORTLY_EXPIRE, 2, 1, 8000);
				msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
				msg.addStringParameter("#" + NpcStringId.ATTACK.getId());
				broadCastPacket(world, msg);
				handleReenterTime(world);
			}
			else
			{
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.YOU_HAVE_FAILED_AT_S1_S2_THE_INSTANCE_WILL_SHORTLY_EXPIRE, 2, 1, 8000);
				msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
				msg.addStringParameter("#" + NpcStringId.ATTACK.getId());
				broadCastPacket(world, msg);
			}
			
			for (final Npc npc : inst.getNpcs())
			{
				if (npc != null)
				{
					if (npc.getId() == 18708 || npc.getId() == 32535)
					{
						npc.deleteMe();
					}
				}
			}
		}
	}
	
	private void notifyTumorDeath(HEWorld world, Npc tumor)
	{
		if ((world.tumorCount == 0) && (world.cohemenes == null))
		{
			final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.ALL_THE_TUMORS_INSIDE_S1_HAVE_BEEN_DESTROYED_DRIVEN_INTO_A_CORNER_COHEMENES_APPEARS_CLOSE_BY, 2, 1, 8000);
			msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
			broadCastPacket(world, msg);
			
			final int[] spawn = COHEMENES_SPAWN[getRandom(0, COHEMENES_SPAWN.length - 1)];
			world.cohemenes = addSpawn(spawn[0], spawn[1], spawn[2], spawn[3], spawn[4], false, 0, false, world.getReflection());
			world.cohemenes.broadcastPacketToOthers(new NpcSay(world.cohemenes.getObjectId(), Say2.SHOUT, world.cohemenes.getId(), NpcStringId.CMON_CMON_SHOW_YOUR_FACE_YOU_LITTLE_RATS_LET_ME_SEE_WHAT_THE_DOOMED_WEAKLINGS_ARE_SCHEMING));
		}
		else
		{
			final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_BEEN_DESTROYED_NIN_ORDER_TO_DRAW_OUT_THE_COWARDLY_COHEMENES_YOU_MUST_DESTROY_ALL_THE_TUMORS, 2, 1, 8000);
			msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
			broadCastPacket(world, msg);
		}
		manageRegenZone(tumor, true);
	}
	
	private void notifyTumorRevival(HEWorld world, Npc tumor)
	{
		if (world.tumorCount > 0 && world.cohemenes != null && !world.cohemenes.isDead())
		{
			world.cohemenes.getMinionList().onMasterDelete();
			world.cohemenes.deleteMe();
			world.cohemenes = null;
		}
		
		final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_COMPLETELY_REVIVED_NTHE_RESTRENGTHENED_COHEMENES_HAS_FLED_DEEPER_INSIDE_THE_SEED, 2, 1, 8000);
		msg.addStringParameter("#" + NpcStringId.HALL_OF_EROSION.getId());
		broadCastPacket(world, msg);

		manageRegenZone(tumor, false);
	}

	private class TumorRevival implements Runnable
	{
		private final Npc _deadTumor;
		private final HEWorld _world;

		public TumorRevival(Npc deadTumor, HEWorld world)
		{
			_deadTumor = deadTumor;
			_world = world;
		}

		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			if (_world.conquestEnded)
			{
				return;
			}

			final Npc tumor = spawnNpc(18708, _deadTumor.getLocation(), 0, _world.getReflection());
			tumor.setCurrentHp(tumor.getMaxHp() * .25);
			_world.tumorCount++;
			notifyTumorRevival(_world, _deadTumor);
			_world.deadTumors.remove(_deadTumor);
			_deadTumor.deleteMe();
			_world.addTag(-1);
		}
	}

	private class RegenerationCoffinSpawn implements Runnable
	{
		private final Npc _deadTumor;
		private final HEWorld _world;

		public RegenerationCoffinSpawn(Npc deadTumor, HEWorld world)
		{
			_deadTumor = deadTumor;
			_world = world;
		}

		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			if (_world.conquestEnded)
			{
				return;
			}
			
			for (int i = 0; i < 4; i++)
			{
				spawnNpc(18710, _deadTumor.getLocation(), 0, _world.getReflection());
			}
		}
	}
	
	private class TimerTask implements Runnable
	{
		private final HEWorld _world;
		
		public TimerTask(HEWorld world)
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

	protected void broadCastPacket(HEWorld world, GameServerPacket packet)
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
	
	private void manageRegenZone(Npc npc, boolean doActivate)
	{
		final Reflection reflection = ReflectionManager.getInstance().getReflection(npc.getReflectionId());
		{
			if (reflection != null)
			{
				int zoneId = 0;
				if (ZoneManager.getInstance().isInsideZone(20000, npc))
				{
					zoneId = 20000;
				}
				else if (ZoneManager.getInstance().isInsideZone(20001, npc))
				{
					zoneId = 20001;
				}
				else if (ZoneManager.getInstance().isInsideZone(20002, npc))
				{
					zoneId = 20002;
				}
				else if (ZoneManager.getInstance().isInsideZone(20003, npc))
				{
					zoneId = 20003;
				}
				else if (ZoneManager.getInstance().isInsideZone(20004, npc))
				{
					zoneId = 20004;
				}
				else if (ZoneManager.getInstance().isInsideZone(20005, npc))
				{
					zoneId = 20005;
				}
				else if (ZoneManager.getInstance().isInsideZone(20006, npc))
				{
					zoneId = 20006;
				}
				else if (ZoneManager.getInstance().isInsideZone(20007, npc))
				{
					zoneId = 20007;
				}
				getActivatedZone(reflection, zoneId, doActivate);
			}
		}
	}

	public static void main(String[] args)
	{
		new ErosionHallAttack(ErosionHallAttack.class.getSimpleName(), "instances");
	}
}
