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
 * Rework by LordWinter 05.10.2020
 */
public class _192_SevenSignSeriesOfDoubt extends Quest
{
	public _192_SevenSignSeriesOfDoubt(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30676);
		addTalkId(30191, 30197, 30200, 32568, 30676);
		
		questItemIds = new int[]
		{
		        13813, 13814, 13815
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
		
		if (npc.getId() == 30676)
		{
			if (event.equalsIgnoreCase("30676-03.htm"))
			{
				if (player.getLevel() >= getMinLvl(getId()) && st.isCreated())
				{
					st.startQuest();
				}
			}
			else
			{
				if (event.equals("8") && st.isCond(1))
				{
					st.setCond(2, true);
					player.showQuestMovie(8);
					startQuestTimer("playertele", 32000L, npc, player);
					return "";
				}
				else if (event.equalsIgnoreCase("playertele"))
				{
					player.teleToLocation(81654, 54851, -1513, true, player.getReflection());
					return "";
				}
				else if (event.equalsIgnoreCase("30676-12.htm") && st.isCond(6))
				{
					st.takeItems(13814, 1);
					st.giveItems(13815, 1);
					st.setCond(7, true);
				}
			}
		}
		else if (npc.getId() == 30197)
		{
			if (event.equalsIgnoreCase("30197-03.htm") && st.isCond(3))
			{
				st.takeItems(13813, 1);
				st.setCond(4, true);
			}
		}
		else if (npc.getId() == 30200)
		{
			if (event.equalsIgnoreCase("30200-04.htm") && st.isCond(4))
			{
				st.setCond(5, true);
			}
		}
		else if (npc.getId() == 32568)
		{
			if (event.equalsIgnoreCase("32568-02.htm") && st.isCond(5))
			{
				st.giveItems(13814, 1);
				st.setCond(6, true);
			}
		}
		else if ((npc.getId() == 30191) && event.equalsIgnoreCase("30191-03.htm"))
		{
			if (st.isCond(7))
			{
				st.takeItems(13815, 1);
				st.calcExpAndSp(getId());
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
			case State.CREATED :
				if (npc.getId() == 30676)
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "30676-01.htm";
					}
					else
					{
						htmltext = "30676-00.htm";
						st.exitQuest(true);
					}
				}
				else if (npc.getId() == 32568)
				{
					htmltext = "32568-04.htm";
				}
				break;
			case State.STARTED :
				if (npc.getId() == 30676)
				{
					if (st.isCond(1))
					{
						htmltext = "30676-04.htm";
					}
					else if (st.isCond(2))
					{
						htmltext = "30676-05.htm";
						st.giveItems(13813, 1);
						st.setCond(3, true);
					}
					else if ((st.getCond() >= 3) && (st.getCond() <= 5))
					{
						htmltext = "30676-06.htm";
					}
					else if (st.isCond(6))
					{
						htmltext = "30676-07.htm";
					}
				}
				else if (npc.getId() == 30197)
				{
					if (st.isCond(3))
					{
						htmltext = "30197-01.htm";
					}
					else if ((st.getCond() >= 4) && (st.getCond() <= 7))
					{
						htmltext = "30197-04.htm";
					}
				}
				else if (npc.getId() == 30200)
				{
					if (st.isCond(4))
					{
						htmltext = "30200-01.htm";
					}
					else if ((st.getCond() >= 5) && (st.getCond() <= 7))
					{
						htmltext = "30200-05.htm";
					}
				}
				else if (npc.getId() == 32568)
				{
					if ((st.getCond() >= 1) && (st.getCond() <= 4))
					{
						htmltext = "32568-03.htm";
					}
					else if (st.isCond(5))
					{
						htmltext = "32568-01.htm";
					}
				}
				else if ((npc.getId() == 30191))
				{
					if (st.isCond(7))
					{
						htmltext = "30191-01.htm";
					}
				}
				break;
			case State.COMPLETED :
				if (npc.getId() == 30676)
				{
					htmltext = "30676-13.htm";
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String args[])
	{
		new _192_SevenSignSeriesOfDoubt(192, _192_SevenSignSeriesOfDoubt.class.getSimpleName(), "");
	}
}