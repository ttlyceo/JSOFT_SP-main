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
package l2e.scripts.teleports;

import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

public class TeleportToFantasy extends Quest
{
	private static final Map<Integer, Integer> TELEPORTERS = new HashMap<>();
	
	private static final Location[] RETURN_LOCS =
	{
		new Location(-80826, 149775, -3043),
		new Location(-12672, 122776, -3116),
		new Location(15670, 142983, -2705),
		new Location(83400, 147943, -3404),
		new Location(111409, 219364, -3545),
		new Location(82956, 53162, -1495),
		new Location(146331, 25762, -2018),
		new Location(116819, 76994, -2714),
		new Location(43835, -47749, -792),
		new Location(147930, -55281, -2728),
		new Location(87386, -143246, -1293),
		new Location(12882, 181053, -3560)
	};
	
	private static final Location[] ISLE_LOCS =
	{
		new Location(-58752, -56898, -2032),
		new Location(-59716, -57868, -2032),
		new Location(-60691, -56893, -2032),
		new Location(-59720, -55921, -2032)
	};
	
	public TeleportToFantasy(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		TELEPORTERS.put(30059, 3);
		TELEPORTERS.put(30080, 4);
		TELEPORTERS.put(30177, 6);
		TELEPORTERS.put(30233, 8);
		TELEPORTERS.put(30256, 2);
		TELEPORTERS.put(30320, 1);
		TELEPORTERS.put(30848, 7);
		TELEPORTERS.put(30899, 5);
		TELEPORTERS.put(31320, 9);
		TELEPORTERS.put(31275, 10);
		TELEPORTERS.put(31964, 11);
		
		for (final int npcId : TELEPORTERS.keySet())
		{
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		
		addStartNpc(32378);
		addTalkId(32378);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String htmltext = "";
		final QuestState st = player.getQuestState(getName());
		
		if (st == null)
		{
			return null;
		}
		
		if (TELEPORTERS.containsKey(npc.getId()))
		{
			final int random_id = getRandom(ISLE_LOCS.length);
			if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
			{
				BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), ISLE_LOCS[random_id], 1000);
				st.setState(State.STARTED);
				st.set("id", String.valueOf(TELEPORTERS.get(npc.getId())));
				return null;
			}
			player.teleToLocation(ISLE_LOCS[random_id], false, player.getReflection());
			st.setState(State.STARTED);
			st.set("id", String.valueOf(TELEPORTERS.get(npc.getId())));
		}
		else if (npc.getId() == 32378)
		{
			if (st.getState() == State.STARTED && st.getInt("id") > 0)
			{
				final int return_id = st.getInt("id") - 1;
				if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
				{
					BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), RETURN_LOCS[return_id], 1000);
					st.unset("id");
					return null;
				}
				player.teleToLocation(RETURN_LOCS[return_id], false, player.getReflection());
				st.unset("id");
			}
			else
			{
				player.sendPacket(new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.IF_YOUR_MEANS_OF_ARRIVAL_WAS_A_BIT_UNCONVENTIONAL_THEN_ILL_BE_SENDING_YOU_BACK_TO_RUNE_TOWNSHIP_WHICH_IS_THE_NEAREST_TOWN));
				if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
				{
					BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), new Location(43835, -47749, -792), 1000);
					return null;
				}
				player.teleToLocation(43835, -47749, -792, true, player.getReflection());
			}
			st.exitQuest(true);
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new TeleportToFantasy(-1, TeleportToFantasy.class.getSimpleName(), "teleports");
	}
}