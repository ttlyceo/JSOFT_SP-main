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
 * Rework by LordWinter 25.02.2020
 */
public class _146_TheZeroHour extends Quest
{
	public _146_TheZeroHour(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31554);
		addTalkId(31554);
		
		addKillId(25671);
		
		questItemIds = new int[]
		{
		        14859
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("31554-02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("reward"))
		{
			if (st.isCond(2) && st.getQuestItemsCount(14859) >= 1)
			{
				htmltext = "31554-06.htm";
				st.takeItems(14859, 1);
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
			else
			{
				htmltext = "31554-05.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					final QuestState st1 = player.getQuestState("_109_InSearchOfTheNest");
					if (st1 != null && st1.isCompleted())
					{
						htmltext = "31554-01.htm";
					}
					else
					{
						htmltext = "31554-00.htm";
					}
				}
				else
				{
					htmltext = "31554-03.htm";
				}
				break;
			case State.STARTED :
				if (st.isCond(1) || st.isCond(2))
				{
					htmltext = "31554-04.htm";
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null && st.isCond(1) && st.calcDropItems(getId(), 14859, npc.getId(), 1))
		{
			st.setCond(2, true);
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new _146_TheZeroHour(146, _146_TheZeroHour.class.getSimpleName(), "");
	}
}