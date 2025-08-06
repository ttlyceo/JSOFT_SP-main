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
public class _331_ArrowOfVengeance extends Quest
{
	private final static String qn = "_331_ArrowOfVengeance";
	
	// Npc
	private static final int BELTON = 30125;
	
	// Items
	private static final int HARPY_FEATHER = 1452;
	private static final int MEDUSA_VENOM = 1453;
	private static final int WYRMS_TOOTH = 1454;

	public _331_ArrowOfVengeance(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(BELTON);
		addTalkId(BELTON);

		addKillId(20145, 20158, 20176);

		questItemIds = new int[]  { HARPY_FEATHER, MEDUSA_VENOM, WYRMS_TOOTH };
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
		
		if (event.equalsIgnoreCase("30125-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30125-06.htm"))
		{
			st.playSound("ItemSound.quest_finish");
			st.exitQuest(true);
		}
		return htmltext;
	}
		
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= 32 && player.getLevel() <= 39)
				{
					htmltext = "30125-02.htm";
				}
				else
				{
					htmltext = "30125-01.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				final long harpyFeather = st.getQuestItemsCount(HARPY_FEATHER);
				final long medusaVenom = st.getQuestItemsCount(MEDUSA_VENOM);
				final long wyrmTooth = st.getQuestItemsCount(WYRMS_TOOTH);
				
				if (harpyFeather + medusaVenom + wyrmTooth > 0)
				{
					htmltext = "30125-05.htm";
					st.takeItems(HARPY_FEATHER, -1);
					st.takeItems(MEDUSA_VENOM, -1);
					st.takeItems(WYRMS_TOOTH, -1);
					
					long reward = harpyFeather * 78 + medusaVenom * 88 + wyrmTooth * 92;
					if (harpyFeather + medusaVenom + wyrmTooth > 10)
					{
						reward += 3100;
					}
					
					st.rewardItems(57, reward);
				}
				else
				{
					htmltext = "30125-04.htm";
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
			if (st.getRandom(10) < 5)
			{
				switch (npc.getId())
				{
					case 20145 :
						st.giveItems(HARPY_FEATHER, 1);
						break;
					
					case 20158 :
						st.giveItems(MEDUSA_VENOM, 1);
						break;
					
					case 20176 :
						st.giveItems(WYRMS_TOOTH, 1);
						break;
				}
				st.playSound("ItemSound.quest_itemget");
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _331_ArrowOfVengeance(331, qn, "");
	}
}