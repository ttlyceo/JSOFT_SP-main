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
 * Rework by LordWinter 23.05.2021
 */
public class _377_GiantsExploration2 extends Quest
{
	public _377_GiantsExploration2(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(31147);
		addTalkId(31147);

		addKillId(22661, 22662, 22663, 22664, 22665, 22666, 22667, 22668, 22669);
		
		questItemIds = new int[]
		{
		        14847
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

		if (event.equalsIgnoreCase("31147-02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31147-quit.htm"))
		{
			st.exitQuest(true, true);
		}
		else if (event.equalsIgnoreCase("rewardBook"))
		{
			if ((st.getQuestItemsCount(14842) >= 5) && (st.getQuestItemsCount(14843) >= 5) && (st.getQuestItemsCount(14844) >= 5) && (st.getQuestItemsCount(14845) >= 5) && (st.getQuestItemsCount(14846) >= 5))
			{
				st.takeItems(14842, 5);
				st.takeItems(14843, 5);
				st.takeItems(14844, 5);
				st.takeItems(14845, 5);
				st.takeItems(14846, 5);
				st.calcReward(getId(), 1, true);
				st.playSound("ItemSound.quest_finish");
				htmltext = "31147-ok.htm";
			}
			else
			{
				htmltext = "31147-no.htm";
			}
		}
		else if (event.equals("randomReward"))
		{
			if ((st.getQuestItemsCount(14842) >= 1) && (st.getQuestItemsCount(14843) >= 1) && (st.getQuestItemsCount(14844) >= 1) && (st.getQuestItemsCount(14845) >= 1) && (st.getQuestItemsCount(14846) >= 1))
			{
				st.takeItems(14842, 1);
				st.takeItems(14843, 1);
				st.takeItems(14844, 1);
				st.takeItems(14845, 1);
				st.takeItems(14846, 1);
				st.calcReward(getId(), 2, true);
				st.playSound("ItemSound.quest_finish");
				htmltext = "31147-ok.htm";
			}
			else
			{
				htmltext = "31147-no.htm";
			}
		}
		else if (isDigit(event))
		{
			if ((st.getQuestItemsCount(14842) >= 1) && (st.getQuestItemsCount(14843) >= 1) && (st.getQuestItemsCount(14844) >= 1) && (st.getQuestItemsCount(14845) >= 1) && (st.getQuestItemsCount(14846) >= 1))
			{
				final int itemId = Integer.parseInt(event);
				st.takeItems(14842, 1);
				st.takeItems(14843, 1);
				st.takeItems(14844, 1);
				st.takeItems(14845, 1);
				st.takeItems(14846, 1);
				if (itemId == 9628)
				{
					st.calcReward(getId(), 3);
				}
				else if (itemId == 9629)
				{
					st.calcReward(getId(), 4);
				}
				else if (itemId == 9630)
				{
					st.calcReward(getId(), 5);
				}
				st.playSound("ItemSound.quest_finish");
				htmltext = "31147-ok.htm";
			}
			else
			{
				htmltext = "31147-no.htm";
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

		if (st.getState() == State.STARTED)
		{
			if ((st.getQuestItemsCount(14842) > 0) && (st.getQuestItemsCount(14843) > 0) && (st.getQuestItemsCount(14844) > 0) && (st.getQuestItemsCount(14845) > 0) && (st.getQuestItemsCount(14846) > 0))
			{
				htmltext = "31147-03.htm";
			}
			else
			{
				htmltext = "31147-02a.htm";
			}
		}
		else
		{
			if (player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "31147-01.htm";
			}
			else
			{
				htmltext = "31147-00.htm";
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
		if (st != null)
		{
			st.calcDropItems(getId(), 14847, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _377_GiantsExploration2(377, _377_GiantsExploration2.class.getSimpleName(), "");
	}
}
