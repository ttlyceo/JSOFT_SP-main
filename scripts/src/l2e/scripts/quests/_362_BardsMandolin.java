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
 * Rework by LordWinter 18.04.2020
 */
public class _362_BardsMandolin extends Quest
{
	public _362_BardsMandolin(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30957);
		addTalkId(30957, 30956, 30958, 30837);

		questItemIds = new int[]
		{
		        4316, 4317
		};
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
		
		if (event.equalsIgnoreCase("30957-3.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("30957-7.htm") || event.equalsIgnoreCase("30957-8.htm"))
		{
			if (st.isCond(5))
			{
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30957-1.htm";
				}
				else
				{
					htmltext = "30957-2.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 30957 :
						if (cond == 1 || cond == 2)
						{
							htmltext = "30957-4.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30957-5.htm";
							st.setCond(4, true);
							st.giveItems(4317, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30957-5a.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30957-6.htm";
						}
						break;
					case 30837 :
						if (cond == 1)
						{
							st.setCond(2, true);
							htmltext = "30837-1.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30837-2.htm";
						}
						else if (cond > 2)
						{
							htmltext = "30837-3.htm";
						}
						break;
					case 30958 :
						if (cond == 2)
						{
							htmltext = "30958-1.htm";
							st.setCond(3, true);
							st.giveItems(4316, 1);
						}
						else if (cond >= 3)
						{
							htmltext = "30958-2.htm";
						}
						break;
					case 30956 :
						if (cond == 4)
						{
							htmltext = "30956-1.htm";
							st.setCond(5, true);
							st.takeItems(4316, 1);
							st.takeItems(4317, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30956-2.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _362_BardsMandolin(362, _362_BardsMandolin.class.getSimpleName(), "");
	}
}