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
 * Rework by LordWinter 25.02.2020
 */
public class _109_InSearchOfTheNest extends Quest
{
	public _109_InSearchOfTheNest(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31553);
		addTalkId(31553, 32015, 31554);

		questItemIds = new int[]
		{
		        14858
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("32015-02.htm") && npc.getId() == 32015)
		{
			if (st.isCond(1))
			{
				st.giveItems(14858, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31553-02.htm") && st.isCond(2) && npc.getId() == 31553)
		{
			st.takeItems(14858, -1);
			st.setCond(3, true);
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
		final int id = st.getState();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if(id == State.CREATED)
		{
			if (player.getLevel() >= getMinLvl(getId()) && npcId == 31553 && (st.getQuestItemsCount(7246) > 0 || st.getQuestItemsCount(7247) > 0))
			{
				st.startQuest();
				htmltext = "31553-00a.htm";
			}
			else
			{
				htmltext = "31553-00.htm";
				st.exitQuest(true);
			}
		}
		else if(id == State.STARTED)
		{
			if (npcId == 32015)
			{
				if (st.isCond(1))
				{
					htmltext = "32015-01.htm";
				}
				else if (st.isCond(2))
				{
					htmltext = "32015-03.htm";
				}
			}
			else if (npcId == 31553)
			{
				if (st.isCond(1))
				{
					htmltext = "31553-01a.htm";
				}
				else if (st.isCond(2))
				{
					htmltext = "31553-01.htm";
				}
				else if (st.isCond(3))
				{
					htmltext = "31553-01b.htm";
				}
			}
			else if (npcId == 31554 && st.isCond(3))
			{
				htmltext = "31554-01.htm";
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _109_InSearchOfTheNest(109, _109_InSearchOfTheNest.class.getSimpleName(), "");
	}
}