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
public class _122_OminousNews extends Quest
{
	public _122_OminousNews(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31979);
		addTalkId(31979, 32017);
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
		
		if (event.equalsIgnoreCase("31979-03.htm") && npc.getId() == 31979)
		{
			if (st.isCreated() && (player.getLevel() >= getMinLvl(getId()) && player.getLevel() <= getMaxLvl(getId())))
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32017-02.htm") && npc.getId() == 32017)
		{
			if (st.isCond(1))
			{
				st.calcExpAndSp(getId());
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
		if(st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()) && player.getLevel() <= getMaxLvl(getId()))
				{
					htmltext = "31979-02.htm";
				}
				else
				{
					htmltext = "31979-01.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 31979 :
						htmltext = "31979-03.htm";
						break;
					case 32017 :
						htmltext = "32017-01.htm";
						break;
				}
				break;
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _122_OminousNews(122, _122_OminousNews.class.getSimpleName(), "");
	}
}