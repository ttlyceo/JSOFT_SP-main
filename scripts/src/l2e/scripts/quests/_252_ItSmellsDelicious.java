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
 * Rework by LordWinter 16.08.2020
 */
public class _252_ItSmellsDelicious extends Quest
{
	public _252_ItSmellsDelicious(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30200);
		addTalkId(30200);
		
		addKillId(22786, 22787, 22788, 18908);

		questItemIds = new int[]
		{
		        15500, 15501
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
			
		if (event.equalsIgnoreCase("30200-05.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		
		if (event.equalsIgnoreCase("30200-08.htm"))
		{
			if (st.isCond(2))
			{
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		final int cond = st.getCond();
		String htmltext = getNoQuestMsg(player);
		
		if (npc.getId() == 30200)
		{
			if (st.getState() == State.CREATED && cond == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30200-01.htm";
				}
				else
				{
					htmltext = "30200-02.htm";
					st.exitQuest(true);
				}
			}
			else if (st.getState() == State.STARTED && cond == 1)
			{
				htmltext = "30200-06.htm";
			}
			else if (st.getState() == State.STARTED && cond == 2)
			{
				htmltext = "30200-07.htm";
			}
			else if (st.getState() == State.COMPLETED)
			{
				htmltext = "30200-03.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final QuestState st = partyMember.getQuestState(getName());
		if (st.isCond(1))
		{
			if (npc.getId() == 18908)
			{
				st.calcDoDropItems(getId(), 15501, npc.getId(), 5);
			}
			else
			{
				st.calcDoDropItems(getId(), 15500, npc.getId(), 10);
			}
			
			if (st.getQuestItemsCount(15501) == 5 && st.getQuestItemsCount(15500) == 10)
			{
				st.setCond(2, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _252_ItSmellsDelicious(252, _252_ItSmellsDelicious.class.getSimpleName(), "");
	}
}