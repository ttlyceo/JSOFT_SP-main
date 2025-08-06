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
 * Created by LordWinter 25.06.2011
 * Based on L2J Eternity-World
 */
public final class _170_DangerousAllure extends Quest
{
	private static final String qn = "_170_DangerousAllure";
	
	// Quest NPCs
	private static final int VELLIOR = 30305;
	
	// Quest items
	private static final int NIGHTMARE_CRYSTAL = 1046;
	
	// Quest monsters
	private static final int MERKENIS = 27022;
	
	private _170_DangerousAllure(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(VELLIOR);
		addTalkId(VELLIOR);
		
		addKillId(MERKENIS);
		
		questItemIds = new int[]
		{
		        NIGHTMARE_CRYSTAL
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("1"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
				htmltext = "30305-04.htm";
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
		else if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		
		final int cond = st.getInt("cond");
		
		if (cond == 0)
		{
			if (player.getRace().ordinal() != 2)
			{
				st.exitQuest(true);
				htmltext = "30305-00.htm";
			}
			else if (player.getLevel() < 21)
			{
				st.exitQuest(true);
				htmltext = "30305-02.htm";
			}
			else
			{
				htmltext = "30305-03.htm";
			}
		}
		else
		{
			if (st.getQuestItemsCount(NIGHTMARE_CRYSTAL) != 0)
			{
				st.rewardItems(57, 102680);
				st.addExpAndSp(38607, 4018);
				st.exitQuest(false, true);
				htmltext = "30305-06.htm";
			}
			else
			{
				htmltext = "30305-05.htm";
			}
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
		if (st != null && st.getQuestItemsCount(NIGHTMARE_CRYSTAL) == 0)
		{
			st.giveItems(NIGHTMARE_CRYSTAL, 1);
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "2");
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _170_DangerousAllure(170, qn, "");
	}
}