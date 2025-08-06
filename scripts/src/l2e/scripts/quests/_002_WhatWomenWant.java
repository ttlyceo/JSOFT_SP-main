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
import l2e.gameserver.network.NpcStringId;

/**
 * Rework by LordWinter 04.12.2019
 */
public class _002_WhatWomenWant extends Quest
{
	public _002_WhatWomenWant(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30223);
		addTalkId(30223, 30146, 30150, 30157);
		
		questItemIds = new int[]
		{
		        1092, 1093, 1094, 689, 693
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

		if (event.equalsIgnoreCase("30223-04.htm") && npc.getId() == 30223)
		{
			st.startQuest();
			st.giveItems(1092, 1);
		}
		else if (event.equalsIgnoreCase("30223-08.htm") && npc.getId() == 30223)
		{
			if (st.isCond(3))
			{
				st.takeItems(1094, -1);
				st.giveItems(689, 1);
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("30223-10.htm") && npc.getId() == 30223)
		{
			if (st.isCond(3))
			{
				st.takeItems(1094, -1);
				st.calcExpAndSp(getId());
				st.calcReward(getId(), 1);
				st.exitQuest(false, true);
				showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
			}
			return null;
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
		
		final int npcId = npc.getId();
		final int cond = st.getCond();

		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 30223)
				{
					if (((player.getRace().ordinal() == 1) || (player.getRace().ordinal() == 0)) && (player.getLevel() >= getMinLvl(getId())))
					{
						htmltext = "30223-02.htm";
					}
					else
					{
						htmltext = "30223-01.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED:
				if (npcId == 30223)
				{
					switch (cond)
					{
						case 1:
							if (st.getQuestItemsCount(1092) > 0)
							{
								htmltext = "30223-05.htm";
							}
							break;
						case 2:
							if (st.getQuestItemsCount(1093) > 0)
							{
								htmltext = "30223-06.htm";
							}
							break;
						case 3:
							if (st.getQuestItemsCount(1094) > 0)
							{
								htmltext = "30223-07.htm";
							}
							break;
						case 4:
							if (st.getQuestItemsCount(689) > 0)
							{
								htmltext = "30223-11.htm";
							}
							break;
						case 5:
							if (st.getQuestItemsCount(693) > 0)
							{
								htmltext = "30223-10.htm";
								st.takeItems(693, -1);
								st.calcExpAndSp(getId());
								st.calcReward(getId(), 2);
								st.exitQuest(false, true);
							}
							break;
					}
				}
				else if (npcId == 30146)
				{
					switch (cond)
					{
						case 1:
							if (st.getQuestItemsCount(1092) > 0)
							{
								htmltext = "30146-01.htm";
								st.takeItems(1092, -1);
								st.giveItems(1093, 1);
								st.setCond(2, true);
							}
							break;
						case 2:
							if (st.getQuestItemsCount(1093) > 0)
							{
								htmltext = "30146-02.htm";
							}
							break;
					}
				}
				else if (npcId == 30150)
				{
					switch (cond)
					{
						case 2:
							if (st.getQuestItemsCount(1093) > 0)
							{
								htmltext = "30150-01.htm";
								st.takeItems(1093, -1);
								st.giveItems(1094, 1);
								st.setCond(3, true);
							}
							break;
						case 3:
							if (st.getQuestItemsCount(1094) > 0)
							{
								htmltext = "30150-02.htm";
							}
							break;
					}
				}
				else if (npcId == 30157)
				{
					switch (cond)
					{
						case 4:
							if (st.getQuestItemsCount(689) > 0)
							{
								htmltext = "30157-01.htm";
								st.takeItems(689, -1);
								st.giveItems(693, 1);
								st.setCond(5, true);
							}
							break;
						case 5:
							if (st.getQuestItemsCount(693) > 0)
							{
								htmltext = "30157-02.htm";
							}
							break;
					}
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _002_WhatWomenWant(2, _002_WhatWomenWant.class.getSimpleName(), "");
	}
}
