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
 * Rework by LordWinter 25.12.2019
 */
public class _10293_SevenSignsForbiddenBook extends Quest
{
	public _10293_SevenSignsForbiddenBook(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32784, 32863);
		addTalkId(32784, 32596, 32785, 32809, 32810, 32811, 32812, 32813, 32861, 32863);
		addFirstTalkId(32863);
		
		questItemIds = new int[]
		{
		        17213
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (npc.getId() == 32784)
		{
			if (event.equalsIgnoreCase("32784-04.htm"))
			{
				if (st.isCreated())
				{
					st.startQuest();
				}
			}
			else if (event.equalsIgnoreCase("32784-09.htm"))
			{
				if (player.isSubClassActive())
				{
					htmltext = "32784-10.htm";
				}
				else
				{
					if (st.isCond(8))
					{
						st.calcExpAndSp(getId());
						st.exitQuest(false, true);
						htmltext = "32784-09.htm";
					}
				}
			}
		}
		else if (npc.getId() == 32861)
		{
			if (event.equalsIgnoreCase("32861-04.htm"))
			{
				st.setCond(2, true);
			}
			if (event.equalsIgnoreCase("32861-08.htm"))
			{
				st.setCond(4, true);
			}
			if (event.equalsIgnoreCase("32861-11.htm"))
			{
				st.setCond(6, true);
			}
		}
		else if (npc.getId() == 32785)
		{
			if (event.equalsIgnoreCase("32785-07.htm"))
			{
				st.setCond(5, true);
			}
		}
		else if (npc.getId() == 32809)
		{
			if (event.equalsIgnoreCase("32809-02.htm"))
			{
				st.giveItems(17213, 1);
				st.setCond(7, true);
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
		else if (npc.getId() == 32784)
		{
			if (st.getState() == State.COMPLETED)
			{
				htmltext = "32784-02.htm";
			}
			else if (player.getLevel() < getMinLvl(getId()))
			{
				htmltext = "32784-11.htm";
			}
			else if (player.getQuestState("_10292_SevenSignsGirlofDoubt") == null || player.getQuestState("_10292_SevenSignsGirlofDoubt").getState() != State.COMPLETED)
			{
				htmltext = "32784-11.htm";
			}
			else if (st.getState() == State.CREATED)
			{
				htmltext = "32784-01.htm";
			}
			else if (st.isCond(1))
			{
				htmltext = "32784-06.htm";
			}
			else if (st.getCond() >= 8)
			{
				htmltext = "32784-07.htm";
			}
		}
		else if (npc.getId() == 32785)
		{
			switch (st.getCond())
			{
				case 1:
					htmltext = "32785-01.htm";
					break;
				case 2:
					htmltext = "32785-04.htm";
					st.setCond(3, true);
					break;
				case 3:
					htmltext = "32785-05.htm";
					break;
				case 4:
					htmltext = "32785-06.htm";
					break;
				case 5:
					htmltext = "32785-08.htm";
					break;
				case 6:
					htmltext = "32785-09.htm";
					break;
				case 7:
					htmltext = "32785-11.htm";
					st.setCond(8, true);
					break;
				case 8:
					htmltext = "32785-12.htm";
					break;
			}
		}
		else if (npc.getId() == 32596)
		{
			switch (st.getCond())
			{
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
					htmltext = "32596-01.htm";
					break;
				case 8:
					htmltext = "32596-05.htm";
					break;
			}
		}
		else if (npc.getId() == 32861)
		{
			switch (st.getCond())
			{
				case 1:
					htmltext = "32861-01.htm";
					break;
				case 2:
					htmltext = "32861-05.htm";
					break;
				case 3:
					htmltext = "32861-06.htm";
					break;
				case 4:
					htmltext = "32861-09.htm";
					break;
				case 5:
					htmltext = "32861-10.htm";
					break;
				case 6:
				case 7:
					htmltext = "32861-12.htm";
					break;
				case 8:
					htmltext = "32861-14.htm";
					break;
			}
		}
		else if (npc.getId() == 32809)
		{
			if (st.isCond(6))
			{
				htmltext = "32809-01.htm";
			}
		}
		else if (npc.getId() == 32810)
		{
			if (st.isCond(6))
			{
				htmltext = "32810-01.htm";
			}
		}
		else if (npc.getId() == 32811)
		{
			if (st.isCond(6))
			{
				htmltext = "32811-01.htm";
			}
		}
		else if (npc.getId() == 32812)
		{
			if (st.isCond(6))
			{
				htmltext = "32812-01.htm";
			}
		}
		else if (npc.getId() == 32813)
		{
			if (st.isCond(6))
			{
				htmltext = "32813-01.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onFirstTalk (Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (npc.getId() == 32863)
		{
			switch (st.getCond())
			{
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
					htmltext = "32863-01.htm";
					break;
				case 8:
					htmltext = "32863-04.htm";
					break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _10293_SevenSignsForbiddenBook(10293, _10293_SevenSignsForbiddenBook.class.getSimpleName(), "");
	}
}