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
 * Rework by LordWinter 26.05.2021
 */
public final class _648_AnIceMerchantsDream extends Quest
{
	public _648_AnIceMerchantsDream(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32020, 32023);
		addTalkId(32020, 32023);
		
		for (int i = 22080; i <= 22098; i++)
		{
			if (i != 22095)
			{
				addKillId(i);
			}
		}
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
		
		if (event.equalsIgnoreCase("32020-02.htm") && npc.getId() == 32020)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32020-07.htm") && npc.getId() == 32020)
		{
			if (st.isCond(2))
			{
				final int silver = (int) st.getQuestItemsCount(8077);
				final int black = (int) st.getQuestItemsCount(8078);
				if (silver > 0)
				{
					st.takeItems(8077, silver);
					st.calcRewardPerItem(getId(), 1, silver);
				}
				
				if (black > 0)
				{
					st.takeItems(8078, black);
					st.calcRewardPerItem(getId(), 2, black);
				}
			}
		}
		else if (event.equalsIgnoreCase("32020-09.htm") && npc.getId() == 32020)
		{
			st.exitQuest(true, true);
		}
		else if (event.equalsIgnoreCase("32023-04.htm") && npc.getId() == 32023)
		{
			st.playSound("ItemSound2.broken_key");
			st.takeItems(8077, 1L);
		}
		else if (event.equalsIgnoreCase("32023-05.htm") && npc.getId() == 32023)
		{
			if (st.getRandom(100) <= 25)
			{
				st.giveItems(8078, 1L);
				st.playSound("ItemSound3.sys_enchant_sucess");
			}
			else
			{
				htmltext = "32023-06.htm";
				st.playSound("ItemSound3.sys_enchant_failed");
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
		final int silver = (int) st.getQuestItemsCount(8077);
		final int black = (int) st.getQuestItemsCount(8078);
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "32020-01.htm";
				}
				else
				{
					htmltext = "32020-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if (npc.getId() == 32020)
				{
					if (cond == 1)
					{
						if ((silver > 0) || (black > 0))
						{
							final QuestState st2 = player.getQuestState("_115_TheOtherSideOfTruth");
							htmltext = "32020-05.htm";
							if (st2 != null && st2.isCompleted())
							{
								htmltext = "32020-10.htm";
								st.setCond(2, true);
							}
						}
						else
						{
							htmltext = "32020-04.htm";
						}
					}
					else if (cond == 2)
					{
						if ((silver > 0) || (black > 0))
						{
							htmltext = "32020-10.htm";
						}
						else
						{
							htmltext = "32020-04a.htm";
						}
					}
				}
				else if (npc.getId() == 32023)
				{
					if (st.getState() == 0)
					{
						htmltext = "32023-00.htm";
					}
					else if (silver > 0)
					{
						htmltext = "32023-02.htm";
					}
					else
					{
						htmltext = "32023-01.htm";
					}
				}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember != null)
		{
			final QuestState st = partyMember.getQuestState(getName());
			if (st != null && st.getCond() >= 1)
			{
				st.calcDropItems(getId(), 8077, npc.getId(), Integer.MAX_VALUE);
				if (st.getCond() >= 2)
				{
					st.calcDropItems(getId(), 8057, npc.getId(), Integer.MAX_VALUE);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _648_AnIceMerchantsDream(648, _648_AnIceMerchantsDream.class.getSimpleName(), "");
	}
}