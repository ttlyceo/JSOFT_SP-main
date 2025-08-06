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
public class _039_RedEyedInvaders extends Quest
{
	public _039_RedEyedInvaders(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30334);
		addTalkId(30334, 30332);

		addKillId(20919, 20920, 20921, 20925);

		questItemIds = new int[]
		{
		        7178, 7180, 7179, 7181
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

		if (event.equalsIgnoreCase("30334-1.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30332-1.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30332-3a.htm"))
		{
			if (st.isCond(3) && (st.getQuestItemsCount(7178) >= 100) && (st.getQuestItemsCount(7178) >= 100))
			{
				st.takeItems(7178, -1);
				st.takeItems(7179, -1);
				st.setCond(4, true);
			}
			else
			{
				htmltext = "no_items.htm";
			}
		}
		else if (event.equalsIgnoreCase("30332-5.htm"))
		{
			if (st.isCond(5) && (st.getQuestItemsCount(7180) >= 30) && (st.getQuestItemsCount(7181) >= 30))
			{
				st.takeItems(7180, -1);
				st.takeItems(7181, -1);
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
			else
			{
				htmltext = "no_items.htm";
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

		if (npcId == 30334)
		{
			if (cond == 0)
			{
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "30334-2.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30334-0.htm";
				}
			}
			else if (cond == 1)
			{
				htmltext = "30334-3.htm";
			}
		}
		else if (npcId == 30332)
		{
			if (cond == 1)
			{
				htmltext = "30332-0.htm";
			}
			else if ((cond == 2) && ((st.getQuestItemsCount(7178) < 100) || (st.getQuestItemsCount(7179) < 100)))
			{
				htmltext = "30332-2.htm";
			}
			else if ((cond == 3) && (st.getQuestItemsCount(7178) >= 100) && (st.getQuestItemsCount(7179) >= 100))
			{
				htmltext = "30332-3.htm";
			}
			else if ((cond == 4) && ((st.getQuestItemsCount(7180) < 30) || (st.getQuestItemsCount(7181) < 30)))
			{
				htmltext = "30332-3b.htm";
			}
			else if ((cond == 5) && (st.getQuestItemsCount(7180) >= 30) && (st.getQuestItemsCount(7181) >= 30))
			{
				htmltext = "30332-4.htm";
			}
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

		if (st.isCond(2))
		{
			if ((npc.getId() == 20919) || (npc.getId() == 20920))
			{
				st.calcDoDropItems(getId(), 7178, npc.getId(), 100);
			}
			else if (npc.getId() == 20921)
			{
				st.calcDoDropItems(getId(), 7179, npc.getId(), 100);
			}
			
			if (st.getQuestItemsCount(7178) >= 100 && st.getQuestItemsCount(7179) >= 100)
			{
				st.setCond(3, true);
			}
		}

		if (st.isCond(4))
		{
			if ((npc.getId() == 20920) || (npc.getId() == 20921))
			{
				st.calcDoDropItems(getId(), 7180, npc.getId(), 30);
			}
			else if (npc.getId() == 20925)
			{
				st.calcDoDropItems(getId(), 7181, npc.getId(), 30);
			}
			
			if (st.getQuestItemsCount(7180) >= 30 && st.getQuestItemsCount(7181) >= 30)
			{
				st.setCond(5, true);
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _039_RedEyedInvaders(39, _039_RedEyedInvaders.class.getSimpleName(), "");
	}
}
