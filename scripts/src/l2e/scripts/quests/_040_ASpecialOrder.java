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
 * Rework by LordWinter 08.12.2019
 */
public class _040_ASpecialOrder extends Quest
{
	public _040_ASpecialOrder(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30081);
		addTalkId(30081, 31572, 30511);

		questItemIds = new int[]
		{
		        12764, 12765
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

		if (event.equalsIgnoreCase("30081-02.htm"))
		{
			st.startQuest();
			final int rnd = getRandom(1, 2);
			if (rnd == 1)
			{
				st.setCond(2, false);
				htmltext = "30081-02a.htm";
			}
			else
			{
				st.setCond(5, false);
				htmltext = "30081-02b.htm";
			}
		}
		else if (event.equalsIgnoreCase("30511-03.htm"))
		{
			st.setCond(6, true);
		}
		else if (event.equalsIgnoreCase("31572-03.htm"))
		{
			st.setCond(3, true);
		}
		else if (event.equalsIgnoreCase("30081-05a.htm"))
		{
			if (st.isCond(4))
			{
				st.takeItems(12764, 1);
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		else if (event.equalsIgnoreCase("30081-05b.htm"))
		{
			if (st.isCond(7))
			{
				st.takeItems(12765, 1);
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

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (npcId == 30081)
		{
			if (cond == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30081-01.htm";
				}
				else
				{
					htmltext = "30081-00.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 2 || cond == 3)
			{
				htmltext = "30081-03a.htm";
			}
			else if (cond == 4)
			{
				htmltext = "30081-04a.htm";
			}
			else if (cond == 5 || cond == 6)
			{
				htmltext = "30081-03b.htm";
			}
			else if (cond == 7)
			{
				htmltext = "30081-04b.htm";
			}
		}
		else if (npcId == 31572)
		{
			if (cond == 2)
			{
				htmltext = "31572-01.htm";
			}
			else if (cond == 3)
			{
				if (st.getQuestItemsCount(6450) >= 10 && st.getQuestItemsCount(6451) >= 10 && st.getQuestItemsCount(6452) >= 10)
				{
					st.takeItems(6450, 10);
					st.takeItems(6451, 10);
					st.takeItems(6452, 10);
					st.setCond(4, true);
					st.giveItems(12764, 1);
					htmltext = "31572-05.htm";
				}
				else
				{
					htmltext = "31572-04.htm";
				}
			}
			else if (cond == 4)
			{
				htmltext = "31572-06.htm";
			}
		}
		else if (npcId == 30511)
		{
			if (cond == 5)
			{
				htmltext = "30511-01.htm";
			}
			else if (cond == 6)
			{
				if (st.getQuestItemsCount(5079) >= 40 && st.getQuestItemsCount(5082) >= 40 && st.getQuestItemsCount(5084) >= 40)
				{
					st.takeItems(5079, 40);
					st.takeItems(5082, 40);
					st.takeItems(5084, 40);
					st.setCond(7, true);
					st.giveItems(12765, 1);
					htmltext = "30511-05.htm";
				}
				else
				{
					htmltext = "30511-04.htm";
				}
			}
			else if (cond == 7)
			{
				htmltext = "30511-06.htm";
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _040_ASpecialOrder(40, _040_ASpecialOrder.class.getSimpleName(), "");
	}
}