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
package l2e.scripts.quests;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 12.11.2021
 */
public class _652_AnAgedExAdventurer extends Quest
{
	public _652_AnAgedExAdventurer(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(32012);
		addTalkId(32012, 30180);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("32012-02.htm"))
		{
			if (st.getQuestItemsCount(1464) < 100)
			{
				return "32012-02a.htm";
			}
			st.startQuest();
			st.takeItems(1464, 100);
			npc.deleteMe();
			htmltext = event;
		}
		else if(event.equalsIgnoreCase("32012-03.htm"))
		{
			htmltext = event;
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (npc.getId())
		{
			case 32012 :
				switch (st.getState())
				{
					case State.CREATED :
						htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32012-01.htm" : "32012-00.htm";
						break;
					case State.STARTED :
						htmltext = "32012-02.htm";
						break;
				}
				break;
			case 30180 :
				if (st.isStarted())
				{
					if (getRandom(100) < 50)
					{
						st.calcReward(getId(), 1);
						htmltext = "30180-01.htm";
					}
					else
					{
						st.calcReward(getId(), 2);
						htmltext = "30180-01a.htm";
					}
					st.exitQuest(true, true);
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _652_AnAgedExAdventurer(652, _652_AnAgedExAdventurer.class.getSimpleName(), "");
	}
}