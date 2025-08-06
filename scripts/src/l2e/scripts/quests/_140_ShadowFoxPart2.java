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
public class _140_ShadowFoxPart2 extends Quest
{
	public _140_ShadowFoxPart2(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addFirstTalkId(30895);
		addTalkId(30895, 30912);
		
		addKillId(20789, 20790, 20791, 20792);
		
		questItemIds = new int[]
		{
		        10347, 10348, 10349
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30895-02.htm") && npc.getId() == 30895)
		{
			st.setCond(1, true);
		}
		else if (event.equalsIgnoreCase("30895-05.htm") && npc.getId() == 30895)
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30895-09.htm") && npc.getId() == 30895)
		{
			if (st.isCond(4))
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
		else if (event.equalsIgnoreCase("30912-07.htm") && npc.getId() == 30912)
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("30912-09.htm") && npc.getId() == 30912)
		{
			if (st.isCond(3))
			{
				st.takeItems(10347, 5);
				if (st.getRandom(100) <= 60)
				{
					st.giveItems(10348, 1);
					if (st.getQuestItemsCount(10348) >= 3)
					{
						htmltext = "30912-09b.htm";
						st.takeItems(10347, -1);
						st.takeItems(10348, -1);
						st.giveItems(10349, 1);
						st.setCond(4, true);
					}
				}
				else
				{
					htmltext = "30912-09a.htm";
				}
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
		
		final QuestState qs = player.getQuestState("_139_ShadowFoxPart1");
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
		else if (npcId == 30895)
		{
			if (cond == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30895-01.htm";
				}
				else
				{
					htmltext = "30895-00.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "30895-02.htm";
			}
			else if (cond == 2 || cond == 3)
			{
				htmltext = "30895-06.htm";
			}
			else if (cond == 4)
			{
				if (cond == talk)
				{
					htmltext = "30895-08.htm";
				}
				else
				{
					htmltext = "30895-07.htm";
					st.takeItems(10349, -1);
					st.set("talk", "1");
				}
			}
		}
		else if (npcId == 30912)
		{
			if (cond == 2)
			{
				htmltext = "30912-01.htm";
			}
			else if (cond == 3)
			{
				if (st.getQuestItemsCount(10347) >= 5)
				{
					htmltext = "30912-08.htm";
				}
				else
				{
					htmltext = "30912-07.htm";
				}
			}
			else if (cond == 4)
			{
				htmltext = "30912-10.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 3);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 10347, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _140_ShadowFoxPart2(140, _140_ShadowFoxPart2.class.getSimpleName(), "");
	}
}
