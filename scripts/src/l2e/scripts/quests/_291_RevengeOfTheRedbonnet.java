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
 * Created by LordWinter 06.07.2012
 * Based on L2J Eternity-World
 */
public class _291_RevengeOfTheRedbonnet extends Quest
{
	private static final String qn = "_291_RevengeOfTheRedbonnet";
	
	// Quest items
	private static final int BlackWolfPelt = 1482;
	
	// Rewards
	private static final int ScrollOfEscape = 736;
	private static final int GrandmasPearl = 1502;
	private static final int GrandmasMirror = 1503;
	private static final int GrandmasNecklace = 1504;
	private static final int GrandmasHairpin = 1505;
	
	public _291_RevengeOfTheRedbonnet(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30553);
		addTalkId(30553);
		
		addKillId(20317);

		questItemIds = new int[] { BlackWolfPelt };
	}
			
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30553-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if(st == null)
		{
			return htmltext;
		}
		
		final int cond = st.getInt("cond");

		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() < 4 && player.getLevel() > 8)
				{
					htmltext = "30553-01.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "30553-02.htm";
				}
				break;
			case State.STARTED:
				if (cond == 1)
				{
					htmltext = "30553-04.htm";
				}
				else if (cond == 2)
				{
					if (st.getQuestItemsCount(BlackWolfPelt) >= 40)
					{
						st.takeItems(BlackWolfPelt, -1);
						
						final int random = getRandom(100);
						if (random < 3)
						{
							st.giveItems(GrandmasPearl, 1);
						}
						else if (random < 21)
						{
							st.giveItems(GrandmasMirror, 1);
						}
						else if (random < 46)
						{
							st.giveItems(GrandmasNecklace, 1);
						}
						else
						{
							st.giveItems(ScrollOfEscape, 1);
							st.giveItems(GrandmasHairpin, 1);
						}
						
						htmltext = "30553-05.htm";
						st.playSound("ItemSound.quest_finish");
						st.exitQuest(true);
					}
					else
					{
						st.set("cond", "1");
						htmltext = "30553-04.htm";
					}
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
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (st.dropQuestItems(BlackWolfPelt, 1, 40, 1000000, true))
			{
				st.set("cond", "2");
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _291_RevengeOfTheRedbonnet(291, qn, "");
	}
}