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

import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Created by LordWinter 12.01.2013 Based on L2J Eternity-World
 */
public class _608_SlayTheEnemyCommander extends Quest
{
	private static final String qn = "_608_SlayTheEnemyCommander";

	private static final int MOS_HEAD = 7236;
	private static final int TOTEM = 7220;

	public _608_SlayTheEnemyCommander(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31370);
		addTalkId(31370);

		addKillId(25312);

		questItemIds = new int[]
		{
		        MOS_HEAD
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;

		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31370-04.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31370-07.htm"))
		{
			if (st.getQuestItemsCount(MOS_HEAD) == 1 && st.isCond(2))
			{
				st.takeItems(MOS_HEAD, -1);
				st.giveItems(TOTEM, 1);
				st.addExpAndSp(10000, 0);
				st.exitQuest(true, true);
			}
			else
			{
				htmltext = "31370-06.htm";
				st.set("cond", "1");
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
		if (st == null)
		{
			return htmltext;
		}

		switch (st.getState())
		{
			case State.CREATED :
				htmltext = "31370-01.htm";
				break;
			case State.STARTED :
				if (st.getQuestItemsCount(MOS_HEAD) > 0)
				{
					htmltext = "31370-05.htm";
				}
				else
				{
					htmltext = "31370-06.htm";
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		executeForEachPlayer(player, npc, isSummon, true, false);
		return super.onKill(npc, player, isSummon);
	}

	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState st = player.getQuestState(qn);
		if ((st != null) && st.isCond(1) && Util.checkIfInRange(1500, npc, player, false))
		{
			st.giveItems(MOS_HEAD, 1);
			st.setCond(2, true);
		}
	}

	public static void main(String[] args)
	{
		new _608_SlayTheEnemyCommander(608, qn, "");
	}
}