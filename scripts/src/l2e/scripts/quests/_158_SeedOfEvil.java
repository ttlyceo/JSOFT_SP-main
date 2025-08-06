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
 * Created by LordWinter 24.06.2011
 * Based on L2J Eternity-World
 */
public final class _158_SeedOfEvil extends Quest
{
	private static final String qn = "_158_SeedOfEvil";

	// Quest NPCs
	private static final int BIOTIN	     = 30031;

	// Quest items
	private static final int CLAY_TABLET = 1025;

	// Quest monsters
	private static final int NERKAS	     = 27016;

	private _158_SeedOfEvil(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(BIOTIN);
		addTalkId(BIOTIN);

		addKillId(NERKAS);

		questItemIds = new int[] { CLAY_TABLET };
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("1"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
				htmltext = "30031-04.htm";
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
				if (player.getLevel() < 21)
				{
					st.exitQuest(true);
					htmltext = "30031-02.htm";
				}
				else
				{
					htmltext = "30031-03.htm";
				}
      				break;
			case State.STARTED:
				if (st.getQuestItemsCount(CLAY_TABLET) != 0)
				{
					st.rewardItems(57, 1495);
					st.giveItems(956, 1);
					st.addExpAndSp(17818, 927);
					st.exitQuest(false, true);
					htmltext = "30031-06.htm";
				}
				else
				{
					htmltext = "30031-05.htm";
				}
	     			break;
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
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
		if (st != null && st.getQuestItemsCount(CLAY_TABLET) == 0)
		{
			st.giveItems(CLAY_TABLET, 1);
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "2");
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _158_SeedOfEvil(158, qn, "");
	}
}