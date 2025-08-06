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
 * Rework by LordWinter 13.03.2023
 */
public class _139_ShadowFoxPart1 extends Quest
{
	public _139_ShadowFoxPart1(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addFirstTalkId(30896);
		addTalkId(30896);
		
		addKillId(20784, 20785);
		
		questItemIds = new int[]
		{
		        10345, 10346
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30896-03.htm"))
		{
			st.setCond(1, true);
		}
		else if (event.equalsIgnoreCase("30896-11.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30896-14.htm"))
		{
			st.takeItems(10345, -1);
			st.takeItems(10346, -1);
			st.set("talk", "1");
		}
		else if (event.equalsIgnoreCase("30896-16.htm"))
		{
			if (st.isCond(2))
			{
				st.unset("talk");
				if (player.getLevel() >= 37 && player.getLevel() <= 42)
				{
					st.calcExpAndSp(getId());
				}
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		final QuestState qs = player.getQuestState("_138_TempleChampionPart2");
		if (qs != null)
		{
			if (qs.getState() == State.COMPLETED && st.getState() == State.CREATED)
			{
				st.setState(State.STARTED);
			}
		}
		npc.showChatWindow(player);
		return null;
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
		
		final int npcId = npc.getId();
		final int id = st.getState();
		final int cond = st.getCond();
		final int talk = st.getInt("talk");
		
		if (id == State.CREATED)
		{
			return htmltext;
		}
		if (id == State.COMPLETED)
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (npcId == 30896)
		{
			if (cond == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30896-01.htm";
				}
				else
				{
					htmltext = "30896-00.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "30896-03.htm";
			}
			else if (cond == 2)
			{
				if (st.getQuestItemsCount(10345) >= 10 && st.getQuestItemsCount(10346) >= 1)
				{
					htmltext = "30896-13.htm";
				}
				else if (cond == talk)
				{
					htmltext = "30896-14.htm";
				}
				else
				{
					htmltext = "30896-12.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 2);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 10345, npc.getId(), 10);
			st.calcDropItems(getId(), 10346, npc.getId(), 1);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _139_ShadowFoxPart1(139, _139_ShadowFoxPart1.class.getSimpleName(), "");
	}
}