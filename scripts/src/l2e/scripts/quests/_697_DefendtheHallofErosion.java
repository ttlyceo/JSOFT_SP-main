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

import l2e.gameserver.instancemanager.SoIManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 18.09.2020
 */
public class _697_DefendtheHallofErosion extends Quest
{
	public _697_DefendtheHallofErosion(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32603);
		addTalkId(32603);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32603-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
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
		
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "32603-00.htm";
					st.exitQuest(true);
				}
				if (SoIManager.getInstance().getCurrentStage() != 4)
				{
					htmltext = "32603-00a.htm";
					st.exitQuest(true);
				}
				htmltext = "32603-01.htm";
				break;
			case State.STARTED:
				if ((cond == 1) && (st.getInt("defenceDone") == 0))
				{
					htmltext = "32603-04.htm";
				}
				else if ((cond == 1) && (st.getInt("defenceDone") != 0))
				{
					st.calcReward(getId());
					htmltext = "32603-05.htm";
					st.unset("defenceDone");
					st.exitQuest(true, true);
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _697_DefendtheHallofErosion(697, _697_DefendtheHallofErosion.class.getSimpleName(), "");
	}
}