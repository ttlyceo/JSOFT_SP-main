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
public class _024_InhabitantsOfTheForrestOfTheDead extends Quest
{
	public _024_InhabitantsOfTheForrestOfTheDead(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31389);
		addTalkId(31389, 31531, 31532, 31522);

		addKillId(21557, 21558, 21560, 21563, 21564, 21565, 21566, 21567);
		
		questItemIds = new int[]
		{
		        7065, 7148, 7152, 7153, 7154, 7151
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		final String htmltext = event;
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31389-02.htm"))
		{
			st.giveItems(7152, 1);
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31389-11.htm"))
		{
			if (st.isCond(2))
			{
				st.giveItems(7153, 1);
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("31389-16.htm"))
		{
			st.playSound("InterfaceSound.charstat_open_01");
		}
		else if (event.equalsIgnoreCase("31389-17.htm"))
		{
			st.takeItems(7154, -1);
			st.giveItems(7148, 1);
			st.setCond(5, false);
		}
		else if (event.equalsIgnoreCase("31522-03.htm"))
		{
			st.takeItems(7151, -1);
		}
		else if (event.equalsIgnoreCase("31522-07.htm"))
		{
			if (st.isCond(10))
			{
				st.setCond(11, false);
			}
		}
		else if (event.equalsIgnoreCase("31522-19.htm"))
		{
			if (st.isCond(11))
			{
				st.giveItems(7156, 1);
				st.calcExpAndSp(getId());
				st.exitQuest(false, true);
			}
		}
		else if (event.equalsIgnoreCase("31531-02.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
				st.takeItems(7152, -1);
			}
		}
		else if (event.equalsIgnoreCase("31532-04.htm"))
		{
			st.giveItems(7065, 1);
			st.setCond(6, true);
		}
		else if (event.equalsIgnoreCase("31532-06.htm"))
		{
			st.takeItems(7148, -1);
			st.takeItems(7065, -1);
		}
		else if (event.equalsIgnoreCase("31532-16.htm"))
		{
			st.setCond(9, true);
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
		final byte state = st.getState();
		final int cond = st.getCond();

		if (state == State.COMPLETED)
		{
			if (npcId == 31522)
			{
				htmltext = "31522-20.htm";
			}
			else
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}

		if (npcId == 31389)
		{
			if (state == State.CREATED)
			{
				final QuestState st2 = player.getQuestState("_023_LidiasHeart");
				if (st2 != null)
				{
					if (st2.isCompleted() && (player.getLevel() >= getMinLvl(getId())))
					{
						htmltext = "31389-01.htm";
					}
					else
					{
						htmltext = "31389-00.htm";
					}
				}
				else
				{
					htmltext = "31389-00.htm";
				}
			}
			else if (cond == 1)
			{
				htmltext = "31389-03.htm";
			}
			else if (cond == 2)
			{
				htmltext = "31389-04.htm";
			}
			else if (cond == 3)
			{
				htmltext = "31389-12.htm";
			}
			else if (cond == 4)
			{
				htmltext = "31389-13.htm";
			}
			else if (cond == 5)
			{
				htmltext = "31389-18.htm";
			}
		}
		else if (npcId == 31531)
		{
			if (cond == 1)
			{
				st.playSound("AmdSound.d_wind_loot_02");
				htmltext = "31531-01.htm";
			}
			else if (cond == 2)
			{
				htmltext = "31531-03.htm";
			}
		}
		else if (npcId == 31532)
		{
			if (cond == 5)
			{
				htmltext = "31532-01.htm";
			}
			else if (cond == 6)
			{
				if ((st.getQuestItemsCount(7065) >= 1) && (st.getQuestItemsCount(7148) >= 1))
				{
					htmltext = "31532-05.htm";
				}
				else
				{
					htmltext = "31532-07.htm";
				}
			}
			else if (cond == 9)
			{
				htmltext = "31532-16.htm";
			}
		}
		else if (npcId == 31522)
		{
			if (cond == 10)
			{
				htmltext = "31522-01.htm";
			}
			else if (cond == 11)
			{
				htmltext = "31522-08.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st != null && (st.getQuestItemsCount(7151) == 0) && st.getCond() == 9 && st.calcDropItems(getId(), 7151, npc.getId(), 1))
		{
			st.setCond(10);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _024_InhabitantsOfTheForrestOfTheDead(24, _024_InhabitantsOfTheForrestOfTheDead.class.getSimpleName(), "");
	}
}
