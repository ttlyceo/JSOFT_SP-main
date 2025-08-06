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

public class _261_CollectorsDream extends Quest
{
	private static final String qn = "_261_CollectorsDream";

	private final static int ALSHUPES = 30222;

	// Items
	private final static int GIANT_SPIDER_LEG = 1087;

	// Reward
	private final static int ADENA = 57;

	public _261_CollectorsDream(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(ALSHUPES);
		addTalkId(ALSHUPES);

		addKillId(20308, 20460, 20466);

		questItemIds = new int[]
		{
		                GIANT_SPIDER_LEG
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

		if (event.equalsIgnoreCase("30222-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
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
			case State.CREATED:
				if ((player.getLevel() >= 15) && (player.getLevel() <= 21))
				{
					htmltext = "30222-02.htm";
				}
				else
				{
					htmltext = "30222-01.htm";
					st.exitQuest(true);
				}
				break;

			case State.STARTED:
				if (st.getInt("cond") == 2)
				{
					htmltext = "30222-05.htm";
					st.takeItems(GIANT_SPIDER_LEG, -1);
					st.rewardItems(ADENA, 1000);
					st.addExpAndSp(2000, 0);
					final var qs = player.getQuestState("NewbieGuideSystem");
					if (qs != null && qs.getInt("finalStep") == 0)
					{
						qs.set("finalStep", 1);
						showOnScreenMsg(player, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
					}
					st.exitQuest(true, true);
				}
				else
				{
					htmltext = "30222-04.htm";
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
			if (st.getQuestItemsCount(GIANT_SPIDER_LEG) < 8)
			{
				st.giveItems(GIANT_SPIDER_LEG, 1);
				if (st.getQuestItemsCount(GIANT_SPIDER_LEG) == 8)
				{
					st.playSound("ItemSound.quest_middle");
					st.set("cond", "2");
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _261_CollectorsDream(261, qn, "");
	}
}
