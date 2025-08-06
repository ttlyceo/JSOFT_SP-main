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
 * Rework by LordWinter 17.06.2023
 */
public class _278_HomeSecurity extends Quest
{
	public _278_HomeSecurity(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31537);
		addTalkId(31537);

		addKillId(18906, 18907);
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

		final long count = st.getQuestItemsCount(15531);
		final long random = getRandom(3);
		
		if (event.equalsIgnoreCase("31537-03.htm"))
		{
			if (st.isCreated() && player.getLevel() >= getMinLvl(getId()))
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31537-05.htm"))
		{
			if (count >= 300)
			{
				st.takeItems(15531, 300);
				if (random == 0)
				{
					st.calcReward(getId(), 1);
				}
				else if (random == 1)
				{
					st.calcReward(getId(), 2);
					st.playSound("ItemSound.quest_middle");
				}
				else if (random == 2)
				{
					st.calcReward(getId(), 3);
					st.playSound("ItemSound.quest_middle");
				}
				htmltext = "31537-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("31537-08.htm"))
		{
			st.takeItems(15531, -1);
			st.exitQuest(true, true);
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
		final int npcId = npc.getId();
		final long count = st.getQuestItemsCount(15531);

		if (npcId == 31537)
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
				if (count < 300)
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
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 15531, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
		
	public static void main(String[] args)
	{
		new _278_HomeSecurity(278, _278_HomeSecurity.class.getSimpleName(), "");
	}
}