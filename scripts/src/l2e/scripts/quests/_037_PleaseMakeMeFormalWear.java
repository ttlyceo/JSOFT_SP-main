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
public class _037_PleaseMakeMeFormalWear extends Quest
{
	public _037_PleaseMakeMeFormalWear(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30842);
		addTalkId(30842, 31520, 31521, 31627);
		
		questItemIds = new int[]
		{
		        7159, 7160, 7164
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

		if (event.equalsIgnoreCase("30842-1.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31520-1.htm"))
		{
			if (st.isCond(1))
			{
				st.giveItems(7164, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31521-1.htm"))
		{
			if (st.isCond(2))
			{
				st.giveItems(7160, 1);
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("31627-1.htm"))
		{
			if (st.getQuestItemsCount(7160) > 0)
			{
				st.takeItems(7160, 1);
				st.setCond(4, true);
			}
			else
			{
				htmltext = "no_items.htm";
			}
		}
		else if (event.equalsIgnoreCase("31521-3.htm"))
		{
			if (st.isCond(4))
			{
				st.giveItems(7159, 1);
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("31520-3.htm"))
		{
			if (st.isCond(5))
			{
				st.setCond(6, true);
			}
		}
		else if (event.equalsIgnoreCase("31520-5.htm"))
		{
			if (st.isCond(6) && st.getQuestItemsCount(7076) > 0 && st.getQuestItemsCount(7077) > 0 && st.getQuestItemsCount(7078) > 0)
			{
				st.takeItems(7076, 1);
				st.takeItems(7077, 1);
				st.takeItems(7078, 1);
				st.setCond(7, true);
			}
			else
			{
				htmltext = "no_items.htm";
			}
		}
		else if (event.equalsIgnoreCase("31520-7.htm"))
		{
			if (st.isCond(7))
			{
				if (st.getQuestItemsCount(7113) > 0)
				{
					st.takeItems(7113, 1);
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "no_items.htm";
				}
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}

		if (npcId == 30842)
		{
			if (cond == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30842-0.htm";
				}
				else
				{
					htmltext = "30842-2.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "30842-2a.htm";
			}
		}
		else if (npcId == 31520)
		{
			if (cond == 1)
			{
				htmltext = "31520-0.htm";
			}
			else if (cond == 2 || cond == 3 || cond == 4 || cond == 5 )
			{
				if (st.getQuestItemsCount(7159) > 0)
				{
					st.takeItems(7159, 1);
					htmltext = "31520-2.htm";
				}
				else
				{
					htmltext = "31520-1a.htm";
				}
			}
			else if (cond == 6)
			{
				if (st.getQuestItemsCount(7076) > 0 && st.getQuestItemsCount(7077) > 0 && st.getQuestItemsCount(7078) > 0)
				{
					htmltext = "31520-4.htm";
				}
				else
				{
					htmltext = "31520-3a.htm";
				}
			}
			else if (cond == 7)
			{
				if (st.getQuestItemsCount(7113) > 0)
				{
					htmltext = "31520-6.htm";
				}
				else
				{
					htmltext = "31520-5a.htm";
				}
			}
		}
		else if (npcId == 31521)
		{
			if (st.getQuestItemsCount(7164) > 0)
			{
				st.takeItems(7164, 1);
				htmltext = "31521-0.htm";
			}
			else if (cond == 3)
			{
				htmltext = "31521-1a.htm";
			}
			else if (cond == 4)
			{
				htmltext = "31521-2.htm";
			}
			else if (cond == 5)
			{
				htmltext = "31521-3a.htm";
			}
		}
		else if (npcId == 31627)
		{
			if (st.getQuestItemsCount(7160) > 0)
			{
				htmltext = "31627-0.htm";
			}
			if (cond == 4)
			{
				htmltext = "31627-1a.htm";
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _037_PleaseMakeMeFormalWear(37, _037_PleaseMakeMeFormalWear.class.getSimpleName(), "");
	}
}