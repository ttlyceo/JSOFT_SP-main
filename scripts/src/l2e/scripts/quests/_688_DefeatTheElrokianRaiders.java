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
 * Rework by LordWinter 22.04.2020
 */
public class _688_DefeatTheElrokianRaiders extends Quest
{
	public _688_DefeatTheElrokianRaiders(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(32105);
		addTalkId(32105);
		
		addKillId(22214);
		
		questItemIds = new int[]
		{
		        8785
		};
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

		final long count = st.getQuestItemsCount(8785);

		if(event.equalsIgnoreCase("32105-02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if(event.equalsIgnoreCase("32105-08.htm"))
		{
			if(count > 0)
			{
				st.takeItems(8785, -1);
				st.giveItems(57, count * 3000);
			}
			st.exitQuest(true, true);
		}
		else if(event.equalsIgnoreCase("32105-06.htm"))
		{
			if(count > 0)
			{
				st.takeItems(8785, -1);
				st.giveItems(57, count * 3000);
			}
			else
			{
				htmltext = "32105-06a.htm";
			}
		}
		else if(event.equalsIgnoreCase("32105-07.htm"))
		{
			if(count >= 100)
			{
				st.takeItems(8785, 100);
				st.calcReward(getId());
			}
			else
			{
				htmltext = "32105-07a.htm";
			}
		}
		else if(event.equalsIgnoreCase("None"))
		{
			htmltext = null;
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
		
		switch (st.getState())
		{
			case State.CREATED :
			{
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32105-01.htm" : "32105-00.htm";
				break;
			}
			case State.STARTED :
			{
				htmltext = (st.hasQuestItems(8785)) ? "32105-05.htm" : "32105-06a.htm";
				break;
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDoDropItems(getId(), 8785, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _688_DefeatTheElrokianRaiders(688, _688_DefeatTheElrokianRaiders.class.getSimpleName(), "");
	}
}