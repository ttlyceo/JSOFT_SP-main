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
 * Rework by LordWinter 16.12.2019
 */
public class _10274_CollectingInTheAir extends Quest
{
	public _10274_CollectingInTheAir(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32557);
		addTalkId(32557);

		questItemIds = new int[]
		{
		        13844, 13858, 13859, 13860
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

		if (event.equalsIgnoreCase("32557-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
				st.giveItems(13844, 8);
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

		switch (st.getState())
		{
			case State.COMPLETED :
				htmltext = "32557-0a.htm";
				break;
			case State.CREATED :
				final QuestState qs = player.getQuestState("_10273_GoodDayToFly");
				if (qs != null)
				{
					htmltext = (qs.isCompleted() && (player.getLevel() >= getMinLvl(getId()))) ? "32557-01.htm" : "32557-00.htm";
				}
				else
				{
					htmltext = "32557-00.htm";
				}
				break;
			case State.STARTED :
				if ((st.getQuestItemsCount(13858) + st.getQuestItemsCount(13859) + st.getQuestItemsCount(13860)) >= 8)
				{
					htmltext = "32557-05.htm";
					st.calcExpAndSp(getId());
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "32557-04.htm";
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _10274_CollectingInTheAir(10274, _10274_CollectingInTheAir.class.getSimpleName(), "");
	}
}