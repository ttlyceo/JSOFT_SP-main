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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.geometry.Polygon;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.MountType;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.instance.NpcInstance;
import l2e.gameserver.model.actor.instance.QuestGuardInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.model.stats.NpcStats;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.EventTrigger;
import l2e.gameserver.network.serverpackets.ExChangeClientEffectInfo;
import l2e.gameserver.network.serverpackets.ExSendUIEvent;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;

/**
 * Rework by LordWinter 07.02.2020
 */
public class FreyaBattleHard extends AbstractReflection
{
	protected class IQCNBWorld extends ReflectionWorld
	{
		public List<Player> playersInside = new ArrayList<>();
		public List<Npc> knightStatues = new ArrayList<>();
		public List<Attackable> spawnedMobs = new CopyOnWriteArrayList<>();
		public NpcInstance controller = null;
		public GrandBossInstance freya = null;
		public QuestGuardInstance supp_Jinia = null;
		public QuestGuardInstance supp_Kegor = null;
		public boolean isSupportActive = false;
		public boolean canSpawnMobs = false;
		public ScheduledFuture<?> firstStageGuardSpawn;
		public ScheduledFuture<?> secondStageGuardSpawn;
		public ScheduledFuture<?> thirdStageGuardSpawn;
	}

	private static final SkillHolder SUICIDE_BREATH = new SkillHolder(6300, 1);
	private static final SkillHolder JINIA_SUPPORT = new SkillHolder(6288, 1);
	private static final SkillHolder KEGOR_SUPPORT = new SkillHolder(6289, 1);
	private static final SkillHolder ANTI_STRIDER = new SkillHolder(4258, 1);

	private final ZoneType _zone = ZoneManager.getInstance().getZoneById(20503);
	private static final Location MIDDLE_POINT = new Location(114730, -114805, -11200);
	private static SpawnTerritory CENTRAL_ROOM = new SpawnTerritory().add(new Polygon().add(114264, -113672).add(113640, -114344).add(113640, -115240).add(114264, -115912).add(115176, -115912).add(115800, -115272).add(115800, -114328).add(115192, -113672).setZmax(-11225).setZmin(-11225));

	private static final Location[] STATUES_STAGE_1_LOC =
	{
	        new Location(113845, -116091, -11168, 8264), new Location(113381, -115622, -11168, 8264), new Location(113380, -113978, -11168, -8224), new Location(113845, -113518, -11168, -8224), new Location(115591, -113516, -11168, -24504), new Location(116053, -113981, -11168, -24504), new Location(116061, -115611, -11168, 24804), new Location(115597, -116080, -11168, 24804),
	};

	private static final Location[] STATUES_STAGE_2_LOC =
	{
	        new Location(112942, -115480, -10960, 52), new Location(112940, -115146, -10960, 52), new Location(112945, -114453, -10960, 52), new Location(112945, -114123, -10960, 52), new Location(116497, -114117, -10960, 32724), new Location(116499, -114454, -10960, 32724), new Location(116501, -115145, -10960, 32724), new Location(116502, -115473, -10960, 32724),
	};

	private static int[] EMMITERS =
	{
	        23140202, 23140204, 23140206, 23140208, 23140212, 23140214, 23140216,
	};

	private FreyaBattleHard()
	{
		super(FreyaBattleHard.class.getSimpleName(), "instances");

		addStartNpc(32762, 18851, 18850);
		addTalkId(32762, 32781, 18851);
		addAttackId(29177, 29180, 18854, 18856);
		addKillId(29177, 25700, 29180, 18856);
		addSpawnId(25700, 29180, 18856);
		addSpellFinishedId(18854);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enterHard"))
		{
			enterInstance(player, npc);
		}
		else
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpworld instanceof IQCNBWorld)
			{
				final IQCNBWorld world = (IQCNBWorld) tmpworld;

				switch (event)
				{
					case "openDoor" :
					{
						if (npc.isScriptValue(0))
						{
							npc.setScriptValue(1);
							world.getReflection().openDoor(23140101);
							world.controller = (NpcInstance) addSpawn(18919, new Location(114394, -112383, -11200), false, 0, true, world.getReflection());
							for (final Location loc : STATUES_STAGE_1_LOC)
							{
								final Npc statue = addSpawn(18919, loc, false, 0, false, world.getReflection());
								world.knightStatues.add(statue);
							}
							startQuestTimer("STAGE_1_MOVIE", 60000, world.controller, null);
						}
						break;
					}
					case "portInside" :
					{
						if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
						{
							if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
							{
								for (final Player member : player.getParty().getMembers())
								{
									if (member != null)
									{
										if (member.getObjectId() == player.getObjectId())
										{
											continue;
										}
										
										if (!Util.checkIfInRange(1000, player, member, true) || member.getReflectionId() != world.getReflectionId())
										{
											continue;
										}
										
										if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
										{
											continue;
										}
										teleportPlayer(member, new Location(114694, -113700, -11200), world.getReflection());
									}
								}
							}
						}
						teleportPlayer(player, new Location(114694, -113700, -11200), world.getReflection());
						break;
					}
					case "STAGE_1_MOVIE" :
					{
						world.getReflection().closeDoor(23140101);
						world.setStatus(1);
						manageMovie(world, 15);
						startQuestTimer("STAGE_1_START", 53500, world.controller, null);
						break;
					}
					case "STAGE_1_START" :
					{
						world.freya = (GrandBossInstance) addSpawn(29177, new Location(114720, -117085, -11088, 15956), false, 0, true, world.getReflection());
						manageScreenMsg(world, NpcStringId.BEGIN_STAGE_1);
						startQuestTimer("STAGE_1_SPAWN", 2000, world.freya, null);
						break;
					}
					case "STAGE_1_SPAWN" :
					{
						world.canSpawnMobs = true;
						notifyEvent("START_SPAWN", world.controller, null);
						if (!world.freya.isInCombat())
						{
							manageScreenMsg(world, NpcStringId.FREYA_HAS_STARTED_TO_MOVE);
							world.freya.setRunning();
							world.freya.getAI().setIntention(CtrlIntention.MOVING, new Location(114730, -114805, -11200), 0);
						}
						
						final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
						if (inst != null)
						{
							world.firstStageGuardSpawn = ThreadPoolManager.getInstance().scheduleAtFixedRate(new GuardSpawnTask(1, world), 2000L, (inst.getParams().getInteger("guardsInterval") * 1000L));
						}
						break;
					}
					case "STAGE_1_FINISH" :
					{
						world.canSpawnMobs = false;
						if (world.firstStageGuardSpawn != null)
						{
							world.firstStageGuardSpawn.cancel(true);
						}
						manageDespawnMinions(world);
						manageMovie(world, 16);
						startQuestTimer("STAGE_1_PAUSE", 24100 - 1000, world.controller, null);
						break;
					}
					case "STAGE_1_PAUSE" :
					{
						world.freya = (GrandBossInstance) addSpawn(29178, new Location(114723, -117502, -10672, 15956), false, 0, true, world.getReflection());
						world.freya.setIsInvul(true);
						world.freya.block();
						world.freya.disableCoreAI(true);
						manageTimer(world, 60, NpcStringId.TIME_REMAINING_UNTIL_NEXT_BATTLE);
						world.setStatus(2);
						startQuestTimer("STAGE_2_START", 60000, world.controller, null);
						break;
					}
					case "STAGE_2_START" :
					{
						world.canSpawnMobs = true;
						notifyEvent("START_SPAWN", world.controller, null);
						manageScreenMsg(world, NpcStringId.BEGIN_STAGE_2);
						final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
						if (inst != null)
						{
							world.secondStageGuardSpawn = ThreadPoolManager.getInstance().scheduleAtFixedRate(new GuardSpawnTask(2, world), 2000L, (inst.getParams().getInteger("guardsInterval") * 1000L));
						}
						final int timeLimit = inst.getParams().getInteger("glakiasTimeLimit");
						startQuestTimer("STAGE_2_FAILED", (timeLimit * 1000), world.controller, null);
						manageTimer(world, timeLimit, NpcStringId.BATTLE_END_LIMIT_TIME);
						world.controller.getVariables().set("TIMER_END", System.currentTimeMillis() + (timeLimit * 1000));
						break;
					}
					case "STAGE_2_MOVIE" :
					{
						manageBlockMinions(world);
						manageMovie(world, 23);
						startQuestTimer("STAGE_2_GLAKIAS", 7000, world.controller, null);
						break;
					}
					case "STAGE_2_GLAKIAS" :
					{
						manageUnblockMinions(world);
						for (final Location loc : STATUES_STAGE_2_LOC)
						{
							final Npc statue = addSpawn(18919, loc, false, 0, false, world.getReflection());
							world.knightStatues.add(statue);
							startQuestTimer("SPAWN_KNIGHT", 5000, statue, null);
						}
						addSpawn(25700, new Location(114707, -114799, -11199, 15956), false, 0, true, world.getReflection());
						startQuestTimer("SHOW_GLAKIAS_TIMER", 3000, world.controller, null);
						break;
					}
					case "STAGE_2_FAILED" :
					{
						if (world.getStatus() <= 3)
						{
							doCleanup(world);
							manageMovie(world, 22);
							startQuestTimer("STAGE_2_FAILED2", 22000, npc, null);
						}
						break;
					}
					case "STAGE_2_FAILED2" :
					{
						final var r = world.getReflection();
						if (r != null)
						{
							r.collapse();
						}
						break;
					}
					case "STAGE_3_MOVIE" :
					{
						if (world.freya != null)
						{
							world.freya.deleteMe();
						}
						manageMovie(world, 17);
						startQuestTimer("STAGE_3_START", 21500, world.controller, null);
						break;
					}
					case "STAGE_3_START" :
					{
						for (final Player players : world.playersInside)
						{
							if ((player != null) && (player.getReflectionId() == world.getReflectionId()))
							{
								players.broadcastPacket(ExChangeClientEffectInfo.STATIC_FREYA_DESTROYED);

								for (final int emmiterId : EMMITERS)
								{
									players.sendPacket(new EventTrigger(emmiterId, true));
								}
							}
						}
						manageScreenMsg(world, NpcStringId.BEGIN_STAGE_3);
						world.canSpawnMobs = true;
						final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
						if (inst != null)
						{
							world.thirdStageGuardSpawn = ThreadPoolManager.getInstance().scheduleAtFixedRate(new GuardSpawnTask(3, world), 2000L, (inst.getParams().getInteger("guardsInterval") * 1000L));
						}
						world.freya = (GrandBossInstance) addSpawn(29180, new Location(114720, -117085, -11088, 15956), false, 0, true, world.getReflection());
						notifyEvent("START_SPAWN", world.controller, null);
						if (!world.freya.isInCombat())
						{
							manageScreenMsg(world, NpcStringId.FREYA_HAS_STARTED_TO_MOVE);
							world.freya.setRunning();
							world.freya.getAI().setIntention(CtrlIntention.MOVING, new Location(114730, -114805, -11200), 0);
						}
						break;
					}
					case "SPAWN_SUPPORT" :
					{
						manageUnblockMinions(world);
						for (final Player players : world.playersInside)
						{
							if (players == null)
							{
								continue;
							}
							players.setIsInvul(false);
							players.unblock();
						}
						world.freya.setIsInvul(false);
						world.freya.unblock();
						world.canSpawnMobs = true;
						world.freya.disableCoreAI(false);
						manageScreenMsg(world, NpcStringId.BEGIN_STAGE_4);
						world.supp_Jinia = (QuestGuardInstance) addSpawn(18850, new Location(114751, -114781, -11205), false, 0, true, world.getReflection());
						world.supp_Jinia.setRunning();
						world.supp_Jinia.setIsInvul(true);
						world.supp_Jinia.setCanReturnToSpawnPoint(false);

						world.supp_Kegor = (QuestGuardInstance) addSpawn(18851, new Location(114659, -114796, -11205), false, 0, true, world.getReflection());
						world.supp_Kegor.setRunning();
						world.supp_Kegor.setIsInvul(true);
						world.supp_Kegor.setCanReturnToSpawnPoint(false);
						startQuestTimer("GIVE_SUPPORT", 1000, world.controller, null);
						break;
					}
					case "GIVE_SUPPORT" :
					{
						if (world.isSupportActive)
						{
							world.supp_Jinia.doCast(JINIA_SUPPORT.getSkill());
							world.supp_Kegor.doCast(KEGOR_SUPPORT.getSkill());
						}
						break;
					}
					case "START_SPAWN" :
					{
						for (final Npc statues : world.knightStatues)
						{
							notifyEvent("SPAWN_KNIGHT", statues, null);
						}
						break;
					}
					case "SPAWN_KNIGHT" :
					{
						if (world.canSpawnMobs)
						{
							final Location loc = new Location(MIDDLE_POINT.getX() + getRandom(-1000, 1000), MIDDLE_POINT.getY() + getRandom(-1000, 1000), MIDDLE_POINT.getZ());
							final Attackable knight = (Attackable) addSpawn(18856, npc.getLocation(), false, 0, false, world.getReflection());
							knight.getSpawn().setLocation(loc);
							world.spawnedMobs.add(knight);
						}
						break;
					}
					case "FIND_TARGET" :
					{
						if (npc.isDead())
						{
							break;
						}
						manageRandomAttack(world, (Attackable) npc);
						break;
					}
					case "ELEMENTAL_KILLED" :
					{
						if (npc.getVariables().getInteger("SUICIDE_ON") == 1)
						{
							npc.setTarget(npc);
							npc.doCast(SUICIDE_BREATH.getSkill());
						}
						break;
					}
					case "FINISH_WORLD" :
					{
						if (world.freya != null)
						{
							world.freya.deleteMe();
						}

						for (final Player players : world.playersInside)
						{
							if ((players != null) && (players.getReflectionId() == world.getReflectionId()))
							{
								players.broadcastPacket(ExChangeClientEffectInfo.STATIC_FREYA_DEFAULT);
							}
						}
						break;
					}
					case "SHOW_GLAKIAS_TIMER" :
					{
						final int time = (int) ((world.controller.getVariables().getLong("TIMER_END", 0) - System.currentTimeMillis()) / 1000);
						manageTimer(world, time, NpcStringId.BATTLE_END_LIMIT_TIME);
						break;
					}
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onSpawn(Npc npc)
	{
		((Attackable) npc).setOnKillDelay(0);
		return super.onSpawn(npc);
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());

		if (tmpworld instanceof IQCNBWorld)
		{
			final IQCNBWorld world = (IQCNBWorld) tmpworld;

			switch (npc.getId())
			{
				case 29180 :
				{
					if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.2)) && !world.isSupportActive)
					{
						world.isSupportActive = true;
						world.freya.setIsInvul(true);
						world.freya.block();
						world.freya.disableCoreAI(true);
						manageBlockMinions(world);
						world.canSpawnMobs = false;
						for (final Player players : world.playersInside)
						{
							if (players == null || players.isDead())
							{
								continue;
							}
							players.setIsInvul(true);
							players.block();
							players.abortAttack();
						}
						manageMovie(world, 18);
						startQuestTimer("SPAWN_SUPPORT", 27000, world.controller, null);
					}

					if ((attacker.getMountType() == MountType.STRIDER) && (attacker.getFirstEffect(ANTI_STRIDER.getId()) == null) && !npc.isCastingNow())
					{
						if (!npc.isSkillDisabled(ANTI_STRIDER.getSkill()))
						{
							npc.setTarget(attacker);
							npc.doCast(ANTI_STRIDER.getSkill());
						}
					}
					break;
				}
				case 18854 :
				{
					if ((npc.getCurrentHp() < (npc.getMaxHp() / 20)) && (npc.getVariables().getInteger("SUICIDE_ON", 0) == 0))
					{
						npc.getVariables().set("SUICIDE_ON", 1);
						startQuestTimer("ELEMENTAL_KILLED", 1000, npc, null);
					}
					break;
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}

	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());

		if (tmpworld instanceof IQCNBWorld)
		{
			switch (npc.getId())
			{
				case 18854 :
				{
					if (skill == SUICIDE_BREATH.getSkill())
					{
						npc.doDie(npc);
					}
					break;
				}
			}
		}
		return super.onSpellFinished(npc, player, skill);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof IQCNBWorld)
		{
			final IQCNBWorld world = (IQCNBWorld) tmpworld;

			switch (npc.getId())
			{
				case 29177 :
				{
					world.freya.deleteMe();
					world.freya = null;
					notifyEvent("STAGE_1_FINISH", world.controller, null);
					break;
				}
				case 25700 :
				{
					if (world.secondStageGuardSpawn != null)
					{
						world.secondStageGuardSpawn.cancel(true);
					}
					manageDespawnMinions(world);
					manageTimer(world, 60, NpcStringId.TIME_REMAINING_UNTIL_NEXT_BATTLE);
					cancelQuestTimer("STAGE_2_FAILED", world.controller, null);
					startQuestTimer("STAGE_3_MOVIE", 60000, world.controller, null);
					world.setStatus(4);
					break;
				}
				case 29180 :
				{
					world.canSpawnMobs = false;
					world.isSupportActive = false;
					doCleanup(world);
					manageMovie(world, 19);
					finishInstance(world, true);
					cancelQuestTimer("GIVE_SUPPORT", world.controller, null);
					if (world.supp_Jinia != null)
					{
						world.supp_Jinia.deleteMe();
						world.supp_Jinia = null;
					}
					
					if (world.supp_Kegor != null)
					{
						world.supp_Kegor.deleteMe();
						world.supp_Kegor = null;
					}
					startQuestTimer("FINISH_WORLD", 300000, world.controller, null);
					break;
				}
				case 18856 :
				{
					final NpcStats var = world.controller.getVariables();
					int knightCount = var.getInteger("KNIGHT_COUNT");

					if ((knightCount < 10) && (world.isStatus(2)))
					{
						knightCount++;
						var.set("KNIGHT_COUNT", knightCount);

						if (knightCount == 10)
						{
							notifyEvent("STAGE_2_MOVIE", world.controller, null);
							world.setStatus(3);
						}
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}

	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new IQCNBWorld(), 144))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			for (final Player players : ((IQCNBWorld) world).playersInside)
			{
				if (players != null)
				{
					players.broadcastPacket(ExChangeClientEffectInfo.STATIC_FREYA_DEFAULT);
					for (final int emmiterId : EMMITERS)
					{
						players.sendPacket(new EventTrigger(emmiterId, false));
					}
				}
			}
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			((IQCNBWorld) world).playersInside.add(player);
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
			final Location teleLoc = world.isStatus(4) ? new Location(114694, -113700, -11200) : template.getTeleportCoord();
			player.getAI().setIntention(CtrlIntention.IDLE);
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}

	private void manageRandomAttack(IQCNBWorld world, Attackable mob)
	{
		final List<Player> players = new ArrayList<>();
		for (final Player player : world.playersInside)
		{
			if ((player != null) && !player.isDead() && (player.getReflectionId() == world.getReflectionId()) && !player.isInvisible())
			{
				players.add(player);
			}
		}

		if (players.size() > 0)
		{
			final Player target = players.get(Rnd.get(players.size()));
			mob.addDamageHate(target, 0, 999);
			mob.setRunning();
			mob.getAI().setIntention(CtrlIntention.ATTACK, target);
		}
		else
		{
			startQuestTimer("FIND_TARGET", 10000, mob, null);
		}
	}

	private void manageDespawnMinions(IQCNBWorld world)
	{
		if (world != null)
		{
			for (final Attackable mob : world.spawnedMobs)
			{
				if (mob != null)
				{
					cancelQuestTimers(mob);
					if (!mob.isDead())
					{
						mob.doDie(mob);
					}
				}
			}
		}
	}

	protected void manageBlockMinions(IQCNBWorld world)
	{
		for (final Attackable mob : world.spawnedMobs)
		{
			if ((mob != null) && !mob.isDead())
			{
				mob.block();
				mob.disableCoreAI(true);
				mob.abortAttack();
			}
		}
	}

	protected void manageUnblockMinions(IQCNBWorld world)
	{
		for (final Attackable mob : world.spawnedMobs)
		{
			if ((mob != null) && !mob.isDead())
			{
				mob.unblock();
				mob.disableCoreAI(false);
			}
		}
	}

	private void manageTimer(IQCNBWorld world, int time, NpcStringId npcStringId)
	{
		for (final Player players : world.playersInside)
		{
			if ((players != null) && (players.getReflectionId() == world.getReflectionId()))
			{
				players.sendPacket(new ExSendUIEvent(players, false, false, time, 0, npcStringId));
			}
		}
	}

	private void manageScreenMsg(IQCNBWorld world, NpcStringId stringId)
	{
		final GameServerPacket packet = new ExShowScreenMessage(stringId, 2, 6000);
		for (final Player player : world.playersInside)
		{
			if ((player != null) && (player.getReflectionId() == world.getReflectionId()))
			{
				player.sendPacket(packet);
			}
		}
	}

	private void manageMovie(IQCNBWorld world, int movie)
	{
		for (final Player player : world.playersInside)
		{
			if ((player != null) && (player.getReflectionId() == world.getReflectionId()))
			{
				player.showQuestMovie(movie);
			}
		}
	}

	private class GuardSpawnTask implements Runnable
	{
		private int _mode, _knightsMin, _knightsMax, _breathMin, _breathMax;
		private final IQCNBWorld _world;

		GuardSpawnTask(int mode, IQCNBWorld world)
		{
			_mode = mode;
			_world = world;
			if ((_mode < 1) || (_mode > 3))
			{
				_mode = 1;
			}
		}

		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				boolean canSpawn = false;
				final ReflectionWorld instance = ReflectionManager.getInstance().getWorld(_world.getReflectionId());
				if ((instance != null) && (instance.getAllowed() != null))
				{
					for (final int objectId : instance.getAllowed())
					{
						final Player activeChar = GameObjectsStorage.getPlayer(objectId);
						if (activeChar != null && activeChar.getReflectionId() == _world.getReflectionId())
						{
							if (_zone != null && _zone.isInsideZone(activeChar))
							{
								continue;
							}
							
							if (!activeChar.isDead())
							{
								canSpawn = true;
								break;
							}
						}
					}
				}
				
				if (!_world.canSpawnMobs)
				{
					canSpawn = false;
				}
				
				if (canSpawn)
				{
					
					switch (_mode)
					{
						case 1 :
							final String[] stage1 = inst.getParams().getString("guardStage1").split(";");
							_knightsMin = Integer.parseInt(stage1[0]);
							_knightsMax = Integer.parseInt(stage1[1]);
							_breathMin = Integer.parseInt(stage1[2]);
							_breathMax = Integer.parseInt(stage1[3]);
							break;
						case 2 :
							final String[] stage2 = inst.getParams().getString("guardStage2").split(";");
							_knightsMin = Integer.parseInt(stage2[0]);
							_knightsMax = Integer.parseInt(stage2[1]);
							_breathMin = Integer.parseInt(stage2[2]);
							_breathMax = Integer.parseInt(stage2[3]);
							break;
						case 3 :
							final String[] stage3 = inst.getParams().getString("guardStage3").split(";");
							_knightsMin = Integer.parseInt(stage3[0]);
							_knightsMax = Integer.parseInt(stage3[1]);
							_breathMin = Integer.parseInt(stage3[2]);
							_breathMax = Integer.parseInt(stage3[3]);
							break;
					}
					
					for (int i = 0; i < Rnd.get(_knightsMin, _knightsMax); i++)
					{
						final Attackable knight = (Attackable) addSpawn(18856, SpawnTerritory.getRandomLoc(CENTRAL_ROOM, false), false, 0, false, _world.getReflection());
						_world.spawnedMobs.add(knight);
					}
					for (int i = 0; i < Rnd.get(_breathMin, _breathMax); i++)
					{
						final Attackable breath = (Attackable) addSpawn(18854, SpawnTerritory.getRandomLoc(CENTRAL_ROOM, false), false, 0, false, _world.getReflection());
						_world.spawnedMobs.add(breath);
					}
					if (Rnd.chance(60))
					{
						for (int i = 0; i < Rnd.get(1, 3); i++)
						{
							final Attackable glacier = (Attackable) addSpawn(18853, SpawnTerritory.getRandomLoc(CENTRAL_ROOM, false), false, 0, false, _world.getReflection());
							_world.spawnedMobs.add(glacier);
						}
					}
				}
			}
		}
	}

	protected void doCleanup(IQCNBWorld world)
	{
		manageDespawnMinions(world);
		if (world.firstStageGuardSpawn != null)
		{
			world.firstStageGuardSpawn.cancel(true);
		}
		if (world.secondStageGuardSpawn != null)
		{
			world.secondStageGuardSpawn.cancel(true);
		}
		if (world.thirdStageGuardSpawn != null)
		{
			world.thirdStageGuardSpawn.cancel(true);
		}
	}

	public static void main(String[] args)
	{
		new FreyaBattleHard();
	}
}