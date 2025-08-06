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
 * Rework by LordWinter 15.12.2019
 */
public class _10268_ToTheSeedOfInfinity extends Quest
{
	public _10268_ToTheSeedOfInfinity(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(32548);
		addTalkId(32548, 32603);
		
		questItemIds = new int[]
		{
		        13811
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

		if (event.equalsIgnoreCase("32548-05.htm"))
		{
			if (st.isCreated())
			{
				st.giveItems(13811, 1);
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

		if (st.isCompleted())
		{
			if (npc.getId() == 32603)
			{
				htmltext = "32530-02.htm";
			}
			else
			{
				htmltext = "32548-0a.htm";
			}
		}
		else if (st.getState() == State.CREATED && npc.getId() == 32548)
		{
			if (player.getLevel() < getMinLvl(getId()))
			{
				htmltext = "32548-00.htm";
			}
			else
			{
				htmltext = "32548-01.htm";
			}
		}
		else if (st.getState() == State.STARTED && npc.getId() == 32548)
		{
			htmltext = "32548-06.htm";
		}
		else if (st.getState() == State.STARTED && npc.getId() == 32603)
		{
			htmltext = "32530-01.htm";
			st.calcExpAndSp(getId());
			st.calcReward(getId());
			st.exitQuest(false, true);
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _10268_ToTheSeedOfInfinity(10268, _10268_ToTheSeedOfInfinity.class.getSimpleName(), "");
	}
}