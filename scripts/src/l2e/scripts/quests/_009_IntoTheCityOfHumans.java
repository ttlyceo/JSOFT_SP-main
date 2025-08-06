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
 * Rework by LordWinter 05.12.2019
 */
public class _009_IntoTheCityOfHumans extends Quest
{
	public _009_IntoTheCityOfHumans(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30583);
		addTalkId(30583, 30571, 30576);
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
		
		if (event.equalsIgnoreCase("30583-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30571-02.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30576-02.htm"))
		{
			if (st.isCond(2))
			{
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		String htmltext = getNoQuestMsg(player);
		
		final int cond = st.getCond();
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 30583)
				{
					if (player.getRace().ordinal() == 3 && player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "30583-02.htm";
					}
					else
		            {
		                htmltext = "30583-01.htm";
						st.exitQuest(true);
		            }
				}
				break;
			case State.STARTED:
				switch (cond)
				{
					case 1:
						if (npcId == 30583)
						{
							htmltext = "30583-04.htm";
						}
						else if (npcId == 30571)
						{
							htmltext = "30571-01.htm";
						}
						break;
					case 2:
						if (npcId == 30571)
						{
							htmltext = "30571-03.htm";
						}
						else if (npcId == 30576)
						{
							htmltext = "30576-01.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _009_IntoTheCityOfHumans(9, _009_IntoTheCityOfHumans.class.getSimpleName(), "");
	}
}