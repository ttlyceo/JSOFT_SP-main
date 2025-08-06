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
 * Rework by LordWinter 08.12.2019
 */
public class _048_ToTheImmortalPlateau extends Quest
{
	public _048_ToTheImmortalPlateau(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30097);
		addTalkId(30097, 30094, 30090, 30116);

		questItemIds = new int[]
		{
		        7563, 7564, 7565, 7568, 7567, 7566
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

		if (event.equalsIgnoreCase("1"))
		{
			if (st.getQuestItemsCount(7570) > 0)
			{
				st.giveItems(7563, 1);
				st.startQuest();
				htmltext = "30097-03.htm";
			}
		}
		else if (event.equalsIgnoreCase("2"))
		{
			if (st.isCond(1) && st.getQuestItemsCount(7563) > 0)
			{
				st.takeItems(7563, 1);
				st.giveItems(7568, 1);
				htmltext = "30094-02.htm";
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("3"))
		{
			if (st.isCond(2) && st.getQuestItemsCount(7568) > 0)
			{
				st.takeItems(7568, 1);
				st.giveItems(7564, 1);
				htmltext = "30097-06.htm";
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("4"))
		{
			if (st.isCond(3) && st.getQuestItemsCount(7564) > 0)
			{
				st.takeItems(7564, 1);
				st.giveItems(7567, 1);
				htmltext = "30090-02.htm";
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("5"))
		{
			if (st.isCond(4) && st.getQuestItemsCount(7567) > 0)
			{
				st.takeItems(7567, 1);
				st.giveItems(7565, 1);
				htmltext = "30097-09.htm";
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("6"))
		{
			if (st.isCond(5) && st.getQuestItemsCount(7565) > 0)
			{
				st.takeItems(7565, 1);
				st.giveItems(7566, 1);
				htmltext = "30116-02.htm";
				st.setCond(6, true);
			}
		}
		else if (event.equalsIgnoreCase("7"))
		{
			if (st.isCond(6) && st.getQuestItemsCount(7566) > 0)
			{
				st.takeItems(7566, 1);
				htmltext = "30097-12.htm";
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
		
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED :
				if (player.getRace().ordinal() == 3 && st.getQuestItemsCount(7570) > 0)
				{
					htmltext = "30097-02.htm";
				}
				else
				{
					htmltext = "30097-01.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				switch (npc.getId())
				{
					case 30097 :
						switch (cond)
						{
							case 1:
								htmltext = "30097-04.htm";
								break;
							case 2:
								htmltext = "30097-05.htm";
								break;
							case 3:
								htmltext = "30097-07.htm";
								break;
							case 4:
								htmltext = "30097-08.htm";
								break;
							case 5:
								htmltext = "30097-10.htm";
								break;
							case 6:
								htmltext = "30097-11.htm";
								break;
						}
						break;
					case 30094 :
						switch (cond)
						{
							case 1:
								htmltext = "30094-01.htm";
								break;
							case 2:
								htmltext = "30094-03.htm";
								break;
						}
						break;
					case 30090 :
						switch (cond)
						{
							case 3:
								htmltext = "30090-01.htm";
								break;
							case 4:
								htmltext = "30090-03.htm";
								break;
						}
						break;
					case 30116 :
						switch (cond)
						{
							case 5:
								htmltext = "30116-01.htm";
								break;
							case 6:
								htmltext = "30116-03.htm";
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
		new _048_ToTheImmortalPlateau(48, _048_ToTheImmortalPlateau.class.getSimpleName(), "");
	}
}