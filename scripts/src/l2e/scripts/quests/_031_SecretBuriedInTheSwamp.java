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

/**
 * Rework by LordWinter 06.12.2019
 */
public class _031_SecretBuriedInTheSwamp extends Quest
{
	public _031_SecretBuriedInTheSwamp(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31555);
		addTalkId(31555, 31661, 31662, 31663, 31664, 31665);

		questItemIds = new int[]
		{
		        7252
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

		if (event.equalsIgnoreCase("31555-1.htm") && npc.getId() == 31555)
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31665-1.htm") && cond == 1 && npc.getId() == 31665)
		{
			st.setCond(2, true);
			st.giveItems(7252, 1);
		}
		else if (event.equalsIgnoreCase("31555-4.htm") && cond == 2 && npc.getId() == 31555)
		{
			st.setCond(3, true);
		}
		else if (event.equalsIgnoreCase("31661-1.htm") && cond == 3 && npc.getId() == 31661)
		{
			st.setCond(4, true);
		}
		else if (event.equalsIgnoreCase("31662-1.htm") && cond == 4 && npc.getId() == 31662)
		{
			st.setCond(5, true);
		}
		else if (event.equalsIgnoreCase("31663-1.htm") && cond == 5 && npc.getId() == 31663)
		{
			st.setCond(6, true);
		}
		else if (event.equalsIgnoreCase("31664-1.htm") && cond == 6 && npc.getId() == 31664)
		{
			st.setCond(7, true);
		}
		else if (event.equalsIgnoreCase("31555-7.htm") && cond == 7 && npc.getId() == 31555)
		{
			st.takeItems(7252, -1);
			st.calcExpAndSp(getId());
			st.calcReward(getId());
			st.exitQuest(false, true);
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

		if (st.isCompleted())
		{
			return htmltext = getAlreadyCompletedMsg(player);
		}

		final int npcId = npc.getId();
		final int cond = st.getCond();
		if (npcId == 31555)
		{
			if(cond == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "31555-0.htm";
				}
				else
				{
					htmltext = "31555-0a.htm";
					st.exitQuest(true);
				}
			}
			else if(cond == 1)
			{
				htmltext = "31555-2.htm";
			}
			else if(cond == 2)
			{
				htmltext = "31555-3.htm";
			}
			else if(cond == 3)
			{
				htmltext = "31555-5.htm";
			}
			else if(cond == 7)
			{
				htmltext = "31555-6.htm";
			}
		}
		else if (npcId == 31665)
		{
			if(cond == 1)
			{
				htmltext = "31665-0.htm";
			}
			else if(cond == 2)
			{
				htmltext = "31665-2.htm";
			}
		}
		else if (npcId == 31661)
		{
			if(cond == 3)
			{
				htmltext = "31661-0.htm";
			}
			else if(cond > 3)
			{
				htmltext = "31661-2.htm";
			}
		}
		else if (npcId == 31662)
		{
			if(cond == 4)
			{
				htmltext = "31662-0.htm";
			}
			else if(cond > 4)
			{
				htmltext = "31662-2.htm";
			}
		}
		else if (npcId == 31663)
		{
			if(cond == 5)
			{
				htmltext = "31663-0.htm";
			}
			else if(cond > 5)
			{
				htmltext = "31663-2.htm";
			}
		}
		else if (npcId == 31664)
		{
			if(cond == 6)
			{
				htmltext = "31664-0.htm";
			}
			else if(cond > 6)
			{
				htmltext = "31664-2.htm";
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _031_SecretBuriedInTheSwamp(31, _031_SecretBuriedInTheSwamp.class.getSimpleName(), "");
	}
}