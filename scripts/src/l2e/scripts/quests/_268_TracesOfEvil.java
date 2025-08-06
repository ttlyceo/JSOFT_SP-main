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
 * Created by LordWinter 28.09.2012
 * Based on L2J Eternity-World
 */
public class _268_TracesOfEvil extends Quest
{
	private static final String qn = "_268_TracesOfEvil";

	private static final int[] NPCS =
	{
		20474, 20476, 20478
	};

	public _268_TracesOfEvil(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30559);
		addTalkId(30559);
		
		for (final int mob : NPCS)
		{
			addKillId(mob);
		}

		questItemIds = new int[]
		{
			10869
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("30559-02.htm"))
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
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() < 15)
				{
					htmltext = "30559-00.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "30559-01.htm";
				}
				break;
			case State.STARTED:
				if (st.getQuestItemsCount(10869) >= 30)
				{
					htmltext = "30559-04.htm";
					st.takeItems(10869, -1);
					st.giveItems(57, 2474);
					st.addExpAndSp(8738, 409);
					st.exitQuest(true, true);
				}
				else
				{
					htmltext = "30559-03.htm";
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (st.getQuestItemsCount(10869) < 29)
			{
				st.playSound("ItemSound.quest_itemget");
			}
			else if (st.getQuestItemsCount(10869) >= 29)
			{
				st.playSound("ItemSound.quest_middle");
				st.set("cond", "2");
				st.giveItems(10869, 1);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _268_TracesOfEvil(268, qn, "");
	}
}