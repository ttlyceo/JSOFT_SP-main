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
 * Rework by LordWinter 22.12.2019
 */
public class _10282_ToTheSeedOfAnnihilation extends Quest
{
	public _10282_ToTheSeedOfAnnihilation(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(32733);
		addTalkId(32733, 32734);
		
		questItemIds = new int[]
		{
		        15512
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

		if (event.equalsIgnoreCase("32733-07.htm") && npc.getId() == 32733)
		{
			if (st.isCreated())
			{
				st.giveItems(15512, 1);
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32734-02.htm") && npc.getId() == 32734)
		{
			if (st.isCond(1))
			{
				st.takeItems(15512, -1);
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

		if (st.isCompleted())
		{
			if (npc.getId() == 32733)
			{
				htmltext = "32733-09.htm";
			}
			else if (npc.getId() == 32734)
			{
				htmltext = "32734-03.htm";
			}
		}
		else if (st.getState() == State.CREATED)
		{
			if (player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "32733-01.htm";
			}
			else
			{
				htmltext = "32733-00.htm";
			}
		}
		else
		{
			if (st.isCond(1))
			{
				if (npc.getId() == 32733)
				{
					htmltext = "32733-08.htm";
				}
				else if (npc.getId() == 32734)
				{
					htmltext = "32734-01.htm";
				}
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _10282_ToTheSeedOfAnnihilation(10282, _10282_ToTheSeedOfAnnihilation.class.getSimpleName(), "");
	}
}