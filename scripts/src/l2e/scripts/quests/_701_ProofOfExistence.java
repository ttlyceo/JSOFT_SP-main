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

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Created by LordWinter 18.06.2012 Based on L2J Eternity-World
 */
public final class _701_ProofOfExistence extends Quest
{
	private static final String qn = "_701_ProofOfExistence";

	private static int ARTIUS = 32559;
	private static int DEADMANS_REMAINS = 13875;

	private static int[] MOBS =
	{
	        22606, 22607, 22608, 22609
	};

	private static int DROP_CHANCE = 80;

	public _701_ProofOfExistence(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(ARTIUS);
		addTalkId(ARTIUS);

		for (final int i : MOBS)
		{
			addKillId(i);
		}

		questItemIds = new int[]
		{
		        DEADMANS_REMAINS
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

		if (event.equalsIgnoreCase("32559-03.htm"))
		{
			if (st.isCreated())
			{
				st.setState(State.STARTED);
				st.set("cond", "1");
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("32559-quit.htm"))
		{
			st.exitQuest(true);
			st.playSound("ItemSound.quest_finish");
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
			case State.CREATED :
				final QuestState first = player.getQuestState("_10273_GoodDayToFly");
				if ((player.getLevel() >= 78) && (first != null) && (first.isCompleted()))
				{
					htmltext = "32559-01.htm";
				}
				else
				{
					htmltext = "32559-00.htm";
				}
				break;
			case State.STARTED :
				final int itemcount = (int) st.getQuestItemsCount(DEADMANS_REMAINS);
				if (itemcount > 0)
				{
					st.takeItems(DEADMANS_REMAINS, -1L);
					st.rewardItems(57, itemcount * 2500);
					st.playSound("ItemSound.quest_itemget");
					htmltext = "32559-06.htm";
				}
				else
				{
					htmltext = "32559-04.htm";
				}
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (ArrayUtils.contains(MOBS, npc.getId()))
			{
				final int chance = (int) (DROP_CHANCE * Config.RATE_QUEST_DROP);
				int numItems = chance / 100;
				if (st.getRandom(100) < chance)
				{
					numItems++;
				}
				if (numItems > 0)
				{
					st.giveItems(DEADMANS_REMAINS, 1L);
				}
				st.playSound("ItemSound.quest_itemget");
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _701_ProofOfExistence(701, qn, "");
	}
}