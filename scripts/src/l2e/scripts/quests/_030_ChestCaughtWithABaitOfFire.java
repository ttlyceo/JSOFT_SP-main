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
 * Rework by LordWinter 06.12.2019
 */
public class _030_ChestCaughtWithABaitOfFire extends Quest
{
	public _030_ChestCaughtWithABaitOfFire(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31577);
		addTalkId(31577, 30629);
		
		questItemIds = new int[]
		{
		        7628
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

		if (event.equalsIgnoreCase("31577-02.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31577-04a.htm"))
		{
			if (st.getQuestItemsCount(6511) > 0)
			{
				st.takeItems(6511, 1);
				st.giveItems(7628, 1);
				st.setCond(2, true);
				htmltext = "31577-04";
			}
		}
		else if (event.equalsIgnoreCase("30629-02.htm"))
		{
			if (st.getQuestItemsCount(7628) == 1)
			{
				st.takeItems(7628, -1);
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
			else
			{
				htmltext = "30629-03.htm";
				st.exitQuest(true);
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
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					final QuestState qs = player.getQuestState("_053_LinnaeusSpecialBait");
					if (qs != null)
					{
						if (qs.isCompleted())
						{
							htmltext = "31577-01.htm";
						}
						else
						{
							htmltext = "31577-03a.htm";
							st.exitQuest(true);
						}
					}
					else
					{
						htmltext = "31577-03a.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "31577-03a.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				if (npc.getId() == 31577)
				{
					if (cond == 1)
					{
						htmltext = "31577-03.htm";
					}
					else if (cond == 2)
					{
						htmltext = "31577-05.htm";
					}
				}
				else if (npc.getId() == 30629)
				{
					if (cond == 2)
					{
						htmltext = "30629-01.htm";
					}
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _030_ChestCaughtWithABaitOfFire(30, _030_ChestCaughtWithABaitOfFire.class.getSimpleName(), "");
	}
}