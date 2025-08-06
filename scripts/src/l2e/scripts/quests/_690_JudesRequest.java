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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Created by LordWinter 02.04.2011
 * Based on L2J Eternity-World
 */
public class _690_JudesRequest extends Quest
{
	private static String qn = "_690_JudesRequest";

	// NPC
	private static final int JUDE = 32356;
	private static final int[] MOBS = {22398, 22399};

	// Items
	private static final int[] REWARDS = {9975, 9968, 9970, 10545, 9972, 9971, 9974, 9969, 10544, 9967, 10374, 10380, 10378, 10379, 10376, 10373, 10375, 10381, 10377};
	private static final int[] MAT = {9624, 9617, 9619, 9621, 9620, 9623, 9618, 9616, 10547, 10546, 10398, 10404, 10402, 10403, 10400, 10397, 10399, 10405, 10406, 10401, 10407};

	private static final int EVIL = 10327;

	// Chance (100% = 1000)
	private static final int DROP_CHANCE = 550;

	public _690_JudesRequest(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(JUDE);
		addTalkId(JUDE);

		for (final int i : MOBS)
		{
			addKillId(i);
		}

		questItemIds = new int[] { EVIL };
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

		final long evil = st.getQuestItemsCount(EVIL);

		if (event.equalsIgnoreCase("32356-03.htm"))
		{
			if (player.getLevel() >= 78 && st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
			else
			{
				htmltext = "32356-02.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equalsIgnoreCase("32356-07.htm"))
		{
			if (evil >= 200)
			{
				htmltext = "32356-07.htm";
				st.takeItems(EVIL, 200);
				st.giveItems(REWARDS[getRandom(REWARDS.length)], 1);
			}
			else
			{
				htmltext = "32356-05.htm";
			}
		}
		else if (event.equalsIgnoreCase("32356-08.htm"))
		{
			st.exitQuest(true);
			st.playSound("ItemSound.quest_giveup");
		}
		else if (event.equalsIgnoreCase("32356-09.htm"))
		{
			if (evil >= 5)
			{
				htmltext = "32356-09.htm";
				st.takeItems(EVIL, 5);
				st.giveItems(MAT[getRandom(MAT.length)], 1);
				st.giveItems(MAT[getRandom(MAT.length)], 1);
				st.giveItems(MAT[getRandom(MAT.length)], 1);
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

		final byte id = st.getState();
		final int cond = st.getInt("cond");
		final long evil = st.getQuestItemsCount(EVIL);
		if (id == State.CREATED)
		{
			if (player.getLevel() >= 78)
			{
				htmltext = "32356-01.htm";
			}
			else
			{
				htmltext = "32356-02.htm";
				st.exitQuest(true);
			}
		}
		else if (cond == 1 && evil >= 200)
		{
			htmltext = "32356-04.htm";
		}
		else if (cond == 1 && evil >= 5 && evil <= 200)
		{
			htmltext = "32356-05.htm";
		}
		else
		{
			htmltext = "32356-05a.htm";
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
			final long count = st.getQuestItemsCount(EVIL);
			int chance = (int) (DROP_CHANCE * Config.RATE_QUEST_DROP);
			int numItems = (chance / 1000);
			chance = chance % 1000;
			if (getRandom(1000) < chance)
			{
				numItems++;
			}
			if (numItems > 0)
			{
				if ((count + numItems) / 200 > count / 200)
				{
					st.playSound("ItemSound.quest_middle");
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
				st.giveItems(EVIL, numItems);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _690_JudesRequest(690, qn, "");
	}
}