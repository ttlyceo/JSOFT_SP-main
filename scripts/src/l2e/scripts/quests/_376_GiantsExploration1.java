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
 * Rework by LordWinter 23.05.2021
 */
public class _376_GiantsExploration1 extends Quest
{
	public _376_GiantsExploration1(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(31147);
		addTalkId(31147);

		addKillId(22670, 22671, 22672, 22673, 22674, 22675, 22676, 22677);

		questItemIds = new int[]
		{
		        14841
		};
	}

	private String onExchangeRequest(String event, QuestState st, int id, long rem)
	{
		if ((st.getQuestItemsCount(14836) >= rem) && (st.getQuestItemsCount(14837) >= rem) && (st.getQuestItemsCount(14838) >= rem) && (st.getQuestItemsCount(14839) >= rem) && (st.getQuestItemsCount(14840) >= rem))
		{
			st.takeItems(14836, rem);
			st.takeItems(14837, rem);
			st.takeItems(14838, rem);
			st.takeItems(14839, rem);
			st.takeItems(14840, rem);
			st.calcReward(getId(), id);
			st.playSound("ItemSound.quest_finish");
			return "31147-ok.htm";
		}
		return "31147-no.htm";
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31147-02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31147-quit.htm"))
		{
			st.exitQuest(true, true);
		}
		else if (isDigit(event))
		{
			final int id = Integer.parseInt(event);

			if (id == 9967)
			{
				htmltext = onExchangeRequest(event, st, 1, 10);
			}
			else if (id == 9968)
			{
				htmltext = onExchangeRequest(event, st, 2, 10);
			}
			else if (id == 9969)
			{
				htmltext = onExchangeRequest(event, st, 3, 10);
			}
			else if (id == 9970)
			{
				htmltext = onExchangeRequest(event, st, 4, 10);
			}
			else if (id == 9971)
			{
				htmltext = onExchangeRequest(event, st, 5, 10);
			}
			else if (id == 9972)
			{
				htmltext = onExchangeRequest(event, st, 6, 10);
			}
			else if (id == 9973)
			{
				htmltext = onExchangeRequest(event, st, 7, 10);
			}
			else if (id == 9974)
			{
				htmltext = onExchangeRequest(event, st, 8, 10);
			}
			else if (id == 9975)
			{
				htmltext = onExchangeRequest(event, st, 9, 10);
			}
			else if (id == 9628)
			{
				htmltext = onExchangeRequest(event, st, 10, 1);
			}
			else if (id == 9629)
			{
				htmltext = onExchangeRequest(event, st, 11, 1);
			}
			else if (id == 9630)
			{
				htmltext = onExchangeRequest(event, st, 12, 1);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (st.getState() == State.STARTED)
		{
			if ((st.getQuestItemsCount(14836) > 0) && (st.getQuestItemsCount(14837) > 0) && (st.getQuestItemsCount(14838) > 0) && (st.getQuestItemsCount(14839) > 0) && (st.getQuestItemsCount(14840) > 0))
			{
				htmltext = "31147-03.htm";
			}
			else
			{
				htmltext = "31147-02a.htm";
			}
		}
		else
		{
			if (player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "31147-01.htm";
			}
			else
			{
				htmltext = "31147-00.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 14841, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _376_GiantsExploration1(376, _376_GiantsExploration1.class.getSimpleName(), "");
	}
}
