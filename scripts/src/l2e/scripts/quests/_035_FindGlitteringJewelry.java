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
 * Rework by LordWinter 06.12.2019
 */
public class _035_FindGlitteringJewelry extends Quest
{
	public _035_FindGlitteringJewelry(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30091);
		addTalkId(30091, 30879);
		
		addKillId(20135);
		
		questItemIds = new int[]
		{
		        7162
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

		final int cond = st.getCond();
		
		if (event.equalsIgnoreCase("30091-1.htm") && (cond == 0))
		{
			st.startQuest();
		}
		if (event.equalsIgnoreCase("30879-1.htm") && (cond == 1))
		{
			st.setCond(2, true);
		}
		if (event.equalsIgnoreCase("30091-3.htm") && (cond == 3))
		{
			st.takeItems(7162, 10);
			st.setCond(4, true);
		}
		if (event.equalsIgnoreCase("30091-5.htm") && (cond == 4))
		{
			if ((st.getQuestItemsCount(1893) >= 5) && (st.getQuestItemsCount(1873) >= 500) && (st.getQuestItemsCount(4044) >= 150))
			{
				st.takeItems(1893, 5);
				st.takeItems(1873, 500);
				st.takeItems(4044, 150);
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
			else
			{
				return "no_items.htm";
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

		final int cond = st.getCond();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if ((npc.getId() == 30091) && (cond == 0) && (st.getQuestItemsCount(7077) == 0))
		{
			final QuestState fwear = player.getQuestState(_037_PleaseMakeMeFormalWear.class.getSimpleName());
			if (fwear != null)
			{
				if (fwear.getInt("cond") == 6)
				{
					htmltext = "30091-0.htm";
				}
				else
				{
					htmltext = "30091-6.htm";
					st.exitQuest(true);
				}
			}
			else
			{
				htmltext = "30091-6.htm";
				st.exitQuest(true);
			}
			st.exitQuest(true);
		}
		else if ((npc.getId() == 30879) && (cond == 1))
		{
			htmltext = "30879-0.htm";
		}
		else if ((npc.getId() == 30879) && (cond == 2))
		{
			htmltext = "30879-1a.htm";
		}
		else if ((npc.getId() == 30879) && (cond == 3))
		{
			htmltext = "30879-1a.htm";
		}
		else if (st.getState() == State.STARTED && (cond == 3))
		{
			if ((npc.getId() == 30091) && (st.getQuestItemsCount(7162) == 10))
			{
				htmltext = "30091-2.htm";
			}
			else
			{
				htmltext = "30091-1a.htm";
			}
		}
		else if ((npc.getId() == 30091) && (cond == 4) && (st.getQuestItemsCount(1893) >= 5) && (st.getQuestItemsCount(1873) >= 500) && (st.getQuestItemsCount(4044) >= 150))
		{
			htmltext = "30091-4.htm";
		}
		else
		{
			htmltext = "30091-3a.htm";
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 2);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null && st.calcDropItems(getId(), 7162, npc.getId(), 10))
		{
			st.setCond(3);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _035_FindGlitteringJewelry(35, _035_FindGlitteringJewelry.class.getSimpleName(), "");
	}
}
