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
 * Created by LordWinter 13.01.2013 Based on L2J Eternity-World
 */
public class _359_ForSleeplessDeadmen extends Quest
{
	private static final String qn = "_359_ForSleeplessDeadmen";
	
	private static final int ORVEN = 30857;
	
	private static final int REMAINS = 5869;
	
	private static final int REWARD[] =
	{
		6341,
		6342,
		6343,
		6344,
		6345,
		6346,
		5494,
		5495
	};
	
	public _359_ForSleeplessDeadmen(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(ORVEN);
		addTalkId(ORVEN);
		
		addKillId(21006, 21007, 21008);

		questItemIds = new int[]
		{
			REMAINS
		};
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
		
		if (event.equalsIgnoreCase("30857-06.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30857-10.htm"))
		{
			if (st.isCond(3))
			{
				st.giveItems(REWARD[getRandom(REWARD.length)], 4);
				st.playSound("ItemSound.quest_finish");
				st.exitQuest(true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= 60)
				{
					htmltext = "30857-02.htm";
				}
				else
				{
					htmltext = "30857-01.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
				{
					htmltext = "30857-07.htm";
				}
				else if (cond == 2)
				{
					htmltext = "30857-08.htm";
					st.set("cond", "3");
					st.playSound("ItemSound.quest_middle");
					st.takeItems(REMAINS, -1);
				}
				else if (cond == 3)
				{
					htmltext = "30857-09.htm";
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
			if (st.dropItems(REMAINS, 1, 60, 100000))
			{
				st.set("cond", "2");
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _359_ForSleeplessDeadmen(359, qn, "");
	}
}