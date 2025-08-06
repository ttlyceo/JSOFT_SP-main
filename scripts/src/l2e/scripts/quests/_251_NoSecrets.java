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

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 21.04.2020
 */
public class _251_NoSecrets extends Quest
{
	private static final int[] MOB =
	{
	        22775, 22776, 22778
	};
	private static final int[] MOB1 =
	{
	        22780, 22782, 22783, 22784, 22785
	};
	
	public _251_NoSecrets(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30201);
		addTalkId(30201);
		
		for(final int npcId : MOB)
		{
			addKillId(npcId);
		}
		
		for(final int npcId : MOB1)
		{
			addKillId(npcId);
		}

		questItemIds = new int[]
		{
		        15508, 15509
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
			
		if (event.equalsIgnoreCase("30201-05.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "30201-01.htm" : "30201-02.htm";
				break;
			case State.STARTED :
				if (st.isCond(1))
				{
					htmltext = "30201-06.htm";
				}
				else if ((st.isCond(2)) && (st.getQuestItemsCount(15508) >= 10) && (st.getQuestItemsCount(15509) >= 5))
				{
					htmltext = "30201-07.htm";
					st.calcExpAndSp(getId());
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
				break;
			case State.COMPLETED :
				htmltext = "30201-03.htm";
				break;
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if ((st != null) && st.isStarted() && st.isCond(1))
		{
			final int npcId = npc.getId();

			if (ArrayUtils.contains(MOB, npcId))
			{
				st.calcDoDropItems(getId(), 15508, npc.getId(), 10);
			}
			else if (ArrayUtils.contains(MOB1, npcId))
			{
				st.calcDoDropItems(getId(), 15509, npc.getId(), 5);
			}
			
			if ((st.getQuestItemsCount(15508) >= 10) && (st.getQuestItemsCount(15509) >= 5))
			{
				st.setCond(2, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _251_NoSecrets(251, _251_NoSecrets.class.getSimpleName(), "");
	}
}