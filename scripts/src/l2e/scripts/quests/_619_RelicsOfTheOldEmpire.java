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
 * Rework by LordWinter 19.12.2022
 */
public class _619_RelicsOfTheOldEmpire extends Quest
{
  	public _619_RelicsOfTheOldEmpire(int questId, String name, String descr)
  	{
		super(questId, name, descr);
		
		addStartNpc(31538);
		addTalkId(31538);
		
		for (int id = 21396; id <= 21437; id++)
		{
			addKillId(id);
		}
		
		addKillId(21798, 21799, 21800);
		
		for (int id = 18212; id <= 18149; id++)
		{
			addKillId(id);
		}
		
		for (int id = 18166; id <= 18230; id++)
		{
			addKillId(id);
		}
		
		questItemIds = new int[]
		{
		        7254
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

		if (event.equalsIgnoreCase("31538-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31538-09.htm"))
		{
			if (st.getQuestItemsCount(7254) >= 1000)
			{
				htmltext = "31538-09.htm";
				st.takeItems(7254, 1000);
				st.calcReward(getId(), 1, true);
			}
			else
			{
				htmltext = "31538-06.htm";
			}
		}
		else if (event.equalsIgnoreCase("31538-10.htm"))
		{
			st.exitQuest(true, true);
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
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "31538-02.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "31538-01.htm";
				}
				break;
			case State.STARTED :
				if (st.getQuestItemsCount(7254) >= 1000)
				{
					htmltext = "31538-04.htm";
				}
				else if (st.getQuestItemsCount(7075) >= 1)
				{
					htmltext = "31538-06.htm";
				}
				else
				{
					htmltext = "31538-07.htm";
				}
				break;
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
			st.calcDropItems(getId(), 7254, npc.getId(), Integer.MAX_VALUE);
			st.calcDropItems(getId(), 7075, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
  	}

  	public static void main(String[] args)
  	{
		new _619_RelicsOfTheOldEmpire(619, _619_RelicsOfTheOldEmpire.class.getSimpleName(), "");
  	}
}