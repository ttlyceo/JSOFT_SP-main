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
public class _029_ChestCaughtWithABaitOfEarth extends Quest
{
	public _029_ChestCaughtWithABaitOfEarth(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31574);
		addTalkId(31574, 30909);
		
		questItemIds = new int[]
		{
		        7627
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31574-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31574-07.htm"))
		{
			if (st.getQuestItemsCount(6507) > 0)
			{
				st.setCond(2, true);
				st.takeItems(6507, 1);
				st.giveItems(7627, 1);
			}
			else
			{
				htmltext = "31574-08.htm";
			}
		}
		else if (event.equalsIgnoreCase("30909-02.htm"))
		{
			if (st.getQuestItemsCount(7627) == 1)
			{
				htmltext = "30909-02.htm";
				st.takeItems(7627, -1);
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
			else
			{
				htmltext = "30909-03.htm";
				st.exitQuest(true);
			}
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
		
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					final QuestState qs = player.getQuestState("_052_WilliesSpecialBait");
					if (qs != null && qs.isCompleted())
					{
						if (qs.isCompleted())
						{
							htmltext = "31574-01.htm";
						}
						else
						{
							htmltext = "31574-02.htm";
							st.exitQuest(true);
						}
					}
				}
				else
				{
					htmltext = "31574-03.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				if (npc.getId() == 31574)
				{
					if (cond == 1)
					{
						htmltext = "31574-05.htm";
						if (st.getQuestItemsCount(6507) == 0)
						{
							htmltext = "31574-06.htm";
						}
					}
					else if (cond == 2)
					{
						htmltext = "31574-09.htm";
					}
				}
				else if (npc.getId() == 30909)
				{
					if (cond == 2)
					{
						htmltext = "30909-01.htm";
					}
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _029_ChestCaughtWithABaitOfEarth(29, _029_ChestCaughtWithABaitOfEarth.class.getSimpleName(), "");
	}
}