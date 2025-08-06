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
 * Rework by LordWinter 21.04.2020
 */
public class _290_ThreatRemoval extends Quest
{
	public _290_ThreatRemoval(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30201);
		addTalkId(30201);

		addKillId(22775, 22776, 22777, 22778, 22780, 22781, 22782, 22783, 22784, 22785);
		
		questItemIds = new int[]
		{
		        15714
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
			
		final long count = st.getQuestItemsCount(15714);

		final int random = getRandom(1, 3);
			
		if (event.equalsIgnoreCase("30201-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("30201-06.htm"))
		{
			if (count >= 400)
			{
				st.takeItems(15714, 400);
				st.calcReward(getId(), random);
			}
		}
		else if (event.equalsIgnoreCase("30201-07.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30201-09.htm"))
		{
			st.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		switch (st.getState())
		{
			case State.CREATED :
			{
				final QuestState qs = player.getQuestState("_251_NoSecrets");
				htmltext = ((player.getLevel() >= getMinLvl(getId())) && (qs != null) && (qs.isCompleted())) ? "30201-02.htm" : "30201-01.htm";
				break;
			}
			case State.STARTED :
			{
				if (st.isCond(1))
				{
					htmltext = (st.getQuestItemsCount(15714) < 400) ? "30201-04.htm" : "30201-05.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDoDropItems(getId(), 15714, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _290_ThreatRemoval(290, _290_ThreatRemoval.class.getSimpleName(), "");
	}
}