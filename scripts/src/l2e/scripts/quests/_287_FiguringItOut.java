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
 * Rework by LordWinter 10.09.2020
 */
public class _287_FiguringItOut extends Quest
{
	public _287_FiguringItOut(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32742);
		addTalkId(32742);

		addKillId(22768, 22769, 22770, 22771, 22772, 22773, 22774);
		
		questItemIds = new int[]
		{
		        15499
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("32742-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32742-05.htm"))
		{
			if (st.getQuestItemsCount(15499) >= 100)
			{
				st.takeItems(15499, 100);
				calcReward(player, getId(), 1, true);
				htmltext = "32742-07.htm";
			}
			else
			{
				htmltext = "32742-05.htm";
			}
		}
		else if (event.equalsIgnoreCase("32742-09.htm"))
		{
			if (st.getQuestItemsCount(15499) >= 500)
			{
				st.takeItems(15499, 500);
				calcReward(player, getId(), 2, true);
				htmltext = "32742-07.htm";
			}
			else
			{
				htmltext = "32742-09.htm";
			}
		}
		else if (event.equalsIgnoreCase("32742-08.htm"))
		{
			st.exitQuest(true, true);
		}
		return htmltext;
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		final QuestState prev = player.getQuestState(_250_WatchWhatYouEat.class.getSimpleName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				htmltext = ((player.getLevel() >= getMinLvl(getId())) && (prev != null) && prev.isCompleted()) ? "32742-01.htm" : "32742-02.htm";
				break;
			case State.STARTED :
				htmltext = (st.getQuestItemsCount(15499) < 100) ? "32742-05.htm" : "32742-04.htm";
				break;
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
		if (st.isCond(1))
		{
			st.calcDoDropItems(getId(), 15499, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
		
	public static void main(String[] args)
	{
		new _287_FiguringItOut(287, _287_FiguringItOut.class.getSimpleName(), "");
	}
}