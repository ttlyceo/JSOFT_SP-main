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
public class _004_LongLiveThePaagrioLord extends Quest
{
	public _004_LongLiveThePaagrioLord(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30578);
		addTalkId(30578, 30585, 30566, 30562, 30560, 30559, 30587);
		
		questItemIds = new int[]
		{
		        1541, 1542, 1543, 1544, 1545, 1546
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

		if (event.equalsIgnoreCase("30578-03.htm") && npc.getId() == 30578)
		{
			st.startQuest();
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

		switch (npc.getId())
		{
			case 30578 :
				switch (st.getState())
				{
					case State.COMPLETED :
						htmltext = getAlreadyCompletedMsg(player);
						break;
					case State.CREATED :
						if (player.getRace().ordinal() != 3)
						{
							htmltext = "30578-00.htm";
							st.exitQuest(true);
						}
						else if (player.getLevel() >= getMinLvl(getId()))
						{
							htmltext = "30578-02.htm";
						}
						else
						{
							htmltext = "30578-01.htm";
							st.exitQuest(true);
						}
						break;
					case State.STARTED :
						switch (cond)
						{
							case 1 :
								htmltext = "30578-04.htm";
								break;
							case 2 :
								htmltext = "30578-06.htm";
								st.takeItems(1541, 1);
								st.takeItems(1542, 1);
								st.takeItems(1543, 1);
								st.takeItems(1544, 1);
								st.takeItems(1545, 1);
								st.takeItems(1546, 1);
								st.calcExpAndSp(getId());
								st.calcReward(getId());
								st.exitQuest(false, true);
								showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								break;
						}
						break;
				}
				break;
			case 30566 :
				htmltext = giveItem(st, npc.getId(), 1541, getRegisteredItemIds());
				break;
			case 30587 :
				htmltext = giveItem(st, npc.getId(), 1546, getRegisteredItemIds());
				break;
			case 30585 :
				htmltext = giveItem(st, npc.getId(), 1542, getRegisteredItemIds());
				break;
			case 30559 :
				htmltext = giveItem(st, npc.getId(), 1545, getRegisteredItemIds());
				break;
			case 30560 :
				htmltext = giveItem(st, npc.getId(), 1544, getRegisteredItemIds());
				break;
			case 30562 :
				htmltext = giveItem(st, npc.getId(), 1543, getRegisteredItemIds());
				break;
		}
		return htmltext;
	}
	
	private static String giveItem(QuestState st, int npcId, int itemId, int... items)
	{
		if (!st.isStarted())
		{
			return getNoQuestMsg(st.getPlayer());
		}
		else if (st.hasQuestItems(itemId))
		{
			return npcId + "-02.htm";
		}
		st.giveItems(itemId, 1);
		st.playSound(QuestSound.ITEMSOUND_QUEST_ITEMGET);
		if (st.hasQuestItems(items))
		{
			st.setCond(2, true);
		}
		return npcId + "-01.htm";
	}

	public static void main(String[] args)
	{
		new _004_LongLiveThePaagrioLord(4, _004_LongLiveThePaagrioLord.class.getSimpleName(), "");
	}
}
