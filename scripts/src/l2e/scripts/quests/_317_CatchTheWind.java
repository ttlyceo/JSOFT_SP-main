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
 * Created by LordWinter 30.09.2012
 * Based on L2J Eternity-World
 */
public class _317_CatchTheWind extends Quest
{
	private static final String qn = "_317_CatchTheWind";
	
	// NPC
	private static final int RIZRAELL = 30361;

	// Item
	private static final int WIND_SHARD = 1078;
	
	public _317_CatchTheWind(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(RIZRAELL);
		addTalkId(RIZRAELL);

		addKillId(20036, 20044);

		questItemIds = new int[] { WIND_SHARD };
	}
		
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30361-04.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30361-08.htm"))
		{
			st.playSound("ItemSound.quest_finish");
			st.exitQuest(true);
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
			case State.CREATED:
				if (player.getLevel() >= 18 && player.getLevel() <= 23)
				{
					htmltext = "30361-03.htm";
				}
				else
				{
					htmltext = "30361-02.htm";
					st.exitQuest(true);
				}
				break;
			
			case State.STARTED:
				final long shards = st.getQuestItemsCount(WIND_SHARD);
				if (shards == 0)
				{
					htmltext = "30361-05.htm";
				}
				else
				{
					final long reward = 40 * shards + (shards >= 10 ? 2988 : 0);
					htmltext = "30361-07.htm";
					st.takeItems(WIND_SHARD, -1);
					st.rewardItems(57, reward);
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (st.getRandom(100) < 50)
			{
				st.giveItems(WIND_SHARD, 1);
				st.playSound("ItemSound.quest_itemget");
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _317_CatchTheWind(317, qn, "");
	}
}