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
public class _631_DeliciousTopChoiceMeat extends Quest
{
	public _631_DeliciousTopChoiceMeat(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31537);
		addTalkId(31537);

		addKillId(18878, 18879, 18885, 18886, 18892, 18893, 18899, 18900);
		
		questItemIds = new int[]
		{
		        15534
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

		if (event.equalsIgnoreCase("31537-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31537-05.htm"))
		{
			if (st.getQuestItemsCount(15534) >= 120)
			{
				st.takeItems(15534, 120);
				st.calcReward(getId(), getRandom(1, 3), true);
				st.playSound("ItemSound.quest_middle");
				htmltext = "31537-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("31537-08.htm"))
		{
			st.takeItems(15534, -1);
			st.playSound("ItemSound.quest_finish");
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

		final int id = st.getState();
		final int cond = st.getCond();
		final long count = st.getQuestItemsCount(15534);
		if (npc.getId() == 31537)
		{
			if(id == State.CREATED && cond == 0)
			{
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "31537-02.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "31537-01.htm";
				}
	
			}
			else if(id == State.STARTED && cond == 1)
			{
				if (count < 120)
				{
					htmltext = "31537-05.htm";
				}
				else
				{
					htmltext = "31537-04.htm";
				}
			}
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember != null)
		{
			final QuestState st = partyMember.getQuestState(getName());
			if (st != null)
			{
				st.calcDropItems(getId(), 15534, npc.getId(), Integer.MAX_VALUE);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
		
	public static void main(String[] args)
	{
		new _631_DeliciousTopChoiceMeat(631, _631_DeliciousTopChoiceMeat.class.getSimpleName(), "");
	}
}