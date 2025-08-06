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
 * Rework by LordWinter 03.03.2020
 */
public final class _637_ThroughOnceMore extends Quest
{
	public _637_ThroughOnceMore(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32010);
		addTalkId(32010);

		addKillId(21565, 21566, 21567);
		
		questItemIds = new int[]
		{
		        8066
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32010-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32010-10.htm"))
		{
			st.exitQuest(true);
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if ((player.getLevel() >= getMinLvl(getId())) && (st.getQuestItemsCount(8064) > 0) && (st.getQuestItemsCount(8067) == 0))
				{
					htmltext = "32010-02.htm";
				}
				else
				{
					htmltext = "32010-01.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if (st.isCond(2) && (st.getQuestItemsCount(8066) >= 10))
				{
					st.takeItems(8066, -1);
					st.takeItems(8064, -1);
					st.takeItems(8065, 1);
					st.calcReward(getId());
					st.exitQuest(true, true);
					htmltext = "32010-05.htm";
				}
				else
				{
					htmltext = "32010-04.htm";
				}
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
			if (st.calcDropItems(getId(), 8066, npc.getId(), 10))
			{
				st.setCond(2);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _637_ThroughOnceMore(637, _637_ThroughOnceMore.class.getSimpleName(), "");
	}
}