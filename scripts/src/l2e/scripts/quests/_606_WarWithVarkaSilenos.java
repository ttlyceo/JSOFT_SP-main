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
public class _606_WarWithVarkaSilenos extends Quest
{
	private _606_WarWithVarkaSilenos(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31370);
		addTalkId(31370);

		addKillId(21350, 21353, 21354, 21355, 21357, 21358, 21360, 21362, 21364, 21365, 21366, 21368, 21369, 21371, 21373);

		questItemIds = new int[]
		{
		        7233
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "31370-03.htm":
				if (st.isCreated())
				{
					st.startQuest();
				}
				break;
			case "31370-06.htm" :
				break;
			case "31370-07.htm" :
				if (st.getQuestItemsCount(7233) < 100)
				{
					return "31370-08.htm";
				}
				st.takeItems(7233, 100);
				st.calcReward(getId());
				break;
			case "31370-09.htm" :
				st.exitQuest(true, true);
				break;
			default:
				htmltext = null;
				break;
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
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "31370-01.htm" : "31370-02.htm";
				break;
			case State.STARTED:
				htmltext = (st.hasQuestItems(7233)) ? "31370-04.htm" : "31370-05.htm";
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Player member = getRandomPartyMember(killer, 1);
		if (member == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		final QuestState st = member.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 7233, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _606_WarWithVarkaSilenos(606, _606_WarWithVarkaSilenos.class.getSimpleName(), "");
	}
}