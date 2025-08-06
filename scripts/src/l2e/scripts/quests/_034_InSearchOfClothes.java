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

/**
 * Rework by LordWinter 06.12.2019
 */
public class _034_InSearchOfClothes extends Quest
{
	public _034_InSearchOfClothes(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30088);
		addTalkId(30088, 30165, 30294);
		
		addKillId(20560);
		
		questItemIds = new int[]
		{
		        7528, 1493
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int cond = st.getCond();
		
		if (event.equalsIgnoreCase("30088-1.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30294-1.htm") && cond == 1)
		{
			st.setCond(2, true);
		}
		else if (event.equalsIgnoreCase("30088-3.htm") && cond == 2)
		{
			st.setCond(3, true);
		}
		else if (event.equalsIgnoreCase("30165-1.htm") && cond == 3)
		{
			st.setCond(4, true);
		}
		else if (event.equalsIgnoreCase("30165-3.htm") && cond == 5)
		{
			if (st.getQuestItemsCount(7528) == 10)
			{
				st.takeItems(7528, 10);
				st.giveItems(1493, 1);
				st.setCond(6, true);
			}
			else
			{
				htmltext = "30165-1a.htm";
			}
		}
		else if (event.equalsIgnoreCase("30088-5.htm") && cond == 6)
		{
			if (st.getQuestItemsCount(1866) >= 3000 && st.getQuestItemsCount(1868) >= 5000 && st.getQuestItemsCount(1493) == 1)
			{
				st.takeItems(1866, 3000);
				st.takeItems(1868, 5000);
				st.takeItems(1493, 1);
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
			else
			{
				htmltext = "30088-4a.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}

		if (npcId == 30088)
		{
			if (cond == 0 && st.getQuestItemsCount(7076) == 0)
			{
				if (player.getLevel() >= getMinLvl(getId()))
				{
					final QuestState fwear = player.getQuestState("_037_PleaseMakeMeFormalWear");
					if (fwear != null && fwear.getCond() == 6)
					{
						htmltext = "30088-0.htm";
					}
					else
					{
						htmltext = "30088-6.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30088-6.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "30088-1a.htm";
			}
			else if (cond == 2)
			{
				htmltext = "30088-2.htm";
			}
			else if (cond == 3)
			{
				htmltext = "30088-3a.htm";
			}
			else if (cond == 4)
			{
				htmltext = "30088-3a.htm";
			}
			else if (cond == 5)
			{
				htmltext = "30088-3a.htm";
			}
			else if (cond == 6)
			{
				htmltext = "30088-4.htm";
			}
		}
		else if (npcId == 30294)
		{
			if (cond == 1)
			{
				htmltext = "30294-0.htm";
			}
			else if (cond == 2)
			{
				htmltext = "30294-1a.htm";
			}
		}
		else if (npcId == 30165)
		{
			if (cond == 3)
			{
				htmltext = "30165-0.htm";
			}
			else if (cond == 4 && st.getQuestItemsCount(7528) < 10)
			{
				htmltext = "30165-1a.htm";
			}
			else if (cond == 5)
			{
				htmltext = "30165-2.htm";
			}
			else if (cond == 6 && (st.getQuestItemsCount(1866) < 3000 || st.getQuestItemsCount(1868) < 5000 || st.getQuestItemsCount(1493) < 1))
			{
				htmltext = "30165-3a.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 4);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final QuestState st = partyMember.getQuestState(getName());
		if (st.calcDropItems(getId(), 7528, npc.getId(), 10))
		{
			st.setCond(5);
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _034_InSearchOfClothes(34, _034_InSearchOfClothes.class.getSimpleName(), "");
	}
}