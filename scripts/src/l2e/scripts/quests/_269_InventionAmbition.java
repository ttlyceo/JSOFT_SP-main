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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Created by LordWinter 18.01.2013 Based on L2J Eternity-World
 */
public class _269_InventionAmbition extends Quest
{
	private static final String qn = "_269_InventionAmbition";

	public final int INVENTOR_MARU = 32486;

	public final int[] MOBS =
	{
	                21124,
	                21125,
	                21126,
	                21127,
	                21128,
	                21129,
	                21130,
	                21131,
	};

	public final int ENERGY_ORES = 10866;

	public _269_InventionAmbition(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(INVENTOR_MARU);
		addTalkId(INVENTOR_MARU);

		for (final int mob : MOBS)
		{
			addKillId(mob);
		}

		questItemIds = new int[]
		{
		                ENERGY_ORES
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return null;
		}

		if (event.equals("32486-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equals("32486-05.htm"))
		{
			st.exitQuest(true, true);
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() < 18)
				{
					htmltext = "32486-00.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "32486-01.htm";
				}
				break;
			case State.STARTED:
				final long count = st.getQuestItemsCount(ENERGY_ORES);
				if (count > 0)
				{
					st.giveItems(57, (count * 50) + (2044 * (count / 20)));
					st.takeItems(ENERGY_ORES, -1);
					htmltext = "32486-07.htm";
				}
				else
				{
					htmltext = "32486-04.htm";
				}
				break;
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (Rnd.chance(60))
			{
				st.giveItems(ENERGY_ORES, 1);
				st.playSound("ItemSound.quest_itemget");
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _269_InventionAmbition(269, qn, "");
	}
}
