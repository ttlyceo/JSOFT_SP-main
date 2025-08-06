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
 * Rework by LordWinter 15.12.2019
 */
public class _10267_JourneyToGracia extends Quest
{
	public _10267_JourneyToGracia(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30857);
		addTalkId(30857, 32548, 32564);
		
		questItemIds = new int[]
		{
		        13810
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30857-06.htm":
				if (st.isCreated() && npc.getId() == 30857)
				{
					st.giveItems(13810, 1);
					st.startQuest();
				}
				break;
			case "32564-02.htm":
				if (st.isCond(1) && npc.getId() == 32564)
				{
					st.setCond(2, true);
				}
				break;
			case "32548-02.htm":
				if (st.isCond(2) && npc.getId() == 32548)
				{
					st.calcExpAndSp(getId());
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
				break;
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
		switch (st.getState())
		{
			case State.COMPLETED:
				if (npcId == 32548)
				{
					htmltext = "32548-03.htm";
				}
				else if (npcId == 30857)
				{
					htmltext = "30857-0a.htm";
				}
				break;
			case State.CREATED:
				if (npcId == 30857)
				{
					htmltext = (player.getLevel() < getMinLvl(getId())) ? "30857-00.htm" : "30857-01.htm";
				}
				break;
			case State.STARTED:
				final int cond = st.getInt("cond");
				if (npcId == 30857)
				{
					htmltext = "30857-07.htm";
				}
				else if (npcId == 32564)
				{
					htmltext = (cond == 1) ? "32564-01.htm" : "32564-03.htm";
				}
				else if ((npcId == 32548) && (cond == 2))
				{
					htmltext = "32548-01.htm";
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _10267_JourneyToGracia(10267, _10267_JourneyToGracia.class.getSimpleName(), "");
	}
}