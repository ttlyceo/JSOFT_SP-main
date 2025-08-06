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
public class _270_TheOneWhoEndsSilence extends Quest
{
	public _270_TheOneWhoEndsSilence(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32757);
		addTalkId(32757);

		addKillId(22790, 22791, 22793, 22789, 22797, 22795, 22794, 22796, 22800, 22798, 22799);
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

		final long count = st.getQuestItemsCount(15526);
		final long random = getRandom(2);
		
		if (event.equalsIgnoreCase("32757-03.htm"))
		{
			if (st.isCreated())
			{
				final var prev = player.getQuestState("_10288_SecretMission");
				if (prev != null && prev.isCompleted() && player.getLevel() >= getMinLvl(getId()))
				{
					st.startQuest();
				}
			}
		}
		else if (event.equalsIgnoreCase("32757-05.htm"))
		{
			if (count >= 100)
			{
				if (random == 0)
				{
					st.takeItems(15526, 100);
					st.calcReward(getId(), 1, true);
				}
				else if (random == 1)
				{
					st.takeItems(15526, 100);
					st.calcReward(getId(), 3, true);
				}
				htmltext = "32757-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("32757-09.htm"))
		{
			if (count >= 200)
			{
				st.takeItems(15526, 200);
				st.calcReward(getId(), 1, true);
				st.calcReward(getId(), 3, true);
				htmltext = "32757-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("32757-10.htm"))
		{
			if (count >= 300)
			{
				st.takeItems(15526, 300);
				st.calcReward(getId(), 1, true);
				st.calcReward(getId(), 2, true);
				st.calcReward(getId(), 3, true);
			}
		}
		else if (event.equalsIgnoreCase("32757-11.htm"))
		{
			if (count >= 400)
			{
				if (random == 0)
				{
					st.takeItems(15526, 400);
					st.calcReward(getId(), 1, true);
					st.calcReward(getId(), 3, true);
				}
				if (random == 1)
				{
					st.takeItems(15526, 400);
					st.calcReward(getId(), 1, true);
					st.calcReward(getId(), 2, true);
					st.calcReward(getId(), 3, true);
				}
				htmltext = "32757-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("32757-12.htm"))
		{
			if (count >= 500)
			{
				st.takeItems(15526, 500);
				st.calcReward(getId(), 1, true);
				st.calcReward(getId(), 2, true);
				st.calcReward(getId(), 3, true);
				htmltext = "32757-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("32757-08.htm"))
		{
			st.takeItems(15526, -1);
			st.exitQuest(true, true);
		}
		return htmltext;
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int id = st.getState();
		final int cond = st.getCond();
		final int npcId = npc.getId();
		final long count = st.getQuestItemsCount(15526);

		if (npcId == 32757)
		{
			if(id == State.CREATED && cond == 0)
			{
				final var prev = player.getQuestState("_10288_SecretMission");
				if (player.getLevel() >= getMinLvl(getId()))
				{
					if (prev != null && prev.isCompleted())
					{
						htmltext = "32757-01.htm";
					}
					else
					{
						htmltext = "32757-02a.htm";
					}
				}
				else
				{
					htmltext = "32757-02.htm";
				}
			}
			else if(id == State.STARTED && cond == 1)
			{
				if (count >= 100)
				{
					htmltext = "32757-04.htm";
				}
				else
				{
					htmltext = "32757-05.htm";
				}
			}
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final var st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 15526, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
		
	public static void main(String[] args)
	{
		new _270_TheOneWhoEndsSilence(270, _270_TheOneWhoEndsSilence.class.getSimpleName(), "");
	}
}