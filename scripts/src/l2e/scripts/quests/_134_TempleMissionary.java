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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 14.01.2023
 */
public final class _134_TempleMissionary extends Quest
{
	public _134_TempleMissionary(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30067);
		addTalkId(30067, 31418);

		addKillId(20157, 20229, 20230, 20231, 20232, 20233, 20234, 27339);

		questItemIds = new int[]
		{
		        10335, 10336, 10337, 10338
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final var htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30067-02.htm") && npc.getId() == 30067)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("30067-04.htm") && npc.getId() == 30067)
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30067-08.htm") && npc.getId() == 30067)
		{
			if (st.isCond(5))
			{
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		else if (event.equalsIgnoreCase("31418-02.htm") && npc.getId() == 31418)
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("31418-07.htm") && npc.getId() == 31418)
		{
			if (st.isCond(4))
			{
				st.setCond(5, true);
				st.giveItems(10338, 1);
				st.unset("Report");
			}
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

		final int cond = st.getCond();
		final int npcId = npc.getId();

		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30067-01.htm";
				}
				else
				{
					htmltext = "30067-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if (npcId == 30067)
				{
					if (cond == 1)
					{
						return "30067-02.htm";
					}
					else if ((cond == 2) || (cond == 3) || (cond == 4))
					{
						htmltext = "30067-05.htm";
					}
					else if (cond == 5)
					{
						if (st.getInt("Report") == 1)
						{
							htmltext = "30067-07.htm";
						}
						if (st.getQuestItemsCount(10338) > 0)
						{
							st.takeItems(10338, -1);
							st.set("Report", "1");
							htmltext = "30067-06.htm";
						}
					}
				}

				if (npcId == 31418)
				{
					if (cond == 2)
					{
						htmltext = "31418-01.htm";
					}
					else if (cond == 3)
					{
						final long Tools = st.getQuestItemsCount(10335) / 10;
						if (Tools < 1)
						{
							htmltext = "31418-03.htm";
						}
						st.takeItems(10335, Tools * 10);
						st.giveItems(10336, Tools);
						htmltext = "31418-04.htm";
					}
					else if (cond == 4)
					{
						if (st.getInt("Report") == 1)
						{
							htmltext = "31418-06.htm";
						}
						if (st.getQuestItemsCount(10337) > 2)
						{
							st.takeItems(10335, -1);
							st.takeItems(10336, -1);
							st.takeItems(10337, -1);
							st.set("Report", "1");
							htmltext = "31418-05.htm";
						}
					}
					else if (cond == 5)
					{
						htmltext = "31418-08.htm";
					}
				}
				break;
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var member = getRandomPartyMember(player, 3);
		if (member == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final var st = member.getQuestState(getName());
		if (st != null)
		{
			if (npc.getId() == 27339)
			{
				if (st.calcDropItems(getId(), 10337, npc.getId(), 3))
				{
					st.setCond(4, true);
				}
			}
			else
			{
				if (st.getQuestItemsCount(10336) < 1)
				{
					st.calcDropItems(getId(), 10335, npc.getId(), Integer.MAX_VALUE);
				}
				else
				{
					st.takeItems(10336, 1);
					if (Rnd.chance(45))
					{
						st.addSpawn(27339, npc, true, 900000);
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _134_TempleMissionary(134, _134_TempleMissionary.class.getSimpleName(), "");
	}
}
