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
public class _010_IntoTheWorld extends Quest
{
	public _010_IntoTheWorld(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30533);
		addTalkId(30533, 30520, 30650);
		
		questItemIds = new int[]
		{
		        7574
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

		if(event.equalsIgnoreCase("30533-03.htm"))
		{
			st.startQuest();
		}
		else if(event.equalsIgnoreCase("30520-02.htm"))
		{
			if (st.isCond(1))
			{
				st.giveItems(7574, 1);
				st.setCond(2, true);
			}
		}
		else if(event.equalsIgnoreCase("30650-02.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(7574, 1);
				st.setCond(3, true);
			}
		}
		else if(event.equalsIgnoreCase("30520-05.htm"))
		{
			if (st.isCond(3))
			{
				st.setCond(4, true);
			}
		}
		else if(event.equalsIgnoreCase("30533-06.htm"))
		{
			if (st.isCond(4))
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
				if (npcId == 30533)
				{
					if (player.getRace().ordinal() == 4 && player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "30533-02.htm";
					}
					else
					{
						htmltext = "30533-01.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED:
				switch (npcId)
				{
					case 30533 :
						switch (cond)
						{
							case 1:
								htmltext = "30533-04.htm";
								break;
							case 4:
								htmltext = "30533-05.htm";
								break;
						}
						break;
					case 30520 :
						switch (cond)
						{
							case 1:
								htmltext = "30520-01.htm";
								break;
							case 2:
								htmltext = "30520-03.htm";
								break;
							case 3:
								htmltext = "30520-04.htm";
								st.set("cond", "4");
								break;
							case 4:
								htmltext = "30520-06.htm";
								break;
						}
						break;
					case 30650 :
						switch (cond)
						{
							case 2:
								if (st.getQuestItemsCount(7574) > 0)
								{
									htmltext = "30650-01.htm";
								}
								break;
							case 3:
								htmltext = "30650-03.htm";
								break;
							default:
								htmltext = "30650-04.htm";
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
		new _010_IntoTheWorld(10, _010_IntoTheWorld.class.getSimpleName(), "");
	}
}