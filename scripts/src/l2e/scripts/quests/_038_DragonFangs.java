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
public class _038_DragonFangs extends Quest
{
	public _038_DragonFangs(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30386);
		addTalkId(30386, 30034, 30344);

		addKillId(20356, 21101, 21100, 20357);

		questItemIds = new int[]
		{
		        7174, 7176, 7177, 7175, 7173
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		final int cond = st.getCond();

		if (event.equalsIgnoreCase("30386-02.htm") && npc.getId() == 30386)
		{
			if(cond == 0)
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("30386-04.htm") && npc.getId() == 30386)
		{
			if (st.isCond(2))
			{
				st.takeItems(7173, 100);
				st.giveItems(7174, 1);
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("30034-02a.htm") && npc.getId() == 30034)
		{
			if (st.isCond(3))
			{
				st.takeItems(7174, 1);
				st.giveItems(7176, 1);
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("30344-02a.htm") && npc.getId() == 30344)
		{
			if (st.isCond(4))
			{
				st.takeItems(7176, 1);
				st.giveItems(7177, 1);
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("30034-04a.htm") && npc.getId() == 30034)
		{
			if (st.isCond(5))
			{
				st.takeItems(7177, 1);
				st.setCond(6, true);
			}
		}
		else if (event.equalsIgnoreCase("30034-06a.htm") && npc.getId() == 30034)
		{
			if (st.isCond(7) & st.getQuestItemsCount(7175) == 50)
			{
				htmltext = "30034-06.htm";
				st.takeItems(7175, 50);
				st.calcExpAndSp(getId());
				st.calcReward(getId(), (getRandom(3) + 1));
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
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}

		if (npcId == 30386 && cond == 0)
		{
			if (player.getLevel() < getMinLvl(getId()))
			{
				htmltext = "30386-01a.htm";
				st.exitQuest(true);
			}
			else if (player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "30386-01.htm";
			}
		}

		if (npcId == 30386 && cond == 1)
		{
			htmltext = "30386-02a.htm";
		}

		if (npcId == 30386 && cond == 2 && st.getQuestItemsCount(7173) == 100)
		{
			htmltext = "30386-03.htm";
		}

		if (npcId == 30386 && cond == 3)
		{
			htmltext = "30386-03a.htm";
		}

		if (npcId == 30034 && cond == 3 && st.getQuestItemsCount(7174) == 1)
		{
			htmltext = "30034-01.htm";
		}

		if (npcId == 30034 && cond == 4)
		{
			htmltext = "30034-02b.htm";
		}

		if (npcId == 30034 && cond == 5 && st.getQuestItemsCount(7177) == 1)
		{
			htmltext = "30034-03.htm";
		}

		if (npcId == 30034 && cond == 6)
		{
			htmltext = "30034-05a.htm";
		}

		if (npcId == 30034 && cond == 7 && st.getQuestItemsCount(7175) == 50)
		{
			htmltext = "30034-05.htm";
		}

		if (npcId == 30344 && cond == 4 && st.getQuestItemsCount(7176) == 1)
		{
			htmltext = "30344-01.htm";
		}

		if (npcId == 30344 && cond == 5)
		{
			htmltext = "30344-03.htm";
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		
		if (npc.getId() == 20357 || npc.getId() == 21100)
		{
			if (st.isCond(1) && st.calcDropItems(getId(), 7173, npc.getId(), 100))
			{
				st.setCond(2);
			}
		}

		if (npc.getId() == 20356 || npc.getId() == 21101)
		{
			if (st.isCond(6) && st.calcDropItems(getId(), 7175, npc.getId(), 50))
			{
				st.setCond(7);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	public static void main(String[] args)
	{
		new _038_DragonFangs(38, _038_DragonFangs.class.getSimpleName(), "");
	}
}