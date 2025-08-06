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
 * Rework by LordWinter 09.06.2021
 */
public class _174_SupplyCheck extends Quest
{
	public _174_SupplyCheck(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32173);
		addTalkId(32173, 32170, 32167);
		
		questItemIds = new int[]
		{
		        9792, 9793
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
		
		if (event.equalsIgnoreCase("32173-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		String htmltext = getNoQuestMsg(player);
		if (st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		final int id = st.getState();
		
		if (id == State.COMPLETED)
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if ((id == State.CREATED) && (npcId == 32173))
		{
			if (player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "32173-01.htm";
			}
			else
			{
				htmltext = "32173-02.htm";
				st.exitQuest(true);
			}
		}
		else if (id == State.STARTED)
		{
			if (npcId == 32173)
			{
				if (cond == 1)
				{
					htmltext = "32173-04.htm";
				}
				else if (cond == 2)
				{
					st.setCond(3, true);
					st.takeItems(9792, -1);
					htmltext = "32173-05.htm";
				}
				else if (cond == 3)
				{
					htmltext = "32173-06.htm";
				}
				else if (cond == 4)
				{
					st.calcExpAndSp(getId());
					if (player.getClassId().isMage())
					{
						st.calcReward(getId(), 1);
					}
					else
					{
						st.calcReward(getId(), 2);
					}
					showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
					st.exitQuest(false, true);
					htmltext = "32173-07.htm";
				}
			}
			else if (npcId == 32170)
			{
				if (cond == 1)
				{
					st.setCond(2, true);
					st.giveItems(9792, 1);
					htmltext = "32170-01.htm";
				}
				else if (cond == 2)
				{
					htmltext = "32170-02.htm";
				}
			}
			else if (npcId == 32167)
			{
				if (cond == 3)
				{
					st.setCond(4, true);
					st.giveItems(9793, 1);
					htmltext = "32167-01.htm";
				}
				else if (cond == 4)
				{
					htmltext = "32167-02.htm";
				}
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _174_SupplyCheck(174, _174_SupplyCheck.class.getSimpleName(), "");
	}
}