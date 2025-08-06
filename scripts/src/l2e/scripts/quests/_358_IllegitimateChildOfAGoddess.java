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
 * Created by LordWinter 01.01.2013
 * Based on L2J Eternity-World
 */
public class _358_IllegitimateChildOfAGoddess extends Quest
{
	private static final String qn = "_358_IllegitimateChildOfAGoddess";
	
	private static final int SCALE = 5868;
	private static final int[] REWARD =
	{
	        6329, 6331, 6333, 6335, 6337, 6339, 5364, 5366
	};
	
	public _358_IllegitimateChildOfAGoddess(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30862);
		addTalkId(30862);
		
		addKillId(20672, 20673);
		
		questItemIds = new int[]
		{
		        SCALE
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30862-05.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30862-07.htm"))
		{
			if (st.getQuestItemsCount(SCALE) >= 108)
			{
				st.takeItems(SCALE, -1);
				st.giveItems(REWARD[getRandom(REWARD.length)], 1);
				st.playSound("ItemSound.quest_finish");
				st.exitQuest(true);
			}
			else
			{
				htmltext = "30862-04.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg(player);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
    		{
			case State.CREATED :
				if (player.getLevel() >= 63)
				{
					htmltext = "30862-02.htm";
				}
				else
				{
					htmltext = "30862-01.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				if (st.getQuestItemsCount(SCALE) >= 108)
				{
					htmltext = "30862-03.htm";
				}
				else
				{
					htmltext = "30862-04.htm";
				}
				break;
    		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (st.dropQuestItems(SCALE, 1, 108, 700000, true))
    		{
				st.set("cond", "2");
    		}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _358_IllegitimateChildOfAGoddess(358, qn, "");
	}
}