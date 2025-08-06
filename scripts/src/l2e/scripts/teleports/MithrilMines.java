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

import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.BotFunctions;

public class MithrilMines extends Quest
{
	private static final Location[] _locs =
	{
		new Location(171946, -173352, 3440),
		new Location(175499, -181586, -904),
		new Location(173462, -174011, 3480),
		new Location(179299, -182831, -224),
		new Location(178591, -184615, 360),
		new Location(175499, -181586, -904)
	};
	
	public MithrilMines(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(32652);
		addFirstTalkId(32652);
		addTalkId(32652);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = "";
		final QuestState st = player.getQuestState(getName());
		
		final int index = Integer.parseInt(event) - 1;
		if (_locs.length > index)
		{
			final Location loc = _locs[index];
			if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
			{
				BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), loc, 1000);
				st.exitQuest(true);
				return null;
			}
			player.teleToLocation(loc, false, player.getReflection());
			st.exitQuest(true);
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if (npc.isInsideRadius(173147, -173762, Npc.INTERACTION_DISTANCE, true))
		{
			htmltext = "32652-01.htm";
		}
		else if (npc.isInsideRadius(181941, -174614, Npc.INTERACTION_DISTANCE, true))
		{
			htmltext = "32652-02.htm";
		}
		else if (npc.isInsideRadius(179560, -182956, Npc.INTERACTION_DISTANCE, true))
		{
			htmltext = "32652-03.htm";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new MithrilMines(-1, MithrilMines.class.getSimpleName(), "teleports");
	}
}