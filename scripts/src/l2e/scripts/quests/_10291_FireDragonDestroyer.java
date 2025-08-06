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
 * Rework by LordWinter 25.12.2019
 */
public class _10291_FireDragonDestroyer extends Quest
{
	public _10291_FireDragonDestroyer(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31540);
		addTalkId(31540);
		
		addKillId(29028);
		
		questItemIds = new int[]
		{
		        15524, 15525
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("31540-07.htm"))
		{
			if (st.isCreated())
			{
				st.giveItems(15524, 1);
				st.startQuest();
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
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (player.getLevel() >= getMinLvl(getId()) && st.getQuestItemsCount(7267) >= 1)
				{
					htmltext = "31540-01.htm";
				}
				else if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "31540-02.htm";
				}
				else
				{
					htmltext = "31540-04.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(1) && st.getQuestItemsCount(15524) >= 1)
				{
					htmltext = "31540-08.htm";
				}
				else if (st.isCond(1) && st.getQuestItemsCount(15524) == 0)
				{
					st.giveItems(15524, 1);
					htmltext = "31540-09.htm";
				}
				else if (st.isCond(2))
				{
					st.takeItems(15525, 1);
					st.calcExpAndSp(getId());
					st.calcReward(getId());
					st.exitQuest(false, true);
					htmltext = "31540-10.htm";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = "31540-03.htm";
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		if (player.getParty() != null)
		{
			for (final Player partyMember : player.getParty().getMembers())
			{
				final QuestState qs = partyMember.getQuestState(getName());
				if (qs != null && qs.isCond(1) && qs.calcDropItems(getId(), 15525, npc.getId(), 1))
				{
					qs.takeItems(15524, 1);
					qs.setCond(2);
				}
			}
		}
		else
		{
			if (st != null && st.isCond(1) && st.calcDropItems(getId(), 15525, npc.getId(), 1))
			{
				st.takeItems(15524, 1);
				st.setCond(2);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _10291_FireDragonDestroyer(10291, _10291_FireDragonDestroyer.class.getSimpleName(), "");
	}
}