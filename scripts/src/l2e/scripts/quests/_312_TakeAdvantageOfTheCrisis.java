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
 * Rework by LordWinter 16.05.2021
 */
public class _312_TakeAdvantageOfTheCrisis extends Quest
{
	public _312_TakeAdvantageOfTheCrisis(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30535);
		addTalkId(30535);

		addKillId(22678, 22679, 22680, 22681, 22682, 22683, 22684, 22685, 22686, 22687, 22688, 22689, 22690);

		questItemIds = new int[]
		{
		        14875
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

		if (event.equalsIgnoreCase("30535-25.htm"))
		{
			st.exitQuest(true, true);
		}
		else if (event.equalsIgnoreCase("30535-6.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("30535-14.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 366)
			{
				st.takeItems(14875, 366);
				st.calcReward(getId(), 1);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-15.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 299)
			{
				st.takeItems(14875, 299);
				st.calcReward(getId(), 2);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-16.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 183)
			{
				st.takeItems(14875, 183);
				st.calcReward(getId(), 3);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-17.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 122)
			{
				st.takeItems(14875, 122);
				st.calcReward(getId(), 4);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-18.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 122)
			{
				st.takeItems(14875, 122);
				st.calcReward(getId(), 5);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-19.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 129)
			{
				st.takeItems(14875, 129);
				st.calcReward(getId(), 6);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-20.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 667)
			{
				st.takeItems(14875, 667);
				st.calcReward(getId(), 7);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-21.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 1000)
			{
				st.takeItems(14875, 1000);
				st.calcReward(getId(), 8);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-22.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 24)
			{
				st.takeItems(14875, 24);
				st.calcReward(getId(), 9);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-23.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 24)
			{
				st.takeItems(14875, 24);
				st.calcReward(getId(), 10);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
			}
		}
		else if (event.equalsIgnoreCase("30535-24.htm"))
		{
			if (st.getQuestItemsCount(14875) >= 36)
			{
				st.takeItems(14875, 36);
				st.calcReward(getId(), 11);
				st.playSound("ItemSound.quest_middle");
				htmltext = "30535-14.htm";
			}
			else
			{
				htmltext = "30535-14no.htm";
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
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30535-0.htm";
				}
				else
				{
					st.exitQuest(true);
					htmltext = "30535-0a.htm";
				}
				break;
			case State.STARTED :
				if (st.getQuestItemsCount(14875) == 0)
				{
					htmltext = "30535-6.htm";
				}
				else
				{
					htmltext = "30535-7.htm";
				}
				break;
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
			st.calcDropItems(getId(), 14875, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _312_TakeAdvantageOfTheCrisis(312, _312_TakeAdvantageOfTheCrisis.class.getSimpleName(), "");
	}
}
