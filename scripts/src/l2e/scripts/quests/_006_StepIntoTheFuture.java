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
public class _006_StepIntoTheFuture extends Quest
{
	public _006_StepIntoTheFuture(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30006);
		addTalkId(30006, 30033, 30311);
		
		questItemIds = new int[]
		{
		        7571
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
		
		if (event.equalsIgnoreCase("30006-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30033-02.htm"))
		{
			if (st.isCond(1))
			{
				st.giveItems(7571, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30311-03.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(7571, 1);
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("30006-06.htm"))
		{
			if (st.isCond(3))
			{
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		String htmltext = getNoQuestMsg(player);

		final int cond = st.getCond();
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 30006)
				{
					if (player.getRace().ordinal() == 0 && player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "30006-02.htm";
					}
					else
					{
						htmltext = "30006-01.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED:
				switch (cond)
				{
					case 1:
						if (npcId == 30006)
						{
							htmltext = "30006-04.htm";
						}
						else if (npcId == 30033)
						{
							htmltext = "30033-01.htm";
						}
						break;
					case 2:
						if (npcId == 30033)
						{
							if (st.getQuestItemsCount(7571) > 0)
							{
								htmltext = "30033-03.htm";
							}
						}
						else if (npcId == 30311)
						{
							if (st.getQuestItemsCount(7571) > 0)
							{
								htmltext = "30311-02.htm";
							}
						}
						break;
					case 3:
						if (npcId == 30006)
						{
							htmltext = "30006-05.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _006_StepIntoTheFuture(6, _006_StepIntoTheFuture.class.getSimpleName(), "");
	}
}