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

/**
 * Rework by LordWinter 08.12.2019
 */
public class _052_WilliesSpecialBait extends Quest
{
	public _052_WilliesSpecialBait(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(31574);
		addTalkId(31574);
		
		addKillId(20573);
		addKillId(20574);

		questItemIds = new int[]
		{
		        7623
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31574-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31574-07.htm"))
		{
			if (st.getQuestItemsCount(7623) < 100)
			{
				htmltext = "31574-07.htm";
			}
			else
			{
				htmltext = "31574-06.htm";
				st.takeItems(7623, -1);
				st.calcReward(getId());
				st.exitQuest(false, true);
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

		final int npcId = npc.getId();
		final int cond = st.getCond();
		
		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}

		if (npcId == 31574)
		{
			if (cond == 1)
			{
				htmltext = "31574-05.htm";
			}
			else if (cond == 2)
			{
				htmltext = "31574-04.htm";
			}
			else if (cond == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()) && player.getLevel() <= getMaxLvl(getId()))
				{
					htmltext = "31574-01.htm";
				}
				else
				{
					htmltext = "31574-02.htm";
					st.exitQuest(true);
				}
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
		if (st.calcDropItems(getId(), 7623, npc.getId(), 100))
		{
			st.setCond(2);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _052_WilliesSpecialBait(52, _052_WilliesSpecialBait.class.getSimpleName(), "");
	}
}