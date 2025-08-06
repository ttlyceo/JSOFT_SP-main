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
public class _018_MeetingWithTheGoldenRam extends Quest
{
	public _018_MeetingWithTheGoldenRam(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31314);
		addTalkId(31314, 31315, 31555);
		
		questItemIds = new int[]
		{
		        7245
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

		if (event.equalsIgnoreCase("31314-03.htm") && npc.getId() == 31314)
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31315-02.htm") && npc.getId() == 31315)
		{
			if (st.isCond(1))
			{
				st.giveItems(7245, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31555-02.htm") && npc.getId() == 31555)
		{
			if (st.isCond(2) && st.getQuestItemsCount(7245) == 1)
			{
				st.takeItems(7245, 1);
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
		
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "31314-01.htm";
				}
				else
				{
					htmltext = "31314-02.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 31314 :
						if (cond == 1)
						{
							htmltext = "31314-04.htm";
						}
						break;
					case 31315 :
						switch (cond)
						{
							case 1:
								htmltext = "31315-01.htm";
								break;
							case 2:
								htmltext = "31315-03.htm";
								break;
						}
						break;
					case 31555 :
						if (st.isCond(2) && st.getQuestItemsCount(7245) == 1)
						{
							htmltext = "31555-01.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _018_MeetingWithTheGoldenRam(18, _018_MeetingWithTheGoldenRam.class.getSimpleName(), "");
	}
}