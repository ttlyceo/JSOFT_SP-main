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
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.ReflectionParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate.ReflectionEntryType;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.spawn.Spawner;

/**
 * Rework by LordWinter 10.02.2020
 */
public class Kamaloka extends AbstractReflection
{
	private static final boolean STEALTH_SHAMAN = true;

	private static final int[] INSTANCE_IDS =
	{
	        57, 58, 73, 60, 61, 74, 63, 64, 75, 66, 67, 76, 69, 70, 77, 72, 78, 79, 134, 59, 62, 65, 68, 71
	};

	private static final int[] BUFFS_WHITELIST =
	{
	        4322, 4323, 4324, 4325, 4326, 4327, 4328, 4329, 4330, 4331, 5632, 5637, 5950
	};

	private static final int[][] FIRST_ROOM =
	{
	        null, null,
	        {
	                22485, 22486, 5699, 1
			}, null, null,
			{
			        22488, 22489, 5699, 2
			},
			null,
			null,
			{
			        22491, 22492, 5699, 3
			},
			null,
			null,
			{
			        22494, 22495, 5699, 4
			},
			null,
			null,
			{
			        22497, 22498, 5699, 5
			},
			null,
			{
			        22500, 22501, 5699, 6
			},
			{
			        22503, 22504, 5699, 7
			},
			{
			        25706, 25707, 5699, 7
			}, null, null, null, null, null
	};

	private static final int[][][] FIRST_ROOM_SPAWNS =
	{
	                null,
	                null,
	                {
	                                {
	                                                -12381,
	                                                -174973,
	                                                -10955
					},
					{
					                -12413,
					                -174905,
					                -10955
					},
					{
					                -12377,
					                -174838,
					                -10953
					},
					{
					                -12316,
					                -174903,
					                -10953
					},
					{
					                -12326,
					                -174786,
					                -10953
					},
					{
					                -12330,
					                -175024,
					                -10953
					},
					{
					                -12211,
					                -174900,
					                -10955
					},
					{
					                -12238,
					                -174849,
					                -10953
					},
					{
					                -12233,
					                -174954,
					                -10953
					}
			},
			null,
			null,
			{
			                {
			                                -12381,
			                                -174973,
			                                -10955
					},
					{
					                -12413,
					                -174905,
					                -10955
					},
					{
					                -12377,
					                -174838,
					                -10953
					},
					{
					                -12316,
					                -174903,
					                -10953
					},
					{
					                -12326,
					                -174786,
					                -10953
					},
					{
					                -12330,
					                -175024,
					                -10953
					},
					{
					                -12211,
					                -174900,
					                -10955
					},
					{
					                -12238,
					                -174849,
					                -10953
					},
					{
					                -12233,
					                -174954,
					                -10953
					}
			},
			null,
			null,
			{
			                {
			                                -12381,
			                                -174973,
			                                -10955
					},
					{
					                -12413,
					                -174905,
					                -10955
					},
					{
					                -12377,
					                -174838,
					                -10953
					},
					{
					                -12316,
					                -174903,
					                -10953
					},
					{
					                -12326,
					                -174786,
					                -10953
					},
					{
					                -12330,
					                -175024,
					                -10953
					},
					{
					                -12211,
					                -174900,
					                -10955
					},
					{
					                -12238,
					                -174849,
					                -10953
					},
					{
					                -12233,
					                -174954,
					                -10953
					}
			},
			null,
			null,
			{
			                {
			                                -12381,
			                                -174973,
			                                -10955
					},
					{
					                -12413,
					                -174905,
					                -10955
					},
					{
					                -12377,
					                -174838,
					                -10953
					},
					{
					                -12316,
					                -174903,
					                -10953
					},
					{
					                -12326,
					                -174786,
					                -10953
					},
					{
					                -12330,
					                -175024,
					                -10953
					},
					{
					                -12211,
					                -174900,
					                -10955
					},
					{
					                -12238,
					                -174849,
					                -10953
					},
					{
					                -12233,
					                -174954,
					                -10953
					}
			},
			null,
			null,
			{
			                {
			                                -12381,
			                                -174973,
			                                -10955
					},
					{
					                -12413,
					                -174905,
					                -10955
					},
					{
					                -12377,
					                -174838,
					                -10953
					},
					{
					                -12316,
					                -174903,
					                -10953
					},
					{
					                -12326,
					                -174786,
					                -10953
					},
					{
					                -12330,
					                -175024,
					                -10953
					},
					{
					                -12211,
					                -174900,
					                -10955
					},
					{
					                -12238,
					                -174849,
					                -10953
					},
					{
					                -12233,
					                -174954,
					                -10953
					}
			},
			null,
			{
			                {
			                                -12381,
			                                -174973,
			                                -10955
					},
					{
					                -12413,
					                -174905,
					                -10955
					},
					{
					                -12377,
					                -174838,
					                -10953
					},
					{
					                -12316,
					                -174903,
					                -10953
					},
					{
					                -12326,
					                -174786,
					                -10953
					},
					{
					                -12330,
					                -175024,
					                -10953
					},
					{
					                -12211,
					                -174900,
					                -10955
					},
					{
					                -12238,
					                -174849,
					                -10953
					},
					{
					                -12233,
					                -174954,
					                -10953
					}
			},
			{
			                {
			                                -12381,
			                                -174973,
			                                -10955
					},
					{
					                -12413,
					                -174905,
					                -10955
					},
					{
					                -12377,
					                -174838,
					                -10953
					},
					{
					                -12316,
					                -174903,
					                -10953
					},
					{
					                -12326,
					                -174786,
					                -10953
					},
					{
					                -12330,
					                -175024,
					                -10953
					},
					{
					                -12211,
					                -174900,
					                -10955
					},
					{
					                -12238,
					                -174849,
					                -10953
					},
					{
					                -12233,
					                -174954,
					                -10953
					}
			},
			{
			                {
			                                20409,
			                                -174827,
			                                -10912
					},
					{
					                20409,
					                -174947,
					                -10912
					},
					{
					                20494,
					                -174887,
					                -10912
					},
					{
					                20494,
					                -174767,
					                -10912
					},
					{
					                20614,
					                -174887,
					                -10912
					},
					{
					                20579,
					                -174827,
					                -10912
					},
					{
					                20579,
					                -174947,
					                -10912
					},
					{
					                20494,
					                -175007,
					                -10912
					},
					{
					                20374,
					                -174887,
					                -10912
					}
			}, null, null, null, null, null
	};

	private static final int[][] SECOND_ROOM =
	{
	                null,
	                null,
	                {
	                                22487,
	                                5700,
	                                1
			},
			null,
			null,
			{
			                22490,
			                5700,
			                2
			},
			null,
			null,
			{
			                22493,
			                5700,
			                3
			},
			null,
			null,
			{
			                22496,
			                5700,
			                4
			},
			null,
			null,
			{
			                22499,
			                5700,
			                5
			},
			null,
			{
			                22502,
			                5700,
			                6
			},
			{
			                22505,
			                5700,
			                7
			},
			{
			                25708,
			                5700,
			                7
			}, null, null, null, null, null
	};

	private static final int[][][] SECOND_ROOM_SPAWNS =
	{
	                null,
	                null,
	                {
	                                {
	                                                -14547,
	                                                -174901,
	                                                -10690
					},
					{
					                -14543,
					                -175030,
					                -10690
					},
					{
					                -14668,
					                -174900,
					                -10690
					},
					{
					                -14538,
					                -174774,
					                -10690
					},
					{
					                -14410,
					                -174904,
					                -10690
					}
			},
			null,
			null,
			{
			                {
			                                -14547,
			                                -174901,
			                                -10690
					},
					{
					                -14543,
					                -175030,
					                -10690
					},
					{
					                -14668,
					                -174900,
					                -10690
					},
					{
					                -14538,
					                -174774,
					                -10690
					},
					{
					                -14410,
					                -174904,
					                -10690
					}
			},
			null,
			null,
			{
			                {
			                                -14547,
			                                -174901,
			                                -10690
					},
					{
					                -14543,
					                -175030,
					                -10690
					},
					{
					                -14668,
					                -174900,
					                -10690
					},
					{
					                -14538,
					                -174774,
					                -10690
					},
					{
					                -14410,
					                -174904,
					                -10690
					}
			},
			null,
			null,
			{
			                {
			                                -14547,
			                                -174901,
			                                -10690
					},
					{
					                -14543,
					                -175030,
					                -10690
					},
					{
					                -14668,
					                -174900,
					                -10690
					},
					{
					                -14538,
					                -174774,
					                -10690
					},
					{
					                -14410,
					                -174904,
					                -10690
					}
			},
			null,
			null,
			{
			                {
			                                -14547,
			                                -174901,
			                                -10690
					},
					{
					                -14543,
					                -175030,
					                -10690
					},
					{
					                -14668,
					                -174900,
					                -10690
					},
					{
					                -14538,
					                -174774,
					                -10690
					},
					{
					                -14410,
					                -174904,
					                -10690
					}
			},
			null,
			{
			                {
			                                -14547,
			                                -174901,
			                                -10690
					},
					{
					                -14543,
					                -175030,
					                -10690
					},
					{
					                -14668,
					                -174900,
					                -10690
					},
					{
					                -14538,
					                -174774,
					                -10690
					},
					{
					                -14410,
					                -174904,
					                -10690
					}
			},
			{
			                {
			                                -14547,
			                                -174901,
			                                -10690
					},
					{
					                -14543,
					                -175030,
					                -10690
					},
					{
					                -14668,
					                -174900,
					                -10690
					},
					{
					                -14538,
					                -174774,
					                -10690
					},
					{
					                -14410,
					                -174904,
					                -10690
					}
			},
			{
			                {
			                                18175,
			                                -174991,
			                                -10653
					},
					{
					                18070,
					                -174890,
					                -10655
					},
					{
					                18157,
					                -174886,
					                -10655
					},
					{
					                18249,
					                -174885,
					                -10653
					},
					{
					                18144,
					                -174821,
					                -10648
					}
			}, null, null, null, null, null
	};

	private static final int[][] MINIBOSS =
	{
	                null,
	                null,
	                {
	                                25616,
	                                -16874,
	                                -174900,
	                                -10427,
	                                5701,
	                                1
			},
			null,
			null,
			{
			                25617,
			                -16874,
			                -174900,
			                -10427,
			                5701,
			                2
			},
			null,
			null,
			{
			                25618,
			                -16874,
			                -174900,
			                -10427,
			                5701,
			                3
			},
			null,
			null,
			{
			                25619,
			                -16874,
			                -174900,
			                -10427,
			                5701,
			                4
			},
			null,
			null,
			{
			                25620,
			                -16874,
			                -174900,
			                -10427,
			                5701,
			                5
			},
			null,
			{
			                25621,
			                -16874,
			                -174900,
			                -10427,
			                5701,
			                6
			},
			{
			                25622,
			                -16874,
			                -174900,
			                -10427,
			                5701,
			                7
			},
			{
			                25709,
			                15828,
			                -174885,
			                -10384,
			                5701,
			                7
			}, null, null, null, null, null
	};

	private static final int[][] BOSS =
	{
	        {
	                18554, -88998, -220077, -7892
			},
			{
			        18555, -81891, -220078, -7893
			},
			{
			        29129, -20659, -174903, -9983
			},
			{
			        18558, -89183, -213564, -8110
			},
			{
			        18559, -81937, -213566, -8100
			},
			{
			        29132, -20659, -174903, -9983
			},
			{
			        18562, -89054, -206144, -8115
			},
			{
			        18564, -81937, -206077, -8100
			},
			{
			        29135, -20659, -174903, -9983
			},
			{
			        18566, -56281, -219859, -8115
			},
			{
			        18568, -49336, -220260, -8068
			},
			{
			        29138, -20659, -174903, -9983
			},
			{
			        18571, -56415, -212939, -8068
			},
			{
			        18573, -56281, -206140, -8115
			},
			{
			        29141, -20659, -174903, -9983
			},
			{
			        18577, -49084, -206140, -8115
			},
			{
			        29144, -20659, -174903, -9983
			},
			{
			        29147, -20659, -174903, -9983
			},
			{
			        25710, 12047, -174887, -9944
			},
			{
			        18557, -74845, -220078, -7904, -16312
			},
			{
			        18561, -74835, -213542, -7888, -16408
			},
			{
			        18565, -74656, -206080, -8096, -16640
			},
			{
			        18570, -42015, -219881, -8128, -16248
			},
			{
			        18575, -49072, -206141, -8118, -16640
			},
	};

	private static final int[][] TELEPORTERS =
	{
	                null,
	                null,
	                {
	                                -10865,
	                                -174905,
	                                -10944
			},
			null,
			null,
			{
			                -10865,
			                -174905,
			                -10944
			},
			null,
			null,
			{
			                -10865,
			                -174905,
			                -10944
			},
			null,
			null,
			{
			                -10865,
			                -174905,
			                -10944
			},
			null,
			null,
			{
			                -10865,
			                -174905,
			                -10944
			},
			null,
			{
			                -10865,
			                -174905,
			                -10944
			},
			{
			                -10865,
			                -174905,
			                -10944
			},
			{
			                21837,
			                -174885,
			                -10904
			}, null, null, null, null, null
	};

	private class KamaWorld extends ReflectionWorld
	{
		public int index;
		public int shaman = 0;
		public List<Spawner> firstRoom;
		public List<Integer> secondRoom;
		public int miniBoss = 0;
		public Npc boss = null;

		public KamaWorld()
		{
		}
	}

	public Kamaloka(String name, String descr)
	{
		super(name, descr);

		addFirstTalkId(32496, 4314);
		addStartNpc(30332, 30071, 30916, 30196, 31981, 31340);
		addTalkId(30332, 30071, 30916, 30196, 31981, 31340, 32496, 4314);

		for (final int[] mob : FIRST_ROOM)
		{
			if (mob != null)
			{
				if (STEALTH_SHAMAN)
				{
					addKillId(mob[1]);
				}
				else
				{
					addKillId(mob[0]);
				}
			}
		}

		for (final int[] mob : SECOND_ROOM)
		{
			if (mob != null)
			{
				addKillId(mob[0]);
			}
		}

		for (final int[] mob : MINIBOSS)
		{
			if (mob != null)
			{
				addKillId(mob[0]);
			}
		}

		for (final int[] mob : BOSS)
		{
			addKillId(mob[0]);
		}
	}

	private static final void removeBuffs(Creature player)
	{
		for (final Effect e : player.getAllEffects())
		{
			if (e == null)
			{
				continue;
			}
			final Skill skill = e.getSkill();
			if (skill.isDebuff() || skill.isStayAfterDeath())
			{
				continue;
			}
			if (Arrays.binarySearch(BUFFS_WHITELIST, skill.getId()) >= 0)
			{
				continue;
			}
			e.exit();
		}
		
		if (player.getSummon() != null)
		{
			for (final Effect e : player.getSummon().getAllEffects())
			{
				if (e == null)
				{
					continue;
				}
				final Skill skill = e.getSkill();
				if (skill.isDebuff() || skill.isStayAfterDeath())
				{
					continue;
				}
				if (Arrays.binarySearch(BUFFS_WHITELIST, skill.getId()) >= 0)
				{
					continue;
				}
				e.exit();
			}
		}
	}

	private final synchronized void enterInstance(Player player, Npc npc, int index)
	{
		if (enterInstance(player, npc, new KamaWorld(), INSTANCE_IDS[index]))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((KamaWorld) world).index = index;
			spawnKama((KamaWorld) world);
		}
	}
	
	@Override
	protected void handleRemoveBuffs(Player player)
	{
		removeBuffs(player);
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

	private final void spawnKama(KamaWorld world)
	{
		int[] npcs;
		int[][] spawns;
		Npc npc;
		final int index = world.index;

		npcs = FIRST_ROOM[index];
		spawns = FIRST_ROOM_SPAWNS[index];
		if (npcs != null)
		{
			world.firstRoom = new ArrayList<>(spawns.length - 1);
			final int shaman = getRandom(spawns.length);

			for (int i = 0; i < spawns.length; i++)
			{
				if (i == shaman)
				{
					npc = addSpawn(STEALTH_SHAMAN ? npcs[1] : npcs[0], spawns[i][0], spawns[i][1], spawns[i][2], 0, false, 0, false, world.getReflection());
					world.shaman = npc.getObjectId();
				}
				else
				{
					npc = addSpawn(npcs[1], spawns[i][0], spawns[i][1], spawns[i][2], 0, false, 0, false, world.getReflection());
					final Spawner spawn = npc.getSpawn();
					spawn.setRespawnDelay(25);
					spawn.setAmount(1);
					spawn.startRespawn();
					world.firstRoom.add(spawn);
				}
				npc.setIsNoRndWalk(true);
			}
		}
		npcs = SECOND_ROOM[index];
		spawns = SECOND_ROOM_SPAWNS[index];
		if (npcs != null)
		{
			world.secondRoom = new ArrayList<>(spawns.length);

			for (final int[] spawn : spawns)
			{
				npc = addSpawn(npcs[0], spawn[0], spawn[1], spawn[2], 0, false, 0, false, world.getReflection());
				npc.setIsNoRndWalk(true);
				world.secondRoom.add(npc.getObjectId());
			}
		}

		if (MINIBOSS[index] != null)
		{
			npc = addSpawn(MINIBOSS[index][0], MINIBOSS[index][1], MINIBOSS[index][2], MINIBOSS[index][3], 0, false, 0, false, world.getReflection());
			npc.setIsNoRndWalk(true);
			world.miniBoss = npc.getObjectId();
		}

		if (TELEPORTERS[index] != null)
		{
			addSpawn(32496, TELEPORTERS[index][0], TELEPORTERS[index][1], TELEPORTERS[index][2], 0, false, 0, false, world.getReflection());
		}

		npc = addSpawn(BOSS[index][0], BOSS[index][1], BOSS[index][2], BOSS[index][3], 0, false, 0, false, world.getReflection());
		((MonsterInstance) npc).setOnKillDelay(100);
		world.boss = npc;
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("enter"))
		{
			final var st = new StringTokenizer(event, " ");
			st.nextToken();
			final var id = st.hasMoreTokens() ? st.nextToken() : null;
			if (id != null)
			{
				enterInstance(player, npc, Integer.parseInt(id));
			}
			return null;
		}
		
		if (npc == null)
		{
			return "";
		}

		try
		{
			enterInstance(player, npc, Integer.parseInt(event));
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
		return "";
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			newQuestState(player);
		}
		final int npcId = npc.getId();

		if (npcId == 32496 || npcId == 4314)
		{
			var canTalk = false;
			var foundTemplate = false;
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpworld != null && tmpworld instanceof KamaWorld)
			{
				final KamaWorld world = (KamaWorld) tmpworld;
				if (world != null)
				{
					final ReflectionTemplate template = ReflectionParser.getInstance().getReflectionId(INSTANCE_IDS[world.index]);
					if (template != null)
					{
						foundTemplate = true;
						if ((!player.isInParty() || (player.isInParty() && player.getParty().isLeader(player))) && (template.getEntryType() == ReflectionEntryType.SOLO || template.getEntryType() == ReflectionEntryType.SOLO_PARTY))
						{
							canTalk = true;
						}
						
						if (template.getEntryType() == ReflectionEntryType.PARTY && player.isInParty() && player.getParty().isLeader(player))
						{
							canTalk = true;
						}
					}
				}
			}
			
			if ((!foundTemplate && player.isInParty() && player.getParty().isLeader(player)) || (foundTemplate && canTalk))
			{
				if (tmpworld != null)
				{
					if (tmpworld.isAllowed(player.getObjectId()))
					{
						final Reflection inst = ReflectionManager.getInstance().getReflection(tmpworld.getReflectionId());
						if (player.getParty() != null)
						{
							for (final Player partyMember : player.getParty().getMembers())
							{
								if ((partyMember != null) && (partyMember.getReflectionId() == tmpworld.getReflectionId()))
								{
									teleportPlayer(partyMember, inst.getReturnLoc(), ReflectionManager.DEFAULT);
								}
							}
						}
						else
						{
							teleportPlayer(player, inst.getReturnLoc(), ReflectionManager.DEFAULT);
						}
					}
				}
			}
		}
		else
		{
			return npcId + ".htm";
		}
		return "";
	}

	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		var canTalk = false;
		var foundTemplate = false;
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof KamaWorld)
		{
			final KamaWorld world = (KamaWorld) tmpworld;
			if (world != null)
			{
				final ReflectionTemplate template = ReflectionParser.getInstance().getReflectionId(INSTANCE_IDS[world.index]);
				if (template != null)
				{
					foundTemplate = true;
					if ((!player.isInParty() || (player.isInParty() && player.getParty().isLeader(player))) && (template.getEntryType() == ReflectionEntryType.SOLO || template.getEntryType() == ReflectionEntryType.SOLO_PARTY))
					{
						canTalk = true;
					}
					
					if (template.getEntryType() == ReflectionEntryType.PARTY && player.isInParty() && player.getParty().isLeader(player))
					{
						canTalk = true;
					}
				}
			}
		}
		if (npc.getId() == 32496)
		{
			if ((!foundTemplate && player.isInParty() && player.getParty().isLeader(player)) || (foundTemplate && canTalk))
			{
				return "32496.htm";
			}
			return "32496-no.htm";
		}
		else if (npc.getId() == 4314)
		{
			if ((!foundTemplate && player.isInParty() && player.getParty().isLeader(player)) || (foundTemplate && canTalk))
			{
				return "4314.htm";
			}
			return "4314-no.htm";
		}
		return "";
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpWorld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpWorld instanceof KamaWorld)
		{
			final KamaWorld world = (KamaWorld) tmpWorld;
			final int objectId = npc.getObjectId();

			if (world.firstRoom != null)
			{
				if ((world.shaman != 0) && (world.shaman == objectId))
				{
					world.shaman = 0;
					for (final Spawner spawn : world.firstRoom)
					{
						if (spawn != null)
						{
							spawn.stopRespawn();
						}
					}
					world.firstRoom.clear();
					world.firstRoom = null;

					if (world.boss != null)
					{
						final int skillId = FIRST_ROOM[world.index][2];
						final int skillLvl = FIRST_ROOM[world.index][3];
						if ((skillId != 0) && (skillLvl != 0))
						{
							final Skill skill = SkillsParser.getInstance().getInfo(skillId, skillLvl);
							if (skill != null)
							{
								skill.getEffects(world.boss, world.boss, false);
							}
						}
					}
					return super.onKill(npc, player, isSummon);
				}
			}

			if (world.secondRoom != null)
			{
				boolean all = true;
				for (int i = 0; i < world.secondRoom.size(); i++)
				{
					if (world.secondRoom.get(i) == objectId)
					{
						world.secondRoom.set(i, 0);
					}
					else if (world.secondRoom.get(i) != 0)
					{
						all = false;
					}
				}

				if (all)
				{
					world.secondRoom.clear();
					world.secondRoom = null;

					if (world.boss != null)
					{
						final int skillId = SECOND_ROOM[world.index][1];
						final int skillLvl = SECOND_ROOM[world.index][2];
						if ((skillId != 0) && (skillLvl != 0))
						{
							final Skill skill = SkillsParser.getInstance().getInfo(skillId, skillLvl);
							if (skill != null)
							{
								skill.getEffects(world.boss, world.boss, false);
							}
						}
					}
					return super.onKill(npc, player, isSummon);
				}
			}

			if ((world.miniBoss != 0) && (world.miniBoss == objectId))
			{
				world.miniBoss = 0;

				if (world.boss != null)
				{
					final int skillId = MINIBOSS[world.index][4];
					final int skillLvl = MINIBOSS[world.index][5];
					if ((skillId != 0) && (skillLvl != 0))
					{
						final Skill skill = SkillsParser.getInstance().getInfo(skillId, skillLvl);
						if (skill != null)
						{
							skill.getEffects(world.boss, world.boss, false);
						}
					}
				}
				return super.onKill(npc, player, isSummon);
			}

			if ((world.boss != null) && (world.boss == npc))
			{
				world.boss = null;
				addSpawn(4314, BOSS[world.index][1], BOSS[world.index][2], BOSS[world.index][3], 0, false, 0, false, world.getReflection());
				finishInstance(world, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new Kamaloka(Kamaloka.class.getSimpleName(), "instances");
	}
}