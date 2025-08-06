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
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 05.01.2022
 */
public class _237_WindsOfChange extends Quest
{
	public _237_WindsOfChange(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(30899);
		addTalkId(30899, 30969, 30897, 30925, 32641, 32643);
		
		questItemIds = new int[]
		{
		        14862, 14863, 14864
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final var st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("30899-06.htm") && npc.getId() == 30899)
		{
			if (st.isCreated() && player.getLevel() >= getMinLvl(getId()))
			{
				st.giveItems(14862, 1);
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("30969-05.htm") && npc.getId() == 30969)
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30897-03.htm") && npc.getId() == 30897)
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("30925-03.htm") && npc.getId() == 30925)
		{
			if (st.isCond(3))
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("30969-09.htm") && npc.getId() == 30969)
		{
			if (st.isCond(4))
			{
				st.giveItems(14863, 1);
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("30969-10.htm") && npc.getId() == 30969)
		{
			if (st.isCond(4))
			{
				st.giveItems(14864, 1);
				st.setCond(6, true);
			}
		}
		else if (event.equalsIgnoreCase("32641-02.htm") && npc.getId() == 32641)
		{
			if (st.hasQuestItems(14863) && st.isCond(5))
			{
				st.takeItems(14863, -1);
				st.calcExpAndSp(getId());
				st.calcReward(getId(), 1);
				st.exitQuest(false, true);
			}
		}
		else if (event.equalsIgnoreCase("32643-02.htm") && npc.getId() == 32643)
		{
			if (st.hasQuestItems(14864) && st.isCond(6))
			{
				st.takeItems(14864, -1);
				st.calcExpAndSp(getId());
				st.calcReward(getId(), 2);
				st.exitQuest(false, true);
			}
		}
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		var htmltext = getNoQuestMsg(player);
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.COMPLETED :
				switch (npc.getId())
				{
					case 30899 :
						htmltext = "30899-09.htm";
						break;
					case 32641 :
						htmltext = "32641-03.htm";
						break;
					case 32643 :
						htmltext = "32643-03.htm";
						break;
				}
				break;
			case State.CREATED :
				htmltext = player.getLevel() < getMinLvl(getId()) ? "30899-00.htm" : "30899-01.htm";
				break;
			case State.STARTED :
				switch (npc.getId())
				{
					case 30899 :
						htmltext = cond < 5 ? "30899-07.htm" : "30899-08.htm";
						break;
					case 30969 :
						if (cond == 1)
						{
							st.takeItems(14862, -1);
							htmltext = "30969-01.htm";
						}
						else if (cond > 1 && cond < 4)
						{
							htmltext = "30969-06.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30969-07.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30969-11.htm";
						}
						break;
					case 30897 :
						if (cond == 2)
						{
							htmltext = "30897-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "30897-04.htm";
						}
						break;
					case 30925 :
						if (cond == 3)
						{
							htmltext = "30925-01.htm";
						}
						else if (cond > 3)
						{
							htmltext = "30925-04.htm";
						}
						break;
					case 32641 :
						if (cond == 5)
						{
							htmltext = "32641-01.htm";
						}
						break;
					case 32643 :
						if (cond == 6)
						{
							htmltext = "32643-01.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _237_WindsOfChange(237, _237_WindsOfChange.class.getSimpleName(), "");
	}
}