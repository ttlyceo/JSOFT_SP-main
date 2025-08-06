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
 * Rework by LordWinter 25.02.2020
 */
public class _628_HuntGoldenRam extends Quest
{
	public _628_HuntGoldenRam(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31554);
		addTalkId(31554);
		
		for (int npcId = 21508; npcId <= 21518; npcId++)
		{
			addKillId(npcId);
		}
		
		questItemIds = new int[]
		{
		        7248, 7249
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
		
		if (event.equalsIgnoreCase("31554-02.htm"))
		{
			st.startQuest();
			if (hasQuestItems(player, 7247))
			{
				st.setCond(3);
				htmltext = "31554-05.htm";
			}
			else if (hasQuestItems(player, 7246))
			{
				st.setCond(2);
				htmltext = "31554-04.htm";
			}
		}
		else if (event.equalsIgnoreCase("31554-03a.htm"))
		{
			if ((st.getQuestItemsCount(7248) >= 100) && (st.isCond(1)))
			{
				st.takeItems(7248, -1);
				st.giveItems(7246, 1);
				st.setCond(2, true);
				htmltext = "31554-04.htm";
			}
		}
		else if (event.equalsIgnoreCase("31554-07.htm"))
		{
			st.exitQuest(true, true);
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
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "31554-01.htm";
				}
				else
				{
					htmltext = "31554-01a.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if (st.isCond(1))
				{
					if (st.getQuestItemsCount(7248) >= 100)
					{
						htmltext = "31554-03.htm";
					}
					else
					{
						htmltext = "31554-03a.htm";
					}
				}
				else if (st.isCond(2))
				{
					if ((st.getQuestItemsCount(7248) >= 100) && (st.getQuestItemsCount(7249) >= 100))
					{
						htmltext = "31554-05.htm";
						st.takeItems(7248, -1);
						st.takeItems(7249, -1);
						st.takeItems(7246, 1);
						st.calcReward(getId());
						st.setCond(3, true);
					}
					else if ((!st.hasQuestItems(7248)) && (!st.hasQuestItems(7249)))
					{
						htmltext = "31554-04b.htm";
					}
					else
					{
						htmltext = "31554-04a.htm";
					}
				}
				else if (st.isCond(3))
				{
					htmltext = "31554-05a.htm";
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
			return null;
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (npc.getId() >= 21508 && npc.getId() <= 21512)
			{
				if (st.isCond(1) || st.isCond(2))
				{
					st.calcDoDropItems(getId(), 7248, npc.getId(), 100);
				}
			}
			else if (npc.getId() >= 21513 && npc.getId() <= 21518)
			{
				if (st.isCond(2))
				{
					st.calcDoDropItems(getId(), 7249, npc.getId(), 100);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _628_HuntGoldenRam(628, _628_HuntGoldenRam.class.getSimpleName(), "");
	}
}