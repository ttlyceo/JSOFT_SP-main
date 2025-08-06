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
package l2e.scripts.custom;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.Config;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;

/**
 * Updated by LordWinter 28.11.2023
 */
public class DragonVortex extends Quest
{
	private static final int[] RAIDS =
	{
	                25724,
	                25723,
	                25722,
	                25721,
	                25720,
	                25719,
	                25718,
	};

	private static Location[] BOSS_SPAWN_1 =
	{
	                new Location(91948, 113665, -3059),
	                new Location(92486, 113568, -3072),
	                new Location(92519, 114071, -3072),
	                new Location(91926, 114162, -3072)
	};

	private static Location[] BOSS_SPAWN_2 =
	{
	                new Location(108953, 112366, -3047),
	                new Location(108500, 112039, -3047),
	                new Location(108977, 111575, -3047),
	                new Location(109316, 112004, -3033)
	};

	private static Location[] BOSS_SPAWN_3 =
	{
	                new Location(109840, 125178, -3687),
	                new Location(110461, 125227, -3687),
	                new Location(110405, 125814, -3687),
	                new Location(109879, 125828, -3686)
	};

	private static Location[] BOSS_SPAWN_4 =
	{
	                new Location(121543, 113580, -3793),
	                new Location(120877, 113714, -3793),
	                new Location(120848, 113058, -3793),
	                new Location(121490, 113084, -3793)
	};

	public DragonVortex(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addFirstTalkId(32871, 32892, 32893, 32894);
		addStartNpc(32871, 32892, 32893, 32894);
		addTalkId(32871, 32892, 32893, 32894);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("Spawn"))
		{
			if (npc.getId() == 32871)
			{
				if (isBossAlive(npc))
				{
					return "32871-03.htm";
				}

				if (hasQuestItems(player, 17248))
				{
					takeItems(player, 17248, 1);
					final var bossSpawn = BOSS_SPAWN_1[getRandom(0, BOSS_SPAWN_1.length - 1)];
					if (bossSpawn != null)
					{
						addSpawn(RAIDS[getRandom(RAIDS.length)], bossSpawn.rnd(null, 50, 100, true), false, 0);
					}
					return "32871-01.htm";
				}
				return "32871-02.htm";
			}
			else if (npc.getId() == 32892)
			{
				if (isBossAlive(npc))
				{
					return "32871-03.htm";
				}

				if (hasQuestItems(player, 17248))
				{
					takeItems(player, 17248, 1);
					final var bossSpawn = BOSS_SPAWN_2[getRandom(0, BOSS_SPAWN_2.length - 1)];
					if (bossSpawn != null)
					{
						addSpawn(RAIDS[getRandom(RAIDS.length)], bossSpawn.rnd(null, 50, 100, true), false, 0);
					}
					return "32871-01.htm";
				}
				return "32871-02.htm";
			}
			else if (npc.getId() == 32893)
			{
				if (isBossAlive(npc))
				{
					return "32871-03.htm";
				}

				if (hasQuestItems(player, 17248))
				{
					takeItems(player, 17248, 1);
					final var bossSpawn = BOSS_SPAWN_3[getRandom(0, BOSS_SPAWN_3.length - 1)];
					if (bossSpawn != null)
					{
						addSpawn(RAIDS[getRandom(RAIDS.length)], bossSpawn.rnd(null, 50, 100, true), false, 0);
					}
					return "32871-01.htm";
				}
				return "32871-02.htm";
			}
			else if (npc.getId() == 32894)
			{
				if (isBossAlive(npc))
				{
					return "32871-03.htm";
				}

				if (hasQuestItems(player, 17248))
				{
					takeItems(player, 17248, 1);
					final var bossSpawn = BOSS_SPAWN_4[getRandom(0, BOSS_SPAWN_4.length - 1)];
					if (bossSpawn != null)
					{
						addSpawn(RAIDS[getRandom(RAIDS.length)], bossSpawn.rnd(null, 50, 100, true), false, 0);
					}
					return "32871-01.htm";
				}
				return "32871-02.htm";
			}
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		var st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		return "32871.htm";
	}

	private boolean isBossAlive(Npc npc)
	{
		if (Config.DRAGON_VORTEX_UNLIMITED_SPAWN)
		{
			return false;
		}
		
		for (final var mob : World.getInstance().getAroundNpc(npc))
		{
			if (ArrayUtils.contains(RAIDS, mob.getId()))
			{
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args)
	{
		new DragonVortex(-1, DragonVortex.class.getSimpleName(), "custom");
	}
}
