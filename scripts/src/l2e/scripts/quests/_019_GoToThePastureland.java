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
public class _019_GoToThePastureland extends Quest
{
	public _019_GoToThePastureland(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31302);
		addTalkId(31302, 31537);

		questItemIds = new int[]
		{
		        15532
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

		if (event.equalsIgnoreCase("31302-1.htm") && npc.getId() == 31302)
		{
			if (st.isCreated() && (player.getLevel() >= getMinLvl(getId())))
			{
				st.giveItems(15532, 1);
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31537-1.htm") && npc.getId() == 31537)
		{
			if (st.isCond(1) && st.getQuestItemsCount(7547) >= 1)
			{
				st.takeItems(7547, 1);
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
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "31302-0.htm";
				}
				else
				{
					htmltext = "31302-0a.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 31302 :
						htmltext = "31302-2.htm";
						break;
					case 31537 :
						if (st.getQuestItemsCount(15532) >= 1)
						{
							htmltext = "31537-0.htm";
						}
						else
						{
							htmltext = "31537-1.htm";
							st.exitQuest(true);
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _019_GoToThePastureland(19, _019_GoToThePastureland.class.getSimpleName(), "");
	}
}