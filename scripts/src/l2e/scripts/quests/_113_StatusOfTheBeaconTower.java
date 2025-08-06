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
 * Rework by LordWinter 26.03.2021
 */
public class _113_StatusOfTheBeaconTower extends Quest
{
	public _113_StatusOfTheBeaconTower(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31979);
		addTalkId(31979, 32016);
		
		questItemIds = new int[]
		{
		        8086
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

		if (event.equalsIgnoreCase("31979-02.htm") && npc.getId() == 31979)
		{
			if (st.isCreated() && (player.getLevel() >= getMinLvl(getId())))
			{
				st.startQuest();
				st.giveItems(8086, 1);
			}
		}
		else if (event.equalsIgnoreCase("32016-02.htm") && npc.getId() == 32016)
		{
			if (st.isCond(1))
			{
				st.takeItems(8086, 1);
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
		if(st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 31979)
		        {
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "31979-01.htm";
					}
					else
		            {
		            	htmltext = "31979-00.htm";
		            	st.exitQuest(true);
		            }
		        }
		        break;
			case State.STARTED:
				if (npcId == 31979)
				{
					htmltext = "31979-03.htm";
				}
				else if (npcId == 32016)
		        {
					if (st.getQuestItemsCount(8086) == 1)
					{
						htmltext = "32016-01.htm";
					}
		        }
		        break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _113_StatusOfTheBeaconTower(113, _113_StatusOfTheBeaconTower.class.getSimpleName(), "");
	}
}