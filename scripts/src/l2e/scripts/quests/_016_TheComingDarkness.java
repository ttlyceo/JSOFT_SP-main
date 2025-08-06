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
public class _016_TheComingDarkness extends Quest
{
	public _016_TheComingDarkness(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31517);
		addTalkId(31517, 31512, 31513, 31514, 31515, 31516);

		questItemIds = new int[]
		{
		        7167
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
		
		final int cond = st.getCond();
		switch (event)
		{
			case "31517-2.htm" :
				st.startQuest();
				st.giveItems(7167, 5);
				break;
			case "31512-1.htm" :
			case "31513-1.htm" :
			case "31514-1.htm" :
			case "31515-1.htm" :
			case "31516-1.htm" :
				final int npcId = Integer.parseInt(event.replace("-1.htm", ""));
				if ((cond == (npcId - 31511)) && st.hasQuestItems(7167))
				{
					st.takeItems(7167, 1);
					st.setCond(cond + 1, true);
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
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				final QuestState st2 = player.getQuestState("_017_LightAndDarkness");
				if (st2 != null && st2.isCompleted())
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "31517-0.htm";
					}
					else
					{
						htmltext = "31517-0a.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "31517-0b.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if (npcId == 31517)
				{
					if (cond > 0 && cond < 6)
					{
						if (st.getQuestItemsCount(7167) > 0)
						{
							htmltext = "31517-3a.htm";
						}
						else
						{
							htmltext = "31517-3b.htm";
						}
					}
					else if (st.isCond(6))
					{
						htmltext = "31517-3.htm";
						st.calcExpAndSp(getId());
						st.exitQuest(false, true);
					}
				}
				else if ((npcId - 31511) == st.getCond())
				{
					htmltext = npcId + "-0.htm";
				}
				else
				{
					htmltext = npcId + "-1.htm";
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _016_TheComingDarkness(16, _016_TheComingDarkness.class.getSimpleName(), "");
	}
}
