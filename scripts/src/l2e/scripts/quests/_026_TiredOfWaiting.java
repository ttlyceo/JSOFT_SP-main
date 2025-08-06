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
 * Rework by LordWinter 06.12.2019
 */
public class _026_TiredOfWaiting extends Quest
{
	public _026_TiredOfWaiting(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30655);
		addTalkId(30655, 31045);
		
		questItemIds = new int[]
		{
		        17281
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		switch (npcId)
		{
			case 30655 :
				if (event.equalsIgnoreCase("30655-04.html"))
				{
					st.giveItems(17281, 1);
					st.startQuest();
				}
				break;
			case 31045 :
				if (event.equalsIgnoreCase("31045-04.html"))
				{
					st.takeItems(17281, 1);
				}
				else if (event.equalsIgnoreCase("31045-10.html"))
				{
					if (st.isCond(1))
					{
						st.calcReward(getId(), 1);
						st.exitQuest(false, true);
					}
				}
				else if (event.equalsIgnoreCase("31045-11.html"))
				{
					if (st.isCond(1))
					{
						st.calcReward(getId(), 2);
						st.exitQuest(false, true);
					}
				}
				else if (event.equalsIgnoreCase("31045-12.html"))
				{
					if (st.isCond(1))
					{
						st.calcReward(getId(), 3);
						st.exitQuest(false, true);
					}
				}
				break;
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
		
		final int npcId = npc.getId();
		switch (st.getState())
		{
			case State.CREATED:
				if (npcId == 30655)
				{
					htmltext = (player.getLevel() >= getMinLvl(getId())) ? "30655-01.htm" : "30655-00.html";
				}
				break;
			case State.STARTED:
				if (st.getCond() == 1)
				{
					switch (npcId)
					{
						case 30655 :
							htmltext = "30655-07.html";
							break;
						case 31045 :
							htmltext = (st.hasQuestItems(17281)) ? "31045-01.html" : "31045-09.html";
							break;
					}
				}
				break;
			case State.COMPLETED:
				if (npcId == 30655)
				{
					htmltext = "30655-08.html";
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _026_TiredOfWaiting(26, _026_TiredOfWaiting.class.getSimpleName(), "");
	}
}