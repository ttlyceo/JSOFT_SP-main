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
import l2e.gameserver.instancemanager.SoIManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.funcs.FuncGet;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 30.09.2020
 */
public class HeartInfinityAttack extends AbstractReflection
{
	private class HIAWorld extends ReflectionWorld
	{
		private long tumorRespawnTime;
		private Player invoker;
		private boolean conquestBegun = false;
		protected boolean conquestEnded = false;
		private boolean houndBlocked = false;
		public List<Npc> deadTumors = new ArrayList<>();
		protected Npc ekimus;
		protected List<Npc> hounds = new ArrayList<>(2);
		public int tumorCount = 6;
		public long startTime = 0;
		protected ScheduledFuture<?> timerTask;
		protected ScheduledFuture<?> ekimusActivityTask;
		private long lastAction = 0L;
		private boolean faildAnnounce = false;
		
		public synchronized void addTag(int value)
		{
			setTag(getTag() + value);
		}

		public HIAWorld()
		{
			setTag(-1);
		}
	}

	public HeartInfinityAttack(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32535, 32536, 32540);
		addTalkId(32535, 32536, 32540);
		addSpawnId(32535);
		addAttackId(29150);
		addKillId(18708, 18711, 29150);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new HIAWorld(), 121))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (inst != null)
			{
				inst.spawnByGroup("soi_hoi_attack_init");
			}
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
		
		if (npc.getId() == 32540)
		{
			enterInstance(player, npc);
		}
		return "";
	}

	protected void notifyEchmusEntrance(final HIAWorld world, Player player)
	{
		if (world.conquestBegun)
		{
			return;
		}

		world.conquestBegun = true;
		world.invoker = player;
		final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.YOU_WILL_PARTICIPATE_IN_S1_S2_SHORTLY_BE_PREPARED_FOR_ANYTHING, 2, 1, 8000);
		msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
		msg.addStringParameter("#" + NpcStringId.ATTACK.getId());
		broadCastPacket(world, msg);
		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				for (final int objId : world.getAllowed())
				{
					final Player player = GameObjectsStorage.getPlayer(objId);
					player.showQuestMovie(2);
				}

				ThreadPoolManager.getInstance().schedule(new Runnable()
		        {
			        @Override
			        public void run()
			        {
				        conquestBegins(world);
			        }
		        }, 62500);
			}
		}, 20000);
	}

	protected void conquestBegins(HIAWorld world)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			inst.despawnByGroup("soi_hoi_attack_init");
			inst.spawnByGroup("soi_hoi_attack_mob_1");
			inst.spawnByGroup("soi_hoi_attack_mob_2");
			inst.spawnByGroup("soi_hoi_attack_mob_3");
			inst.spawnByGroup("soi_hoi_attack_mob_4");
			inst.spawnByGroup("soi_hoi_attack_mob_5");
			inst.spawnByGroup("soi_hoi_attack_mob_6");
			inst.spawnByGroup("soi_hoi_attack_tumors");
			for (final Npc n : inst.getNpcs())
			{
				if (n != null && n.getId() == 18708 && n.getReflectionId() == world.getReflectionId())
				{
					n.setCurrentHp(n.getMaxHp() * .5);
				}
			}
			inst.spawnByGroup("soi_hoi_attack_wards");
			world.tumorRespawnTime = 150 * 1000L;
			world.ekimus = addSpawn(29150, -179537, 208854, -15504, 16384, false, 0, false, world.getReflection());
			world.hounds.add(addSpawn(29151, -179224, 209624, -15504, 16384, false, 0, false, world.getReflection()));
			world.hounds.add(addSpawn(29151, -179880, 209464, -15504, 16384, false, 0, false, world.getReflection()));
			world.lastAction = System.currentTimeMillis();
			world.ekimusActivityTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new NotifyEkimusActivity(world), 60000, 60000);
			handleEkimusStats(world);
			for (int zoneId = 20040; zoneId < 20046; zoneId++)
			{
				getActivatedZone(inst, zoneId, true);
			}
			inst.getDoor(14240102).openMe();
			broadCastPacket(world, new ExShowScreenMessage(NpcStringId.YOU_CAN_HEAR_THE_UNDEAD_OF_EKIMUS_RUSHING_TOWARD_YOU_S1_S2_IT_HAS_NOW_BEGUN, 2, 1, 8000));
			if (world.invoker != null)
			{
				world.ekimus.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, world.invoker, 50000);
				final NpcSay cs = new NpcSay(world.ekimus.getObjectId(), Say2.SHOUT, world.ekimus.getId(), NpcStringId.I_SHALL_ACCEPT_YOUR_CHALLENGE_S1_COME_AND_DIE_IN_THE_ARMS_OF_IMMORTALITY);
				cs.addStringParameter(world.invoker.getName(null));
				world.ekimus.broadcastPacketToOthers(cs);
			}
			world.startTime = System.currentTimeMillis();
			world.timerTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new TimerTask(world), 298 * 1000, 5 * 60 * 1000);
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if (tmpworld instanceof HIAWorld)
		{
			final HIAWorld world = (HIAWorld) tmpworld;

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
				notifyEchmusEntrance(world, player);
			}
			else if (event.startsWith("reenterechmus"))
			{
				player.destroyItemByItemId("SOI", 13797, 3, player, true);
				notifyEkimusRoomEntrance(world);
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
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof HIAWorld)
		{
			final HIAWorld world = (HIAWorld) tmpworld;
			if (npc.getId() == 29150)
			{
				if (world.faildAnnounce)
				{
					world.faildAnnounce = false;
				}
				world.lastAction = System.currentTimeMillis();
				
				for (final Npc mob : world.hounds)
				{
					((MonsterInstance) mob).addDamageHate(attacker, 0, 500);
					mob.setRunning();
					mob.getAI().setIntention(CtrlIntention.ATTACK, attacker);
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}

	@Override
	public final String onSpawn(Npc npc)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof HIAWorld)
		{
			final HIAWorld world = (HIAWorld) tmpworld;
			if (npc.getId() == 32535)
			{
				world.addTag(1);
			}
		}
		return super.onSpawn(npc);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof HIAWorld)
		{
			final HIAWorld world = (HIAWorld) tmpworld;
			if (npc.getId() == 18708)
			{
				((MonsterInstance) npc).dropSingleItem(player, 13797, getRandom(2, 5));
				npc.deleteMe();
				world.tumorCount--;
				notifyTumorDeath(world);
				final Npc deadTumor = spawnNpc(32535, npc.getLocation(), 0, world.getReflection());
				world.deadTumors.add(deadTumor);
				ThreadPoolManager.getInstance().schedule(new TumorRevival(deadTumor, world), world.tumorRespawnTime);
				ThreadPoolManager.getInstance().schedule(new RegenerationCoffinSpawn(deadTumor, world), 20000);
			}
			else if (npc.getId() == 29150)
			{
				conquestConclusion(world, true);
				SoIManager.getInstance().notifyEkimusKill();
			}
			else if (npc.getId() == 18711)
			{
				world.tumorRespawnTime += 8 * 1000;
			}
		}
		return "";
	}
	
	private void notifyTumorDeath(HIAWorld world)
	{
		if (world.tumorCount < 1)
		{
			world.houndBlocked = true;
			for (final Npc hound : world.hounds)
			{
				hound.block();
			}
			broadCastPacket(world, new ExShowScreenMessage(NpcStringId.WITH_ALL_CONNECTIONS_TO_THE_TUMOR_SEVERED_EKIMUS_HAS_LOST_ITS_POWER_TO_CONTROL_THE_FERAL_HOUND, 2, 1, 8000));
		}
		else
		{
			final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_THAT_HAS_PROVIDED_ENERGY_N_TO_EKIMUS_IS_DESTROYED, 2, 1, 8000);
			msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
			broadCastPacket(world, msg);
		}
		handleEkimusStats(world);
	}
	
	protected void notifyTumorRevival(HIAWorld world)
	{
		if ((world.tumorCount == 1) && world.houndBlocked)
		{
			world.houndBlocked = false;
			for (final Npc hound : world.hounds)
			{
				hound.unblock();
			}
			broadCastPacket(world, new ExShowScreenMessage(NpcStringId.WITH_THE_CONNECTION_TO_THE_TUMOR_RESTORED_EKIMUS_HAS_REGAINED_CONTROL_OVER_THE_FERAL_HOUND, 2, 1, 8000));
		}
		else
		{
			final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THE_TUMOR_INSIDE_S1_HAS_BEEN_COMPLETELY_RESURRECTED_N_AND_STARTED_TO_ENERGIZE_EKIMUS_AGAIN, 2, 1, 8000);
			msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
			broadCastPacket(world, msg);
		}
		handleEkimusStats(world);
	}

	private class TumorRevival implements Runnable
	{
		private final Npc _deadTumor;
		private final HIAWorld _world;

		public TumorRevival(Npc deadTumor, HIAWorld world)
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
			final Npc alivetumor = spawnNpc(18708, _deadTumor.getLocation(), 0, _world.getReflection());
			alivetumor.setCurrentHp(alivetumor.getMaxHp() * .25);
			_world.tumorCount++;
			notifyTumorRevival(_world);
			_world.deadTumors.add(_deadTumor);
			_deadTumor.deleteMe();
			_world.addTag(-1);
		}
	}

	private class TimerTask implements Runnable
	{
		private final HIAWorld _world;

		TimerTask(HIAWorld world)
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
				conquestConclusion(_world, false);
			}
			else
			{
				if (time == 20)
				{
					final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
					if (inst != null)
					{
						inst.spawnByGroup("soi_hoi_attack_bosses");
					}
				}
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.S1_MINUTES_REMAINING, 2, 1, 8000);
				msg.addStringParameter(Integer.toString((int) (_world.startTime + 25 * 60 * 1000L - System.currentTimeMillis()) / 60000));
				broadCastPacket(_world, msg);
			}
		}
	}

	private class RegenerationCoffinSpawn implements Runnable
	{
		private final Npc _deadTumor;
		private final HIAWorld _world;

		public RegenerationCoffinSpawn(Npc deadTumor, HIAWorld world)
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

	private void handleEkimusStats(HIAWorld world)
	{
		final double[] a = getStatMultiplier(world);
		world.ekimus.removeStatsOwner(this);
		world.ekimus.addStatFunc(new FuncGet(Stats.POWER_ATTACK, 0x30, this, world.ekimus.getTemplate().getBasePAtk() * 3));
		world.ekimus.addStatFunc(new FuncGet(Stats.MAGIC_ATTACK, 0x30, this, world.ekimus.getTemplate().getBaseMAtk() * 10));
		world.ekimus.addStatFunc(new FuncGet(Stats.POWER_DEFENCE, 0x30, this, world.ekimus.getTemplate().getBasePDef() * a[1]));
		world.ekimus.addStatFunc(new FuncGet(Stats.MAGIC_DEFENCE, 0x30, this, world.ekimus.getTemplate().getBaseMDef() * a[0]));
		world.ekimus.addStatFunc(new FuncGet(Stats.REGENERATE_HP_RATE, 0x30, this, world.ekimus.getTemplate().getBaseHpReg() * a[2]));
	}

	private double[] getStatMultiplier(HIAWorld world)
	{
		final double[] a = new double[3];
		switch (world.tumorCount)
		{
			case 6 :
				a[0] = 2;
				a[1] = 1;
				a[2] = 4;
				break;
			case 5 :
				a[0] = 1.9;
				a[1] = 0.9;
				a[2] = 3.5;
				break;
			case 4 :
				a[0] = 1.5;
				a[1] = 0.6;
				a[2] = 3.0;
				break;
			case 3 :
				a[0] = 1.0;
				a[1] = 0.4;
				a[2] = 2.5;
				break;
			case 2 :
				a[0] = 0.7;
				a[1] = 0.3;
				a[2] = 2.0;
				break;
			case 1 :
				a[0] = 0.3;
				a[1] = 0.15;
				a[2] = 1.0;
				break;
			case 0 :
				a[0] = 0.12;
				a[1] = 0.06;
				a[2] = 0.25;
				break;
		}
		return a;
	}
	
	protected class NotifyEkimusActivity implements Runnable
	{
		private final HIAWorld _world;
		
		public NotifyEkimusActivity(HIAWorld world)
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
			
			final Long temp = (System.currentTimeMillis() - _world.lastAction);
			
			if (temp >= 120000L && !_world.faildAnnounce)
			{
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.THERE_IS_NO_PARTY_CURRENTLY_CHALLENGING_EKIMUS_N_IF_NO_PARTY_ENTERS_WITHIN_S1_SECONDS_THE_ATTACK_ON_THE_HEART_OF_IMMORTALITY_WILL_FAIL, 2, 1, 8000);
				msg.addStringParameter("60");
				broadCastPacket(_world, msg);
				_world.faildAnnounce = true;
			}
			else if (temp >= 180000L)
			{
				ThreadPoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						conquestConclusion(_world, false);
					}
				}, 8000L);
			}
		}
	}

	public void notifyEkimusRoomEntrance(final HIAWorld world)
	{
		for (final Player ch : ZoneManager.getInstance().getZoneById(200032).getPlayersInside())
		{
			if (ch != null)
			{
				ch.teleToLocation(-179537, 211233, -15472, true, ch.getReflection());
			}
		}

		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				broadCastPacket(world, new ExShowScreenMessage(NpcStringId.EKIMUS_HAS_SENSED_ABNORMAL_ACTIVITY_NTHE_ADVANCING_PARTY_IS_FORCEFULLY_EXPELLED, 2, 1, 8000));
			}
		}, 10000);
	}

	protected void conquestConclusion(HIAWorld world, boolean win)
	{
		final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		if (inst != null)
		{
			if (world.timerTask != null)
			{
				world.timerTask.cancel(false);
			}
			
			if (world.ekimusActivityTask != null)
			{
				world.ekimusActivityTask.cancel(false);
			}
			
			world.conquestEnded = true;
			
			inst.despawnByGroup("soi_hoi_attack_wards");
			inst.despawnByGroup("soi_hoi_attack_mob_1");
			inst.despawnByGroup("soi_hoi_attack_mob_2");
			inst.despawnByGroup("soi_hoi_attack_mob_3");
			inst.despawnByGroup("soi_hoi_attack_mob_4");
			inst.despawnByGroup("soi_hoi_attack_mob_5");
			inst.despawnByGroup("soi_hoi_attack_mob_6");
			inst.despawnByGroup("soi_hoi_attack_bosses");
			if (world.ekimus != null && !world.ekimus.isDead())
			{
				world.ekimus.deleteMe();
			}
			
			for (final Npc npc : world.hounds)
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
			}
			
			if (win)
			{
				finishInstance(world, 900000, true);
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.CONGRATULATIONS_YOU_HAVE_SUCCEEDED_AT_S1_S2_THE_INSTANCE_WILL_SHORTLY_EXPIRE, 2, 1, 8000);
				msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
				msg.addStringParameter("#" + NpcStringId.ATTACK.getId());
				broadCastPacket(world, msg);
				handleReenterTime(world);
			}
			else
			{
				finishInstance(world, 900000, false);
				final ExShowScreenMessage msg = new ExShowScreenMessage(NpcStringId.YOU_HAVE_FAILED_AT_S1_S2_THE_INSTANCE_WILL_SHORTLY_EXPIRE, 2, 1, 8000);
				msg.addStringParameter("#" + NpcStringId.HEART_OF_IMMORTALITY.getId());
				msg.addStringParameter("#" + NpcStringId.ATTACK.getId());
				broadCastPacket(world, msg);
			}
			
			for (final Npc npc : inst.getNpcs())
			{
				if (npc != null)
				{
					if (npc.getId() == 18708 || npc.getId() == 32535 || npc.getId() == 18710)
					{
						npc.deleteMe();
					}
				}
			}
		}
	}

	protected void broadCastPacket(HIAWorld world, GameServerPacket packet)
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
		new HeartInfinityAttack(HeartInfinityAttack.class.getSimpleName(), "instances");
	}
}
