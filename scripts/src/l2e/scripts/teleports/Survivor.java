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

public class Survivor extends Quest
{
	public Survivor(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32632);
		addTalkId(32632);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if (!event.isEmpty())
		{
			if (player.getLevel() < 75)
			{
				event = "32632-3.htm";
			}
			else if (st.getQuestItemsCount(57) >= 150000)
			{
				if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
				{
					BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), new Location(-149406, 255247, -80), 1000);
					st.takeItems(57, 150000);
					return null;
				}
				st.takeItems(57, 150000);
				player.teleToLocation(-149406, 255247, -80, true, player.getReflection());
			}
		}
		
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		return "32632-1.htm";
	}
	
	public static void main(String[] args)
	{
		new Survivor(-1, Survivor.class.getSimpleName(), "teleports");
	}
}