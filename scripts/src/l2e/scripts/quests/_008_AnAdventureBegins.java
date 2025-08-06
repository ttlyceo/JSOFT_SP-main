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
public class _008_AnAdventureBegins extends Quest
{
	public _008_AnAdventureBegins(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30134);
		addTalkId(30134, 30355, 30144);
		
		questItemIds = new int[]
		{
		        7573
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
		
		if (event.equalsIgnoreCase("30134-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30355-02.htm"))
		{
			if (st.isCond(1))
			{
				st.giveItems(7573, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30144-02.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(7573, 1);
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("30134-06.htm"))
		{
			if (st.isCond(3))
			{
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		String htmltext = getNoQuestMsg(player);
		
		final int cond = st.getCond();
		final int npcId = npc.getId();

		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 30134)
				{
					if (player.getRace().ordinal() == 2 && player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "30134-02.htm";
					}
					else
		            {
		                htmltext = "30134-01.htm";
						st.exitQuest(true);
		            }
				}
				break;
			case State.STARTED:
				if (npcId == 30355)
				{
					switch (cond)
					{
						case 1:
							if (st.getQuestItemsCount(7573) == 0)
							{
								htmltext = "30355-01.htm";
							}
					         break;
						case 2:
							htmltext = "30355-03.htm";
							break;
					}
				}
				else if (npcId == 30134)
				{
					switch (cond)
					{
						case 1:
						case 2:
							htmltext = "30134-04.htm";
							break;
						case 3 :
							htmltext = "30134-05.htm";
							break;
					}
				}
				else if (npcId == 30144)
				{
					if (cond == 2 && st.getQuestItemsCount(7573) > 0)
					{
						htmltext = "30144-01.htm";
					}
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _008_AnAdventureBegins(8, _008_AnAdventureBegins.class.getSimpleName(), "");
	}
}