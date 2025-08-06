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

import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 13.03.2023
 */
public class _141_ShadowFoxPart3 extends Quest
{
	public _141_ShadowFoxPart3(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addFirstTalkId(30894);
		addTalkId(30894);
		
		addKillId(20791, 20792, 20135);
		
		questItemIds = new int[]
		{
		        10350
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
		
		if (event.equalsIgnoreCase("30894-02.htm"))
		{
			st.setCond(1, true);
		}
		else if (event.equalsIgnoreCase("30894-04.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30894-15.htm"))
		{
			if (st.isCond(3))
			{
				st.unset("talk");
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("30894-18.htm"))
		{
			if (st.isCond(4))
			{
				st.unset("talk");
				if ((player.getLevel() >= 37) && (player.getLevel() <= 42))
				{
					st.calcExpAndSp(getId());
				}
				st.calcReward(getId());
				st.exitQuest(false, true);
				
				QuestState qs = player.getQuestState("_998_FallenAngelSelect");
				if (qs == null)
				{
					final Quest q = QuestManager.getInstance().getQuest("_998_FallenAngelSelect");
					if (q != null)
					{
						qs = q.newQuestState(player);
						qs.setState(State.STARTED);
					}
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
			final QuestState qs = player.getQuestState("_140_ShadowFoxPart2");
			if (qs != null && qs.isCompleted())
			{
				st = newQuestState(player);
				st.setState(State.STARTED);
			}
		}
		else if (st.isCompleted())
		{
			QuestState qs2 = player.getQuestState("_998_FallenAngelSelect");
			final QuestState qs3 = player.getQuestState("_142_FallenAngelRequestOfDawn");
			final QuestState qs4 = player.getQuestState("_143_FallenAngelRequestOfDusk");
			if (qs2 == null)
			{
				if (qs3 == null || qs4 == null)
				{
					final Quest q = QuestManager.getInstance().getQuest("_998_FallenAngelSelect");
					if (q != null)
					{
						qs2 = q.newQuestState(player);
						qs2.setState(State.STARTED);
					}
				}
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
		else if (id == State.STARTED)
		{
			if (cond == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30894-01.htm";
				}
				else
				{
					htmltext = "30894-00.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "30894-02.htm";
			}
			else if (cond == 2)
			{
				htmltext = "30894-05.htm";
			}
			else if (cond == 3)
			{
				if (cond == talk)
				{
					htmltext = "30894-07.htm";
				}
				else
				{
					htmltext = "30894-06.htm";
					st.takeItems(10350, -1);
					st.set("talk", "1");
				}
			}
			else if (cond == 4)
			{
				htmltext = "30894-16.htm";
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
			if (st.calcDropItems(getId(), 10350, npc.getId(), 30))
			{
				st.setCond(3);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _141_ShadowFoxPart3(141, _141_ShadowFoxPart3.class.getSimpleName(), "");
	}
}