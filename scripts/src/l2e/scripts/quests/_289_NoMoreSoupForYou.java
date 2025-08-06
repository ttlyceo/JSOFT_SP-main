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
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 17.06.2023
 */
public class _289_NoMoreSoupForYou extends Quest
{
	public _289_NoMoreSoupForYou(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30200);
		addTalkId(30200);

		addKillId(18908, 22779, 22786, 22787, 22788);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30200-03.htm"))
		{
			if (st.isCreated())
			{
				final var prev = player.getQuestState("_252_ItSmellsDelicious");
				if ((prev != null) && (prev.isCompleted()) && (player.getLevel() >= getMinLvl(getId())))
				{
					st.startQuest();
				}
			}
		}
		else if (event.equalsIgnoreCase("30200-05.htm"))
		{
			if (st.getQuestItemsCount(15712) >= 500)
			{
				st.takeItems(15712, 500);
				st.calcReward(getId(), 1, true);
				htmltext = "30200-04.htm";
			}
			else
			{
				htmltext = "30200-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("30200-06.htm"))
		{
			if (st.getQuestItemsCount(15712) >= 100)
			{
				st.takeItems(15712, 100);
				st.calcReward(getId(), 2, true);
				htmltext = "30200-04.htm";
			}
			else
			{
				htmltext = "30200-07.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (npc.getId() == 30200)
		{
			switch (st.getState())
			{
				case State.CREATED :
					final var prev = player.getQuestState("_252_ItSmellsDelicious");
					if ((prev != null) && (prev.isCompleted()) && (player.getLevel() >= getMinLvl(getId())))
					{
						htmltext = "30200-01.htm";
					}
					else
					{
						htmltext = "30200-00.htm";
					}
					break;
				case State.STARTED :
					if (st.getInt("cond") == 1)
					{
						if (st.getQuestItemsCount(15712) >= 100)
						{
							htmltext = "30200-04.htm";
						}
						else
						{
							htmltext = "30200-03.htm";
						}
					}
					break;
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
		
		final var st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 15712, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _289_NoMoreSoupForYou(289, _289_NoMoreSoupForYou.class.getSimpleName(), "");
	}
}