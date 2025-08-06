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
 * Rework by LordWinter 09.06.2021
 */
public class _642_APowerfulPrimevalCreature extends Quest
{
	public _642_APowerfulPrimevalCreature(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(32105);
		addTalkId(32105);
		
		addKillId(22196, 22197, 22198, 22199, 22215, 22216, 22217, 22218, 22223, 18344);
		
		questItemIds = new int[]
		{
		        8774, 8775
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
		
		final long count_tissue = st.getQuestItemsCount(8774);
		final long count_egg = st.getQuestItemsCount(8775);
		
		if (event.equalsIgnoreCase("32105-04.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32105-06a.htm"))
		{
			if (count_tissue > 0)
			{
				st.takeItems(8774, -1);
				st.calcRewardPerItem(getId(), 1, (int) count_tissue);
			}
		}
		else if (event.equalsIgnoreCase("32105-07.htm"))
		{
			if ((count_tissue < 150) || (count_egg == 0))
			{
				htmltext = "32105-07a.htm";
			}
		}
		else if (isDigit(event))
		{
			if ((count_tissue >= 150) && (count_egg >= 1))
			{
				htmltext = "32105-08.htm";
				st.takeItems(8774, 150);
				st.takeItems(8775, 1);
				st.calcReward(getId(), Integer.parseInt(event));
			}
			else
			{
				htmltext = "32105-07a.htm";
			}
		}
		else if (event.equalsIgnoreCase("32105-10.htm"))
		{
			if (count_tissue >= 450)
			{
				htmltext = "32105-10.htm";
			}
			else
			{
				htmltext = "32105-11.htm";
			}
		}
		else if (event.equalsIgnoreCase("32105-09.htm"))
		{
			st.exitQuest(true, true);
		}
		else if (isDigit(event))
		{
			if (count_tissue >= 450)
			{
				htmltext = "32105-10.htm";
				st.takeItems(8774, 450);
				st.calcReward(getId(), Integer.parseInt(event));
			}
			else
			{
				htmltext = "32105-11.htm";
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
		
		final long count = st.getQuestItemsCount(8774);
		final int npcId = npc.getId();
		final int id = st.getState();
		final int cond = st.getCond();
		
		if (id == State.CREATED)
		{
			if ((npcId == 32105) & (cond == 0))
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "32105-01.htm";
				}
				else
				{
					htmltext = "32105-00.htm";
					st.exitQuest(true);
				}
			}
		}
		else if (id == State.STARTED)
		{
			if ((npcId == 32105) & (cond == 1))
			{
				if (count == 0)
				{
					htmltext = "32105-05.htm";
				}
				else
				{
					htmltext = "32105-06.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player member = getRandomPartyMemberState(player, State.STARTED);
		if (member != null)
		{
			final QuestState st = member.getQuestState(getName());
			if (st != null && st.isCond(1))
			{
				if (npc.getId() == 18344)
				{
					st.calcDropItems(getId(), 8775, npc.getId(), Integer.MAX_VALUE);
				}
				else
				{
					st.calcDropItems(getId(), 8774, npc.getId(), Integer.MAX_VALUE);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _642_APowerfulPrimevalCreature(642, _642_APowerfulPrimevalCreature.class.getSimpleName(), "");
	}
}