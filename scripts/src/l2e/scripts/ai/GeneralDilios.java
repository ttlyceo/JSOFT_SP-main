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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

public class GeneralDilios extends AbstractNpcAI
{
	private static final int GENERAL_ID = 32549;
	private static final int GUARD_ID = 32619;
	
	private Npc _general;
	private final List<Npc> _guards = new ArrayList<>();

	private static final NpcStringId[] diliosText =
	{
	        NpcStringId.MESSENGER_INFORM_THE_PATRONS_OF_THE_KEUCEREUS_ALLIANCE_BASE_WERE_GATHERING_BRAVE_ADVENTURERS_TO_ATTACK_TIATS_MOUNTED_TROOP_THATS_ROOTED_IN_THE_SEED_OF_DESTRUCTION, NpcStringId.MESSENGER_INFORM_THE_BROTHERS_IN_KUCEREUS_CLAN_OUTPOST_BRAVE_ADVENTURERS_WHO_HAVE_CHALLENGED_THE_SEED_OF_INFINITY_ARE_CURRENTLY_INFILTRATING_THE_HALL_OF_EROSION_THROUGH_THE_DEFENSIVELY_WEAK_HALL_OF_SUFFERING, NpcStringId.MESSENGER_INFORM_THE_PATRONS_OF_THE_KEUCEREUS_ALLIANCE_BASE_THE_SEED_OF_INFINITY_IS_CURRENTLY_SECURED_UNDER_THE_FLAG_OF_THE_KEUCEREUS_ALLIANCE
	};
	
	private GeneralDilios(String name, String descr)
	{
		super(name, descr);
		findNpcs();
		if ((_general != null) && !_guards.isEmpty())
		{
			startQuestTimer("command_0", 60000, null, null);
		}
	}
	
	private void findNpcs()
	{
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn != null)
			{
				if (spawn.getId() == GENERAL_ID)
				{
					_general = spawn.getLastSpawn();
				}
				else if (spawn.getId() == GUARD_ID)
				{
					_guards.add(spawn.getLastSpawn());
				}
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("command_"))
		{
			int value = Integer.parseInt(event.substring(8));
			if (value < 6)
			{
				_general.broadcastPacketToOthers(2000, new NpcSay(_general.getObjectId(), Say2.NPC_ALL, GENERAL_ID, NpcStringId.STABBING_THREE_TIMES));
				startQuestTimer("guard_animation_0", 3400, null, null);
			}
			else
			{
				value = -1;
				_general.broadcastPacketToOthers(new NpcSay(_general.getObjectId(), Say2.NPC_SHOUT, GENERAL_ID, diliosText[getRandom(diliosText.length)]));
			}
			startQuestTimer("command_" + (value + 1), 60000, null, null);
		}
		else if (event.startsWith("guard_animation_"))
		{
			final int value = Integer.parseInt(event.substring(16));
			
			for (final Npc guard : _guards)
			{
				guard.broadcastSocialAction(4);
			}
			if (value < 2)
			{
				startQuestTimer("guard_animation_" + (value + 1), 1500, null, null);
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new GeneralDilios(GeneralDilios.class.getSimpleName(), "ai");
	}
}
