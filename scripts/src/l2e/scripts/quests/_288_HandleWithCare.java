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
 * Rework by LordWinter 26.04.2021
 */
public class _288_HandleWithCare extends Quest
{
	public _288_HandleWithCare(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32741);
		addTalkId(32741);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (npc.getId() == 32741)
		{
			if (event.equalsIgnoreCase("32741-03.htm"))
			{
				if (st.isCreated())
				{
					st.startQuest();
				}
			}
			else if (event.equalsIgnoreCase("32741-07.htm") && st.getCond() > 1)
			{
				if (st.hasQuestItems(15498))
				{
					st.takeItems(15498, 1);
					st.calcReward(getId(), 1, true);
				}
				else if (st.hasQuestItems(15497))
				{
					st.takeItems(15497, 1);
					st.calcReward(getId(), 1, true);
					st.calcReward(getId(), 2);
				}
				st.exitQuest(true, true);
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

		if (npc.getId() == 32741)
		{
			switch(st.getState())
			{
				case State.CREATED:
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "32741-01.htm";
					}
					else
					{
						htmltext = "32741-00.htm";
					}
					break;
				case State.STARTED:
					if (st.isCond(2) && st.hasQuestItems(15498))
					{
						htmltext = "32741-05.htm";
					}
					else if (st.isCond(3) && st.hasQuestItems(15497))
					{
						htmltext = "32741-06.htm";
					}
					else
					{
						htmltext = "32741-04.htm";
					}
					break;
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _288_HandleWithCare(288, _288_HandleWithCare.class.getSimpleName(), "");
	}
}