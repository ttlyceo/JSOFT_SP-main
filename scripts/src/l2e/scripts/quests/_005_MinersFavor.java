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
import l2e.gameserver.network.NpcStringId;

/**
 * Rework by LordWinter 05.12.2019
 */
public class _005_MinersFavor extends Quest
{
	public _005_MinersFavor(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30554);
		addTalkId(30554, 30517, 30518, 30520, 30526);
		
		questItemIds = new int[]
		{
		        1547, 1552, 1548, 1549, 1550, 1551
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

		if (event.equalsIgnoreCase("30554-03.htm") && npc.getId() == 30554)
		{
			st.giveItems(1547, 1);
			st.giveItems(1552, 1);
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30526-02.htm") && npc.getId() == 30526)
		{
			st.takeItems(1552, -1);
			st.giveItems(1549, 1);
			if ((st.getQuestItemsCount(1547) > 0) && ((st.getQuestItemsCount(1548) > 0) && (st.getQuestItemsCount(1549) > 0) && (st.getQuestItemsCount(1550) > 0) && (st.getQuestItemsCount(1551) > 0)))
			{
				st.setCond(2, true);
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
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30554-02.htm";
				}
				else
				{
					htmltext = "30554-01.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (cond)
				{
					case 1:
						if (npcId == 30554)
						{
							htmltext = "30554-04.htm";
						}
						else if (npcId == 30517)
						{
							if (st.getQuestItemsCount(1547) > 0)
							{
								if (st.getQuestItemsCount(1550) == 0)
								{
									htmltext = "30517-01.htm";
									st.giveItems(1550, 1);
									st.playSound("ItemSound.quest_itemget");
								}
								else
								{
									htmltext = "30517-02.htm";
								}
							}
						}
						else if (npcId == 30518)
						{
							if ((st.getQuestItemsCount(1547) > 0) && (st.getQuestItemsCount(1550) > 0))
							{
								if (st.getQuestItemsCount(1548) == 0)
								{
									htmltext = "30518-01.htm";
									st.giveItems(1548, 1);
									st.playSound("ItemSound.quest_itemget");
								}
								else
								{
									htmltext = "30518-02.htm";
								}
							}
						}
						else if (npcId == 30520)
						{
							if ((st.getQuestItemsCount(1547) > 0) && (st.getQuestItemsCount(1548) > 0))
							{
								if (st.getQuestItemsCount(1551) == 0)
								{
									htmltext = "30520-01.htm";
									st.giveItems(1551, 1);
									st.playSound("ItemSound.quest_itemget");
								}
								else
								{
									htmltext = "30520-02.htm";
								}
							}
						}
						else if (npcId == 30526)
						{
							if ((st.getQuestItemsCount(1547) > 0) && (st.getQuestItemsCount(1551) > 0))
							{
								if ((st.getQuestItemsCount(1549) == 0) && (st.getQuestItemsCount(1552) > 0))
								{
									htmltext = "30526-01.htm";
								}
								else
								{
									htmltext = "30526-03.htm";
								}
							}
						}
						break;
					case 2:
						if (npcId == 30554)
						{
							htmltext = "30554-06.htm";
							st.calcExpAndSp(getId());
							st.calcReward(getId());
							st.exitQuest(false, true);
							showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _005_MinersFavor(5, _005_MinersFavor.class.getSimpleName(), "");
	}
}