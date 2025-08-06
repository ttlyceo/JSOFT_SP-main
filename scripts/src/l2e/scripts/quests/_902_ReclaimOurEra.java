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
 * Rework by LordWinter 02.05.2020
 */
public class _902_ReclaimOurEra extends Quest
{
	public _902_ReclaimOurEra(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31340);
		addTalkId(31340);
		
		addKillId(25309, 25312, 25315, 25299, 25302, 25305, 25667, 25668, 25669, 25670, 25701);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31340-04.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31340-06.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31340-08.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("31340-10.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(4, true);
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

		switch (st.getState())
		{
			case State.COMPLETED :
				if (!st.isNowAvailable())
				{
					htmltext = "31340-completed.htm";
					break;
				}
				st.setState(State.CREATED);
			case State.CREATED :
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "31340-01.htm" : "31340-00.htm";
				break;
			case State.STARTED :
				switch (st.getCond())
				{
					case 1 :
						htmltext = "31340-05.htm";
						break;
					case 2 :
						htmltext = "31340-07.htm";
						break;
					case 3 :
						htmltext = "31340-09.htm";
						break;
					case 4 :
						htmltext = "31340-11.htm";
						break;
					case 5 :
						if (st.getQuestItemsCount(21997) > 0)
						{
							st.takeItems(21997, 1);
							st.calcReward(getId(), 1);
						}
						else if (st.getQuestItemsCount(21998) > 0)
						{
							st.takeItems(21998, 1);
							st.calcReward(getId(), 2);
						}
						else if (st.getQuestItemsCount(21999) > 0)
						{
							st.takeItems(21999, 1);
							st.calcReward(getId(), 3);
						}
						st.exitQuest(QuestType.DAILY, true);
						htmltext = "31340-12.htm";
						break;
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (killer.isInParty())
		{
			for (final Player player : killer.getParty().getMembers())
			{
				rewardPlayer(npc, player);
			}
		}
		else
		{
			rewardPlayer(npc, killer);
		}
		return super.onKill(npc, killer, isSummon);
	}

	private void rewardPlayer(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st != null && st.isStarted() && player.isInsideRadius(npc, 1500, false, false))
		{
			if (st.isCond(2) && ArrayUtils.contains(new int[]
			{
			        25309, 25312, 25315, 25299, 25302, 25305
			}, npc.getId()))
			{
				if (st.calcDropItems(getId(), 21997, npc.getId(), 1))
				{
					st.setCond(5);
				}
			}
			else if (st.isCond(3) && ArrayUtils.contains(new int[]
			{
			        25667, 25668, 25669, 25670
			}, npc.getId()))
			{
				if (st.calcDropItems(getId(), 21998, npc.getId(), 1))
				{
					st.setCond(5);
				}
			}
			else if (st.isCond(4) && npc.getId() == 25701)
			{
				if (st.calcDropItems(getId(), 21999, npc.getId(), 1))
				{
					st.setCond(5);
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new _902_ReclaimOurEra(902, _902_ReclaimOurEra.class.getSimpleName(), "");
	}
}