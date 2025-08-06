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
 * Rework by LordWinter 05.12.2019
 */
public class _310_OnlyWhatRemains extends Quest
{
	public _310_OnlyWhatRemains(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32640);
		addTalkId(32640);

		addKillId(22617);
		addKillId(22618);
		addKillId(22619);
		addKillId(22620);
		addKillId(22621);
		addKillId(22622);
		addKillId(22623);
		addKillId(22624);
		addKillId(22625);
		addKillId(22626);
		addKillId(22627);
		addKillId(22628);
		addKillId(22629);
		addKillId(22630);
		addKillId(22631);
		addKillId(22632);
		addKillId(22633);
		
		registerQuestItems(14880);
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
			case "32640-04.htm":
				if (st.isCreated())
				{
					st.startQuest();
				}
				break;
			case "32640-02.htm":
			case "32640-03.htm":
			case "32640-05.htm":
			case "32640-06.htm":
			case "32640-07.htm":
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
		
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.CREATED:
				final QuestState prev = player.getQuestState("_240_ImTheOnlyOneYouCanTrust");
				htmltext = ((player.getLevel() >= getMinLvl(getId())) && (prev != null) && prev.isCompleted()) ? "32640-01.htm" : "32640-00.htm";
				break;
			case State.STARTED:
				if (cond == 1)
				{
					if (!st.hasQuestItems(14880))
					{
						htmltext = "32640-08.htm";
					}
					else
					{
						htmltext = "32640-09.htm";
					}
				}
				else if (cond == 2)
				{
					st.takeItems(14880, 500);
					st.calcReward(getId());
					st.exitQuest(true, true);
					htmltext = "32640-10.htm";
				}
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
		if (st != null && st.calcDropItems(getId(), 14880, npc.getId(), 500))
		{
			st.setCond(2);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _310_OnlyWhatRemains(310, _310_OnlyWhatRemains.class.getSimpleName(), "");
	}
}