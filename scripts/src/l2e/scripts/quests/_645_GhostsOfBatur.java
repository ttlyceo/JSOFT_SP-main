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
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 03.10.2022
 */
public class _645_GhostsOfBatur extends Quest
{
	public _645_GhostsOfBatur(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(32017);
		addTalkId(32017);
		
		addKillId(22703, 22704, 22705, 22706);
		
		questItemIds = new int[]
		{
		        14861
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("32017-03.htm"))
		{
			if (player.getLevel() < getMinLvl(getId()))
			{
				htmltext = "32017-02.htm";
				st.exitQuest(true);
			}
			else
			{
				st.startQuest();
			}
		}
		
		else if (event.equalsIgnoreCase("32017-06.htm"))
		{
			if (player.getLevel() < getMinLvl(getId()))
			{
				htmltext = "32017-02.htm";
				st.exitQuest(true);
			}
			else
			{
				htmltext = "32017-06.htm";
			}
		}
		
		else if (event.equalsIgnoreCase("REWARDS"))
		{
			if (st.getQuestItemsCount(14861) >= 500)
			{
				st.takeItems(14861, 500);
				st.calcReward(getId(), 1, true);
				st.playSound("ItemSound.quest_middle");
				htmltext = "32017-05c.htm";
			}
			else
			{
				htmltext = "32017-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("LEO"))
		{
			if (st.getQuestItemsCount(14861) >= 8)
			{
				st.takeItems(14861, 8);
				st.calcReward(getId(), 2);
				st.playSound("ItemSound.quest_middle");
				htmltext = "32017-05c.htm";
			}
			else
			{
				htmltext = "32017-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("ADA"))
		{
			if (st.getQuestItemsCount(14861) >= 15)
			{
				st.takeItems(14861, 15);
				st.calcReward(getId(), 3);
				st.playSound("ItemSound.quest_middle");
				htmltext = "32017-05c.htm";
			}
			else
			{
				htmltext = "32017-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("ORI"))
		{
			if (st.getQuestItemsCount(14861) >= 12)
			{
				st.takeItems(14861, 12);
				st.calcReward(getId(), 4);
				st.playSound("ItemSound.quest_middle");
				htmltext = "32017-05c.htm";
			}
			else
			{
				htmltext = "32017-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("32017-08.htm"))
		{
			st.takeItems(14861, -1);
			st.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				htmltext = "32017-01.htm";
				break;
			case State.STARTED :
				switch (st.getCond())
				{
					case 0:
						htmltext = "32017-04.htm";
						break;
					case 1:
						if (st.getQuestItemsCount(14861) > 0)
						{
							htmltext = "32017-05b.htm";
						}
						else
						{
							htmltext = "32017-05a.htm";
						}
						break;
					default:
						htmltext = "32017-02.htm";
						st.exitQuest(true);
						break;
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final var st = partyMember.getQuestState(getName());
		if (st != null && st.isCond(1))
		{
			st.calcDropItems(getId(), 14861, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _645_GhostsOfBatur(645, _645_GhostsOfBatur.class.getSimpleName(), "");
	}
}
