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
package l2e.scripts.ai;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SpecialCamera;

public class DrChaos extends Quest
{
	private static final int DOCTER_CHAOS = 32033;
	private static final int STRANGE_MACHINE = 32032;
	private static final int CHAOS_GOLEM = 25703;
	private static boolean _IsGolemSpawned;

	public DrChaos(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addFirstTalkId(32033);
		_IsGolemSpawned = false;
	}

	public Npc findTemplate(int npcId)
	{
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if ((spawn != null) && (spawn.getId() == npcId))
			{
				return spawn.getLastSpawn();
			}
		}
		return null;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("1"))
		{
			final Npc machine_instance = findTemplate(STRANGE_MACHINE);
			if (machine_instance != null)
			{
				npc.getAI().setIntention(CtrlIntention.ATTACK, machine_instance);
				machine_instance.broadcastPacketToOthers(new SpecialCamera(machine_instance, 1, -200, 15, 10000, 1000, 20000, 0, 0, 0, 0, 0));
			}
			else
			{
				startQuestTimer("2", 2000, npc, player);
			}
			startQuestTimer("3", 10000, npc, player);
		}
		else if (event.equalsIgnoreCase("2"))
		{
			npc.broadcastSocialAction(3);
		}
		else if (event.equalsIgnoreCase("3"))
		{
			npc.broadcastPacketToOthers(new SpecialCamera(npc, 1, -150, 10, 3000, 1000, 20000, 0, 0, 0, 0, 0));
			startQuestTimer("4", 2500, npc, player);
		}
		else if (event.equalsIgnoreCase("4"))
		{
			npc.getAI().setIntention(CtrlIntention.MOVING, new Location(96055, -110759, -3312, 0), 0);
			startQuestTimer("5", 2000, npc, player);
		}
		else if (event.equalsIgnoreCase("5"))
		{
			player.teleToLocation(94832, -112624, -3304, true, player.getReflection());
			npc.teleToLocation(-113091, -243942, -15536, true, npc.getReflection());
			if (!_IsGolemSpawned)
			{
				final Npc golem = addSpawn(CHAOS_GOLEM, 94640, -112496, -3336, 0, false, 0);
				_IsGolemSpawned = true;
				startQuestTimer("6", 1000, golem, player);
				player.sendPacket(new PlaySound(1, "Rm03_A", 0, 0, 0, 0, 0));
			}
		}
		else if (event.equalsIgnoreCase("6"))
		{
			npc.broadcastPacketToOthers(new SpecialCamera(npc, 30, -200, 20, 6000, 700, 8000, 0, 0, 0, 0, 0));
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == DOCTER_CHAOS)
		{
			npc.getAI().setIntention(CtrlIntention.MOVING, new Location(96323, -110914, -3328, 0), 0);
			this.startQuestTimer("1", 3000, npc, player);
		}
		return "";
	}

	public static void main(String[] args)
	{
		new DrChaos(-1, "DrChaos", "ai");
	}
}
