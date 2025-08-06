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
 * Created by LordWinter 02.10.2012
 * Based on L2J Eternity-World
 */
public class _341_HuntingForWildBeasts extends Quest
{
	private static final String qn = "_341_HuntingForWildBeasts";
	
	// NPC
	private static final int PANO = 30078;
	
	// Item
	private static final int BEAR_SKIN = 4259;
	
	public _341_HuntingForWildBeasts(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(PANO);
		addTalkId(PANO);

		addKillId(20203, 20021, 20310, 20143);

		questItemIds = new int[]
		{
			BEAR_SKIN
		};
	}
			
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30078-02.htm"))
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
		final QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg(player);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= 20 && player.getLevel() <= 24)
				{
					htmltext = "30078-01.htm";
				}
				else
				{
					htmltext = "30078-00.htm";
					st.exitQuest(false);
				}
				break;
			case State.STARTED:
				if (st.getQuestItemsCount(BEAR_SKIN) >= 20)
				{
					htmltext = "30078-04.htm";
					st.takeItems(BEAR_SKIN, -1);
					st.rewardItems(57, 3710);
					st.playSound("ItemSound.quest_finish");
					st.exitQuest(true);
				}
				else
				{
					htmltext = "30078-03.htm";
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.dropQuestItems(BEAR_SKIN, 1, 20, 400000, true);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _341_HuntingForWildBeasts(341, qn, "");
	}
}