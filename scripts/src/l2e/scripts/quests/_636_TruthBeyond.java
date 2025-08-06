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
 * Rework by LordWinter 03.03.2020
 */
public final class _636_TruthBeyond extends Quest
{
	public _636_TruthBeyond(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31329);
		addTalkId(31329);
		addTalkId(32010);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if ("31329-04.htm".equalsIgnoreCase(event))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if ("32010-02.htm".equalsIgnoreCase(event))
		{
			if (st.isCond(1))
			{
				st.calcReward(getId());
				st.exitQuest(true, true);
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
		
		final int npcId = npc.getId();
		switch (st.getState())
		{
			case State.CREATED:
				if (npcId == 31329)
				{
					if ((st.getQuestItemsCount(8064) == 0) && (st.getQuestItemsCount(8067) == 0))
					{
						if (player.getLevel() >= getMinLvl(getId()))
						{
							htmltext = "31329-02.htm";
						}
						else
						{
							st.exitQuest(true);
							htmltext = "31329-01.htm";
						}
					}
					else
					{
						htmltext = "31329-mark.htm";
					}
				}
				else if (npcId == 32010)
				{
					if (st.getQuestItemsCount(8064) == 1)
					{
						htmltext = "32010-03.htm";
					}
				}
				break;
			case State.STARTED:
				if (npcId == 31329)
				{
					htmltext = "31329-05.htm";
				}
				else if (npcId == 32010)
				{
					if (st.isCond(1))
					{
						htmltext = "32010-01.htm";
					}
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _636_TruthBeyond(636, _636_TruthBeyond.class.getSimpleName(), "");
	}
}