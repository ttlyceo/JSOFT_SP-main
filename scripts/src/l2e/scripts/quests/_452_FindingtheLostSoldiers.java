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

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.QuestState.QuestType;
import l2e.gameserver.model.quest.State;

/**
 * Based on L2J Eternity-World
 */
public class _452_FindingtheLostSoldiers extends Quest
{
	private static final String qn = "_452_FindingtheLostSoldiers";
	
	private static final int JAKAN = 32773;
	private static final int TAG_ID = 15513;
	
	private static final int[] SOLDIER_CORPSES =
	{
	        32769, 32770, 32771, 32772
	};

	public _452_FindingtheLostSoldiers(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(JAKAN);
		addTalkId(JAKAN);
		addTalkId(SOLDIER_CORPSES);
		
		questItemIds = new int[]
		{
		        TAG_ID
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(qn);
		if (st == null || (st.isCompleted() && !st.isNowAvailable()))
		{
			return event;
		}

		if (npc.getId() == JAKAN)
		{
			if (event.equalsIgnoreCase("32773-3.htm"))
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (ArrayUtils.contains(SOLDIER_CORPSES, npc.getId()))
		{
			if (st.getInt("cond") == 1)
			{
				st.giveItems(TAG_ID, 1);
				st.set("cond", "2");
				npc.deleteMe();
			}
			else
			{
				return "corpse-3.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		if (npc.getId() == JAKAN)
		{
			switch (st.getState())
			{
				case State.CREATED :
					htmltext = (player.getLevel() < 84) ? "32773-0.htm" : "32773-1.htm";
					break;
				case State.STARTED :
					if (st.getInt("cond") == 1)
					{
						htmltext = "32773-4.htm";
					}
					else if (st.getInt("cond") == 2)
					{
						htmltext = "32773-5.htm";
						st.takeItems(TAG_ID, 1);
						st.rewardItems(57, 95200);
						st.addExpAndSp(435024, 50366);
						st.exitQuest(QuestType.DAILY);
					}
					break;
				case State.COMPLETED :
					if (st.isNowAvailable())
					{
						st.setState(State.CREATED);
						htmltext = (player.getLevel() < 84) ? "32773-0.htm" : "32773-1.htm";
					}
					else
					{
						htmltext = "32773-6.htm";
					}
					break;
			}
		}
		else if (ArrayUtils.contains(SOLDIER_CORPSES, npc.getId()))
		{
			if (st.getInt("cond") == 1)
			{
				htmltext = "corpse-1.htm";
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _452_FindingtheLostSoldiers(452, qn, "");
	}
}