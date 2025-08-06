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
 * Rework by LordWinter 31.08.2021
 */
public class _612_WarWithKetraOrcs extends Quest
{
	public _612_WarWithKetraOrcs(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31377);
		addTalkId(31377);
		
		addKillId(21324, 21327, 21328, 21329, 21331, 21332, 21334, 21336, 21338, 21339, 21340, 21342, 21343, 21345, 21347);
		
		questItemIds = new int[]
		{
		        7234
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "31377-03.htm" :
				st.startQuest();
				break;
			case "31377-06.htm" :
				break;
			case "31377-07.htm" :
				if (st.getQuestItemsCount(7234) < 100)
				{
					return "31377-08.htm";
				}
				st.takeItems(7234, 100);
				st.calcReward(getId());
				break;
			case "31377-09.htm" :
				st.exitQuest(true, true);
				break;
			default :
				htmltext = null;
				break;
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		String htmltext = getNoQuestMsg(player);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "31377-01.htm" : "31377-02.htm";
				break;
			case State.STARTED:
				htmltext = (st.hasQuestItems(7234)) ? "31377-04.htm" : "31377-05.htm";
				break;
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player member = getRandomPartyMember(player, 1);
		if (member == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = member.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 7234, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _612_WarWithKetraOrcs(612, _612_WarWithKetraOrcs.class.getSimpleName(), "");
	}
}