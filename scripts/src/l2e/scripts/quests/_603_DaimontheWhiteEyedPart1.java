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
 * Rework by LordWinter 13.06.2020
 */
public class _603_DaimontheWhiteEyedPart1 extends Quest
{
	public _603_DaimontheWhiteEyedPart1(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31683);
		addTalkId(31683, 31548, 31549, 31550, 31551, 31552);
		
		addKillId(21297, 21299, 21304);
		
		questItemIds = new int[]
		{
		        7190, 7191
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
		
		if (event.equalsIgnoreCase("31683-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31683-06.htm"))
		{
			if (st.getQuestItemsCount(7191) > 4)
			{
				st.takeItems(7191, -1);
				st.setCond(7, true);
			}
			else
			{
				htmltext = "31683-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("31683-10.htm"))
		{
			if (st.getQuestItemsCount(7190) >= 200)
			{
				st.takeItems(7190, -1);
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
			else
			{
				st.setCond(7);
				htmltext = "31683-11.htm";
			}
		}
		else if (event.equalsIgnoreCase("31548-02.htm"))
		{
			if (st.isCond(1))
			{
				st.giveItems(7191, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31549-02.htm"))
		{
			if (st.isCond(2))
			{
				st.giveItems(7191, 1);
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("31550-02.htm"))
		{
			if (st.isCond(3))
			{
				st.giveItems(7191, 1);
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("31551-02.htm"))
		{
			if (st.isCond(4))
			{
				st.giveItems(7191, 1);
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("31552-02.htm"))
		{
			if (st.isCond(5))
			{
				st.giveItems(7191, 1);
				st.setCond(6, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		String htmltext = getNoQuestMsg(player);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "31683-02.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "31683-01.htm";
				}
				break;
			case State.STARTED :
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case 31683 :
						if ((cond >= 1) && (cond <= 5))
						{
							htmltext = "31683-04.htm";
						}
						else if (cond == 6)
						{
							htmltext = "31683-05.htm";
						}
						else if (cond == 7)
						{
							htmltext = "31683-08.htm";
						}
						else if (cond == 8)
						{
							htmltext = "31683-09.htm";
						}
						break;
					case 31548 :
						if (cond == 1)
						{
							htmltext = "31548-01.htm";
						}
						else if (cond >= 2)
						{
							htmltext = "31548-03.htm";
						}
						break;
					case 31549 :
						if (cond == 2)
						{
							htmltext = "31549-01.htm";
						}
						else if (cond >= 3)
						{
							htmltext = "31549-03.htm";
						}
						break;
					case 31550 :
						if (cond == 3)
						{
							htmltext = "31550-01.htm";
						}
						else if (cond >= 4)
						{
							htmltext = "31550-03.htm";
						}
						break;
					case 31551 :
						if (cond == 4)
						{
							htmltext = "31551-01.htm";
						}
						else if (cond >= 5)
						{
							htmltext = "31551-03.htm";
						}
						break;
					case 31552 :
						if (cond == 5)
						{
							htmltext = "31552-01.htm";
						}
						else if (cond >= 6)
						{
							htmltext = "31552-03.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 7);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (st.calcDropItems(getId(), 7190, npc.getId(), 200))
			{
				st.setCond(8, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _603_DaimontheWhiteEyedPart1(603, _603_DaimontheWhiteEyedPart1.class.getSimpleName(), "");
	}
}