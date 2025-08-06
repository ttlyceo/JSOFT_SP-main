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

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.spawn.Spawner;
import l2e.scripts.ai.AbstractNpcAI;

public class TownPets extends AbstractNpcAI
{
	private static final int[] PETS =
	{
	                31202,
	                31203,
	                31204,
	                31205,
	                31206,
	                31207,
	                31208,
	                31209,
	                31266,
	                31593,
	                31758,
	                31955
	};

	private TownPets(String name, String descr)
	{
		super(name, descr);

		addSpawnId(PETS);

		for (int npcId : PETS)
		{
			for (Spawner spawn : SpawnParser.getInstance().getSpawnData())
			{
				if ((spawn != null) && (spawn.getId() == npcId))
				{
					onSpawn(spawn.getLastSpawn());
				}
			}
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("move"))
		{
			final int locX = (npc.getSpawn().getX() - 50) + getRandom(100);
			final int locY = (npc.getSpawn().getY() - 50) + getRandom(100);
			npc.setRunning();
			npc.getAI().setIntention(CtrlIntention.MOVING, new Location(locX, locY, npc.getZ(), 0), 0);
			startQuestTimer("move", 5000, npc, null);
		}
		return null;
	}

	@Override
	public String onSpawn(Npc npc)
	{
		if (Config.ALLOW_PET_WALKERS)
		{
			startQuestTimer("move", 3000, npc, null);
		}
		return super.onSpawn(npc);
	}

	public static void main(String[] args)
	{
		new TownPets(TownPets.class.getSimpleName(), "custom");
	}
}
