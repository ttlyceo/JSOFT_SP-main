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
 * Rework by LordWinter 26.05.2021
 */
public class _382_KailsMagicCoin extends Quest
{
	public _382_KailsMagicCoin(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30687);
		addTalkId(30687);

		addKillId(21017, 21019, 21020, 21022);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}

		if(event.equalsIgnoreCase("30687-03.htm"))
		{
			if (player.getLevel() >= getMinLvl(getId()) && st.getQuestItemsCount(5898) > 0)
			{
				st.startQuest();
			}
			else
			{
				htmltext = "30687-01.htm";
				st.exitQuest(true);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int cond = st.getCond();

		if (st.getQuestItemsCount(5898) == 0 || player.getLevel() < getMinLvl(getId()))
		{
			htmltext = "30687-01.htm";
			st.exitQuest(true);
		}
		else if(cond == 0)
		{
			htmltext = "30687-02.htm";
		}
		else
		{
			htmltext = "30687-04.htm";
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player member = getRandomPartyMemberState(player, State.STARTED);
		if (member != null)
		{
			final QuestState st = member.getQuestState(getName());
			if (st != null && st.getQuestItemsCount(5898) > 0)
			{
				if (npc.getId() == 21022)
				{
					st.calcDropItems(getId(), 5961 + getRandom(3), npc.getId(), Integer.MAX_VALUE);
				}
				else
				{
					final int itemId = npc.getId() == 21017 ? 5961 : npc.getId() == 21019 ? 5962 : 5963;
					st.calcDropItems(getId(), itemId, npc.getId(), Integer.MAX_VALUE);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _382_KailsMagicCoin(382, _382_KailsMagicCoin.class.getSimpleName(), "");
	}
}