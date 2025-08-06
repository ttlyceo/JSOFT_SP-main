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
public class _10269_ToTheSeedOfDestruction extends Quest
{
	public _10269_ToTheSeedOfDestruction(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32548);
		addTalkId(32548, 32526);
		
		questItemIds = new int[]
		{
		        13812
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
				st.giveItems(13812, 1);
				st.startQuest();
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
		
		final int npcId = npc.getId();
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = (npcId == 32526) ? "32526-02.htm" : "32548-0a.htm";
				break;
			case State.CREATED:
				if (npcId == 32548)
				{
					htmltext = player.getLevel() < getMinLvl(getId()) ? "32548-00.htm" : "32548-01.htm";
				}
				break;
			case State.STARTED:
				if (npcId == 32548)
				{
					htmltext = "32548-06.htm";
				}
				else if (npcId == 32526)
				{
					htmltext = "32526-01.htm";
					st.calcExpAndSp(getId());
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _10269_ToTheSeedOfDestruction(10269, _10269_ToTheSeedOfDestruction.class.getSimpleName(), "");
	}
}