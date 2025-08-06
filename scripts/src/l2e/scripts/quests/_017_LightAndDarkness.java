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
public class _017_LightAndDarkness extends Quest
{
	public _017_LightAndDarkness(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31517);
		addTalkId(31517, 31508, 31509, 31510, 31511);
		
		questItemIds = new int[]
		{
		        7168
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
		
		if (event.equalsIgnoreCase("31517-02.htm"))
		{
			st.giveItems(7168, 4);
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31508-02.htm"))
		{
			if (st.isCond(1))
			{
				st.takeItems(7168, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31509-02.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(7168, 1);
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("31510-02.htm"))
		{
			if (st.isCond(3))
			{
				st.takeItems(7168, 1);
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("31511-02.htm"))
		{
			if (st.isCond(4))
			{
				st.takeItems(7168, 1);
				st.setCond(5, true);
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
		
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				final QuestState st2 = player.getQuestState("_015_SweetWhisper");
				if (st2 != null && st2.getState() == State.COMPLETED)
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "31517-00.htm";
					}
					else
					{
						htmltext = "31517-02a.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "31517-02b.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 31517 :
						if (cond > 0 && cond < 5)
						{
							if (st.getQuestItemsCount(7168) > 0)
							{
								htmltext = "31517-04.htm";
							}
							else
							{
								htmltext = "31517-05.htm";
							}
						}
						else if (cond == 5 && st.getQuestItemsCount(7168) == 0)
						{
							htmltext = "31517-03.htm";
							st.calcExpAndSp(getId());
							st.exitQuest(false, true);
						}
						break;
					case 31508 :
						switch (cond)
						{
							case 1:
								if (st.getQuestItemsCount(7168) != 0)
								{
									htmltext = "31508-00.htm";
								}
								else
								{
									htmltext = "31508-02.htm";
								}
								break;
							case 2:
								htmltext = "31508-03.htm";
								break;
						}
						break;
					case 31509 :
						switch (cond)
						{
							case 2:
								if (st.getQuestItemsCount(7168) != 0)
								{
									htmltext = "31509-00.htm";
								}
								else
								{
									htmltext = "31509-02.htm";
								}
								break;
							case 3:
								htmltext = "31509-03.htm";
								break;
						}
						break;
					case 31510 :
						switch (cond)
						{
							case 3:
								if (st.getQuestItemsCount(7168) != 0)
								{
									htmltext = "31510-00.htm";
								}
								else
								{
									htmltext = "31510-02.htm";
								}
								break;
							case 4:
								htmltext = "31510-03.htm";
								break;
						}
						break;
					case 31511 :
						switch (cond)
						{
							case 4:
								if (st.getQuestItemsCount(7168) != 0)
								{
									htmltext = "31511-00.htm";
								}
								else
								{
									htmltext = "31511-02.htm";
								}
								break;
							case 5:
								htmltext = "31511-03.htm";
								break;
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _017_LightAndDarkness(17, _017_LightAndDarkness.class.getSimpleName(), "");
	}
}