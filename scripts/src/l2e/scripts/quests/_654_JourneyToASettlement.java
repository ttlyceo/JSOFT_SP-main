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
 * Rework by LordWinter 11.03.2022
 */
public final class _654_JourneyToASettlement extends Quest
{
	public _654_JourneyToASettlement(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31453);
		addTalkId(31453);
		
		addKillId(21294, 21295);
		
		questItemIds = new int[]
		{
		        8072
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("31453-02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31453-03.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31453-05.htm") && st.hasQuestItems(8072))
		{
			if (st.isCond(3))
			{
				st.takeItems(8072, 1);
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		String htmltext = getNoQuestMsg(player);
		
		if (st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.CREATED:
				final QuestState qs = player.getQuestState(_119_LastImperialPrince.class.getSimpleName());
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "31453-06.htm";
					st.exitQuest(true);
				}
				else if ((qs == null) || !qs.isCompleted())
				{
					htmltext = "31453-07.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "31453-01.htm";
				}
				break;
			case State.STARTED:
				if ((npcId == 31453) && (cond != 3))
				{
					htmltext = "31453-02.htm";
				}
				else if ((npcId == 31453) && (cond == 3))
				{
					htmltext = "31453-04.htm";
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 2);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null && st.calcDropItems(getId(), 8072, npc.getId(), 1))
		{
			st.setCond(3, true);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _654_JourneyToASettlement(654, _654_JourneyToASettlement.class.getSimpleName(), "");
	}
}