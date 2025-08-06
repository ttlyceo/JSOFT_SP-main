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

public class _602_ShadowOfLight extends Quest
{
	private static final int EYE_OF_ARGOS = 31683;
	
	private static final int EYE_OF_DARKNESS = 7189;
	
	private static final int[] MOBS =
	{
		21299, 21304
	};
	
	private static final int[][] REWARD =
	{
		{ 6699 ,40000, 120000, 20000 },
		{ 6698, 60000, 110000, 15000 },
		{ 6700, 40000, 150000, 10000 },
		{ 0 ,100000, 140000, 11250 }
	};

	public _602_ShadowOfLight(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS);

		addKillId(MOBS);
		
		registerQuestItems(EYE_OF_DARKNESS);
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
			case "31683-02.htm":
				st.startQuest();
				break;
			case "31683-05.htm":
				if (st.getQuestItemsCount(EYE_OF_DARKNESS) < 100)
				{
					return "31683-06.htm";
				}
				
				final int i = getRandom(4);
				if (i < 3)
				{
					st.giveItems(REWARD[i][0], 3);
				}
				st.rewardItems(57, REWARD[i][1]);
				st.addExpAndSp(REWARD[i][2], REWARD[i][3]);
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
				htmltext = (player.getLevel() >= 68) ? "31683-01.htm" : "31683-00.htm";
				break;
			case State.STARTED:
				htmltext = (st.isCond(1)) ? "31683-03.htm" : "31683-04.htm";
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
		if (st != null)
		{
			final int chance = (npc.getId() == MOBS[0]) ? 560 : 800;
		
			if (getRandom(1000) < chance)
			{
				st.giveItems(EYE_OF_DARKNESS, 1);
				if (st.getQuestItemsCount(EYE_OF_DARKNESS) == 100)
				{
					st.setCond(2, true);
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _602_ShadowOfLight(602, _602_ShadowOfLight.class.getSimpleName(), "");
	}
}