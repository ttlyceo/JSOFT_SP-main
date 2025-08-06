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
 * Rework by LordWinter 08.12.2019
 */
public class _053_LinnaeusSpecialBait extends Quest
{
	public _053_LinnaeusSpecialBait(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(31577);
		addTalkId(31577);
		
		addKillId(20670);

		questItemIds = new int[]
		{
		        7624
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31577-1.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31577-3.htm"))
		{
			if (st.getQuestItemsCount(7624) < 100)
			{
				htmltext = "31577-5.htm";
			}
			else
			{
				htmltext = "31577-3.htm";
				st.takeItems(7624, -1);
				st.calcReward(getId());
				st.exitQuest(false, true);
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
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED :
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "31577-0.htm" : "31577-0a.htm";
				break;
			case State.STARTED :
				htmltext = (st.isCond(1)) ? "31577-4.htm" : "31577-2.htm";
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st.calcDropItems(getId(), 7624, npc.getId(), 100))
		{
			st.setCond(2);
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _053_LinnaeusSpecialBait(53, _053_LinnaeusSpecialBait.class.getSimpleName(), "");
	}
}