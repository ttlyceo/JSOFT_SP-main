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
public class _027_ChestCaughtWithABaitOfWind extends Quest
{
	public _027_ChestCaughtWithABaitOfWind(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31570);
		addTalkId(31570, 31434);
		
		questItemIds = new int[]
		{
		        7625
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

		if (event.equalsIgnoreCase("31570-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31570-07.htm"))
		{
			if (st.getQuestItemsCount(6500) > 0)
			{
				st.takeItems(6500, 1);
				st.giveItems(7625, 1);
				st.setCond(2, true);
			}
			else
			{
				htmltext = "31570-08.htm";
			}
		}
		else if (event.equalsIgnoreCase("31434-02.htm"))
		{
			if (st.getQuestItemsCount(7625) == 1)
			{
				st.takeItems(7625, -1);
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
			else
			{
				htmltext = "31434-03.htm";
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
				final QuestState qs = player.getQuestState("_050_LanoscosSpecialBait");
				if (qs != null && qs.isCompleted() && player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "31570-01.htm";
				}
				else
				{
					htmltext = "31570-02.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				switch (npc.getId())
				{
					case 31570 :
						switch (cond)
						{
							case 1:
								if (st.getQuestItemsCount(6500) == 0)
								{
									htmltext = "31570-06.htm";
								}
								else
								{
									htmltext = "31570-05.htm";
								}
								break;
							case 2:
								htmltext = "31570-09.htm";
								break;
						}
						break;
					case 31434 :
						switch (cond)
						{
							case 2:
								htmltext = "31434-01.htm";
								break;
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _027_ChestCaughtWithABaitOfWind(27, _027_ChestCaughtWithABaitOfWind.class.getSimpleName(), "");
	}
}