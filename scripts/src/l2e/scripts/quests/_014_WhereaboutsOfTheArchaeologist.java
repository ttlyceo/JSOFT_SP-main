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
 * Rework by LordWinter 05.12.2019
 */
public class _014_WhereaboutsOfTheArchaeologist extends Quest
{
	public _014_WhereaboutsOfTheArchaeologist(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31263);
		addTalkId(31263, 31538);
		
		questItemIds = new int[]
		{
		        7253
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
		
		if (event.equalsIgnoreCase("31263-2.htm") && npc.getId() == 31263)
		{
			st.giveItems(7253, 1);
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31538-1.htm") && npc.getId() == 31538)
		{
			if (st.isCond(1) && st.getQuestItemsCount(7253) == 1)
			{
				st.takeItems(7253, 1);
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
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
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "31263-0.htm";
				}
				else
				{
					htmltext = "31263-1.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 31263 :
						if (st.isCond(1))
						{
							htmltext = "31263-2.htm";
						}
						break;
					case 31538 :
						if (st.isCond(1) && st.getQuestItemsCount(7253) == 1)
						{
							htmltext = "31538-0.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _014_WhereaboutsOfTheArchaeologist(14, _014_WhereaboutsOfTheArchaeologist.class.getSimpleName(), "");
	}
}