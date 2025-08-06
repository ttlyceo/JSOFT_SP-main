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
 * Rework by LordWinter 27.04.2020
 */
public class _624_TheFinestIngredientsPart1 extends Quest
{
  	public _624_TheFinestIngredientsPart1(int questId, String name, String descr)
  	{
		super(questId, name, descr);

		addStartNpc(31521);
		addTalkId(31521);

		addKillId(21319, 21321, 21317, 21314);

		questItemIds = new int[]
		{
		        7202, 7203, 7204
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

		if (event.equalsIgnoreCase("31521-02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31521-05.htm"))
		{
			if (st.isCond(3) && (st.getQuestItemsCount(7202) >= 50) && (st.getQuestItemsCount(7203) >= 50) && (st.getQuestItemsCount(7204) >= 50))
			{
				st.takeItems(7202, -1);
				st.takeItems(7203, -1);
				st.takeItems(7204, -1);
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
			else
			{
				st.setCond(1);
				htmltext = "31521-07.htm";
			}
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
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "31521-01.htm";
				}
				else
				{
					htmltext = "31521-03.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				if (st.isCond(3))
				{
					if ((st.getQuestItemsCount(7202) >= 50) && (st.getQuestItemsCount(7203) >= 50) && (st.getQuestItemsCount(7204) >= 50))
					{
						htmltext = "31521-04.htm";
					}
					else
					{
						htmltext = "31521-07.htm";
					}
				}
				else
				{
					htmltext = "31521-06.htm";
				}
				break;
		}
		return htmltext;
  	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if ((partyMember != null) && partyMember.isInsideRadius(npc, 1500, true, false))
		{
			final QuestState st = partyMember.getQuestState(getName());
			switch (npc.getId())
    		{
				case 21319 :
					st.calcDoDropItems(getId(), 7202, npc.getId(), 50);
					break;
				case 21317 :
				case 21321 :
					st.calcDoDropItems(getId(), 7204, npc.getId(), 50);
					break;
				case 21314 :
					st.calcDoDropItems(getId(), 7203, npc.getId(), 50);
					break;
    		}
			
			if (st.getQuestItemsCount(7202) >= 50 && st.getQuestItemsCount(7203) >= 50 && st.getQuestItemsCount(7204) >= 50)
    		{
				st.setCond(3, true);
    		}
		}
		return super.onKill(npc, player, isSummon);
  	}

  	public static void main(String[] args)
  	{
		new _624_TheFinestIngredientsPart1(624, _624_TheFinestIngredientsPart1.class.getSimpleName(), "");
  	}
}