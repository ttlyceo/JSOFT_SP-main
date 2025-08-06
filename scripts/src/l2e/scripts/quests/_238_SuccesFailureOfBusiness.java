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
 * Rework by LordWinter 21.04.2020
 */
public class _238_SuccesFailureOfBusiness extends Quest
{
	public _238_SuccesFailureOfBusiness(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(32641);
		addTalkId(32641);

		addKillId(18806);
		addKillId(22659);
		addKillId(22658);

		questItemIds = new int[]
		{
		        14867, 14868
		};
    	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return null;
		}

		if (event.equals("32461-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equals("32461-06.htm"))
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
			}
		}
		return event;
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
			case State.COMPLETED :
				htmltext = "32461-09.htm";
				break;
			case State.CREATED :
				final QuestState qs = player.getQuestState("_237_WindsOfChange");
				final QuestState qs2 = player.getQuestState("_239_WontYouJoinUs");
				if ((qs2 != null) && qs2.isCompleted())
				{
					htmltext = "32461-10.htm";
				}
				else if ((qs != null) && qs.isCompleted() && (player.getLevel() >= getMinLvl(getId())) && st.hasQuestItems(14865))
				{
					htmltext = "32461-01.htm";
				}
				else
				{
					htmltext = "32461-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				switch (st.getCond())
				{
					case 1 :
						htmltext = "32461-04.htm";
						break;
					case 2 :
						if (st.getQuestItemsCount(14867) >= 10)
						{
							st.takeItems(14867, -1);
							htmltext = "32461-05.htm";
						}
						break;
					case 3 :
						htmltext = "32461-07.htm";
						break;
					case 4 :
						if (st.getQuestItemsCount(14868) >= 20)
						{
							htmltext = "32461-08.htm";
							st.takeItems(14865, -1);
							st.takeItems(14868, -1);
							st.calcExpAndSp(getId());
							st.calcReward(getId());
							st.exitQuest(false, true);
						}
						break;
				}
				break;
		}
		return htmltext;
   	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (npc.getId() == 18806)
			{
				if (st.isCond(1) && st.calcDropItems(getId(), 14867, npc.getId(), 10))
				{
					st.setCond(2);
				}
			}
			else if (npc.getId() == 22659 || npc.getId() == 22658)
			{
				if (st.isCond(3) && st.calcDropItems(getId(), 14868, npc.getId(), 20))
				{
					st.setCond(4);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _238_SuccesFailureOfBusiness(238, _238_SuccesFailureOfBusiness.class.getSimpleName(), "");
	}
}