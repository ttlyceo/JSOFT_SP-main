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
 * Rework by LordWinter 13.04.2020
 */
public class _634_InSearchOfFragmentsOfDimension extends Quest
{
	public _634_InSearchOfFragmentsOfDimension(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		for (int npcId = 31494; npcId < 31508; npcId++)
		{
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		for (int mobs = 21208; mobs < 21256; mobs++)
		{
			addKillId(mobs);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("05.htm"))
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
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "01a.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "01.htm";
				}
				break;
			case State.STARTED:
				htmltext = "03.htm";
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
		if (st != null)
		{
			st.calcDoDropItems(getId(), 7079, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _634_InSearchOfFragmentsOfDimension(634, _634_InSearchOfFragmentsOfDimension.class.getSimpleName(), "");
	}
}