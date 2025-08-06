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
 * Rework by LordWinter 22.12.2019
 */
public class _10290_LandDragonConqueror extends Quest
{
	public _10290_LandDragonConqueror(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30755);
		addTalkId(30755);
		
		addKillId(29019, 29066, 29067, 29068);
		
		questItemIds = new int[]
		{
		        15523, 15522
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
		
		if (event.equalsIgnoreCase("30755-07.htm"))
		{
			if (st.isCreated())
			{
				st.giveItems(15522, 1);
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
				if (player.getLevel() >= getMinLvl(getId()) && st.getQuestItemsCount(3865) >= 1)
				{
					htmltext = "30755-01.htm";
				}
				else if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "30755-02.htm";
				}
				else
				{
					htmltext = "30755-04.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(1) && st.getQuestItemsCount(15522) >= 1)
				{
					htmltext = "30755-08.htm";
				}
				else if (st.isCond(1) && st.getQuestItemsCount(15522) == 0)
				{
					st.giveItems(15522, 1);
					htmltext = "30755-09.htm";
				}
				else if (st.isCond(2))
				{
					st.takeItems(15523, 1);
					st.calcExpAndSp(getId());
					st.calcReward(getId());
					st.exitQuest(false, true);
					htmltext = "30755-10.htm";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = "30755-03.htm";
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
				if (qs != null && qs.isCond(1) && qs.calcDropItems(getId(), 15523, npc.getId(), 1))
				{
					qs.takeItems(15522, 1);
					qs.setCond(2);
				}
			}
		}
		else
		{
			if (st != null && st.isCond(1) && st.calcDropItems(getId(), 15523, npc.getId(), 1))
			{
				st.takeItems(15522, 1);
				st.setCond(2);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _10290_LandDragonConqueror(10290, _10290_LandDragonConqueror.class.getSimpleName(), "");
	}
}