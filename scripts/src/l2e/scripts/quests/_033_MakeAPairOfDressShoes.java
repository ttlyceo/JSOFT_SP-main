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
public class _033_MakeAPairOfDressShoes extends Quest
{
	public _033_MakeAPairOfDressShoes(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30838);
		addTalkId(30838, 30164, 31520);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30838-1.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31520-1.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30838-3.htm"))
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("30838-5.htm"))
		{
			if (st.getQuestItemsCount(1882) >= 200 && st.getQuestItemsCount(1868) >= 600 && st.getQuestItemsCount(57) >= 200000)
			{
				st.takeItems(1882, 200);
				st.takeItems(1868, 600);
				st.takeItems(57, 200000);
				st.setCond(4, true);
			}
			else
			{
				htmltext = "30838-3a.htm";
			}
		}
		else if (event.equalsIgnoreCase("30164-1.htm"))
		{
			if (st.getQuestItemsCount(57) >= 300000)
			{
				st.takeItems(57, 300000);
				st.setCond(5, true);
			}
			else
			{
				htmltext = "30164-1b.htm";
			}
		}
		else if (event.equalsIgnoreCase("30838-7.htm"))
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
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}

		if (npcId == 30838)
		{
			if (cond == 0 && st.getQuestItemsCount(7113) == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					final QuestState fwear = player.getQuestState("_037_PleaseMakeMeFormalWear");
					if (fwear != null && fwear.get("cond") != null)
					{
						if (fwear.get("cond").equals("7"))
						{
							htmltext = "30838-0.htm";
						}
						else
						{
							htmltext = "30838-8.htm";
							st.exitQuest(true);
						}
					}
					else
					{
						htmltext = "30838-8.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30838-8.htm";
				}
			}
			else if (cond == 1)
			{
				htmltext = "30838-1a.htm";
			}
			else if (cond == 2)
			{
				htmltext = "30838-2.htm";
			}
			else if (cond == 3)
			{
				htmltext = "30838-4.htm";
			}
			else if (cond == 4)
			{
				htmltext = "30838-5a.htm";
			}
			else if (cond == 5)
			{
				htmltext = "30838-6.htm";
			}
		}
		else if (npcId == 31520)
		{
			if (cond == 1)
			{
				htmltext = "31520-0.htm";
			}
			else if (cond == 2)
			{
				htmltext = "31520-1a.htm";
			}
		}
		else if (npcId == 30164)
		{
			if (cond == 4)
			{
				htmltext = "30164-0.htm";
			}
			else if (cond == 5)
			{
				htmltext = "30164-1a.htm";
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _033_MakeAPairOfDressShoes(33, _033_MakeAPairOfDressShoes.class.getSimpleName(), "");
	}
}