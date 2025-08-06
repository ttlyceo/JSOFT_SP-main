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
 * Created by LordWinter 30.06.2012
 * Based on L2J Eternity-World
 */
public class _157_RecoverSmuggledGoods extends Quest
{
	private static final String qn = "_157_RecoverSmuggledGoods";
	
	private static final int WILFORD = 30005;
	private static final int TOAD = 20121;
	
	private static final int ADAMANTITE_ORE = 1024;
	private static final int BUCKLER = 20;
	
	public _157_RecoverSmuggledGoods(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(WILFORD);
		addTalkId(WILFORD);
		
		addKillId(TOAD);
		
		questItemIds = new int[]
		{
		        ADAMANTITE_ORE
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30005-05.htm"))
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
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		final int cond = st.getInt("cond");
		
		switch (st.getState())
		{
			case State.CREATED :
				if ((player.getLevel() >= 5) && (player.getLevel() <= 9))
				{
					htmltext = "30005-03.htm";
				}
				else
				{
					htmltext = "30005-02.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				if ((cond == 1) && (st.getQuestItemsCount(ADAMANTITE_ORE) < 20))
				{
					htmltext = "30005-06.htm";
				}
				else if ((cond == 2) && (st.getQuestItemsCount(ADAMANTITE_ORE) == 20))
				{
					htmltext = "30005-07.htm";
					st.takeItems(ADAMANTITE_ORE, 20);
					st.giveItems(BUCKLER, 1);
					st.exitQuest(false, true);
				}
				break;
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
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
		if (st != null && st.getQuestItemsCount(ADAMANTITE_ORE) < 20)
		{
			st.giveItems(ADAMANTITE_ORE, 1);
			if (st.getQuestItemsCount(ADAMANTITE_ORE) == 20)
			{
				st.playSound("ItemSound.quest_middle");
				st.set("cond", "2");
			}
			else
			{
				st.playSound("ItemSound.quest_itemget");
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _157_RecoverSmuggledGoods(157, qn, "");
	}
}