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
 * Rework by LordWinter 02.05.2021
 */
public class _639_GuardiansOfTheHolyGrail extends Quest
{
	public _639_GuardiansOfTheHolyGrail(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31350);
		addTalkId(31350, 32008, 32028);
		
		for (int i = 22789; i <= 22800; i++)
		{
			addKillId(i);
		}
		
		addKillId(18909, 18910);
		
		questItemIds = new int[]
		{
		        8070, 8071, 8069
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("31350-03.htm") && npc.getId() == 31350)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31350-07.htm") && npc.getId() == 31350)
		{
			st.exitQuest(true, true);
		}
		else if (event.equalsIgnoreCase("31350-08.htm") && npc.getId() == 31350)
		{
			final long items = st.getQuestItemsCount(8069);
			if (items > 0)
			{
				st.takeItems(8069, -1);
				st.calcRewardPerItem(getId(), 1, (int) items);
			}
		}
		else if (event.equalsIgnoreCase("32008-05.htm") && npc.getId() == 32008)
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
				st.giveItems(8070, 1);
			}
		}
		else if (event.equalsIgnoreCase("32028-02.htm") && npc.getId() == 32028)
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
				st.takeItems(8070, -1);
				st.giveItems(8071, 1);
			}
		}
		else if (event.equalsIgnoreCase("32008-07.htm") && npc.getId() == 32008)
		{
			if (st.isCond(3))
			{
				st.setCond(4, true);
				st.takeItems(8071, -1);
			}
		}
		else if (event.equalsIgnoreCase("32008-08a.htm") && npc.getId() == 32008)
		{
			if (st.getQuestItemsCount(8069) >= 4000 && st.isCond(4))
			{
				st.takeItems(8069, 4000);
				st.calcReward(getId(), 2);
			}
		}
		else if (event.equalsIgnoreCase("32008-08b.htm") && npc.getId() == 32008)
		{
			if (st.getQuestItemsCount(8069) >= 400 && st.isCond(4))
			{
				st.takeItems(8069, 400);
				st.calcReward(getId(), 3);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.CREATED:
				if (npcId == 31350)
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "31350-01.htm";
					}
					else
					{
						htmltext = "31350-00.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED:
				if (npcId == 31350)
				{
					if (st.getQuestItemsCount(8069) > 0)
					{
						htmltext = "31350-04.htm";
					}
					else
					{
						htmltext = "31350-05.htm";
					}
				}
				else if (npcId == 32008)
				{
					if (cond == 1)
					{
						htmltext = "32008-01.htm";
					}
					else if (cond == 2)
					{
						htmltext = "32008-05b.htm";
					}
					else if (cond == 3)
					{
						htmltext = "32008-06.htm";
					}
					else if (cond == 4)
					{
						if (st.getQuestItemsCount(8069) < 400)
						{
							htmltext = "32008-08d.htm";
						}
						else if (st.getQuestItemsCount(8069) >= 4000)
						{
							htmltext = "32008-08c.htm";
						}
						else
						{
							htmltext = "32008-08.htm";
						}
					}
				}
				else if (npcId == 32028)
				{
					if (cond == 2)
					{
						htmltext = "32028-01.htm";
					}
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 8069, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _639_GuardiansOfTheHolyGrail(639, _639_GuardiansOfTheHolyGrail.class.getSimpleName(), "");
	}
}