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
 * Rework by LordWinter 21.04.2020
 */
public class _617_GatherTheFlames extends Quest
{
	public _617_GatherTheFlames(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31271, 31539);
		addTalkId(32049, 31271, 31539);
		
		for (int mobs = 22634; mobs < 22650; mobs++)
		{
			addKillId(mobs);
		}
		
		for (int mobs = 18799; mobs < 18804; mobs++)
		{
			addKillId(mobs);
		}
		
		questItemIds = new int[]
		{
		        7264
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
		
		final long torches = st.getQuestItemsCount(7264);
		
		if (event.equalsIgnoreCase("31539-03.htm"))
		{
			if (player.getLevel() >= 74)
			{
				st.startQuest();
			}
			else
			{
				htmltext = "31539-02.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equalsIgnoreCase("31271-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31539-05.htm") && (torches >= 1000))
		{
			htmltext = "31539-07.htm";
			st.takeItems(7264, 1000);
			st.calcReward(getId(), getRandom(1, 10));
		}
		else if (event.equalsIgnoreCase("31539-08.htm"))
		{
			st.takeItems(7264, -1);
			st.exitQuest(true);
		}
		else if (event.startsWith("reward"))
		{
			final int rewardId = Integer.parseInt(event.substring(7));
			if (rewardId > 0)
			{
				if (torches >= 1200)
				{
					st.takeItems(7264, 1200);
					st.calcReward(getId(), rewardId);
					return null;
				}
				htmltext = "Incorrect item count";
			}
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
		
		switch (npc.getId())
		{
			case 31539 :
				if (st.isCreated())
				{
					htmltext = (player.getLevel() >= getMinLvl(getId())) ? "31539-01.htm" : "31539-02.htm";
				}
				else
				{
					htmltext = (st.getQuestItemsCount(7264) >= 1000) ? "31539-04.htm" : "31539-05.htm";
				}
				break;
			case 31271 :
				if (st.isCreated())
				{
					htmltext = (player.getLevel() >= 74) ? "31271-01.htm" : "31271-02.htm";
				}
				else
				{
					htmltext = "31271-04.htm";
				}
				break;
			case 32049 :
				if (st.isStarted())
				{
					htmltext = (st.getQuestItemsCount(7264) >= 1200) ? "32049-01.htm" : "32049-02.htm";
				}
				break;
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
			st.calcDropItems(getId(), 7264, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _617_GatherTheFlames(617, _617_GatherTheFlames.class.getSimpleName(), "");
	}
}