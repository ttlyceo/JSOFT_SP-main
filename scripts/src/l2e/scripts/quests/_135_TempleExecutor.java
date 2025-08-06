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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 14.01.2023
 */
public class _135_TempleExecutor extends Quest
{
	public _135_TempleExecutor(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30068);
		addTalkId(30068, 30291, 31773, 30078);

		addKillId(20781, 21104, 21105, 21106, 21107);

		questItemIds = new int[]
		{
		        10328, 10329, 10330, 10331, 10332, 10333
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30068-02.htm") && npc.getId() == 30068)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("30068-09.htm") && npc.getId() == 30068)
		{
			if (st.isCond(5))
			{
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		else if (event.equalsIgnoreCase("30068-03.htm") && npc.getId() == 30068)
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30291-06.htm") && npc.getId() == 30291)
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
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

		final int npcId = npc.getId();
		final int cond = st.getCond();

		switch (st.getState())
		{
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30068-01.htm";
				}
				else
				{
					st.exitQuest(true);
					htmltext = "30068-00.htm";
				}
				break;
			case State.STARTED :
				if (npcId == 30068)
				{
					if (cond == 1)
					{
						htmltext = "30068-02.htm";
					}
					else if ((cond == 2) || (cond == 3) || (cond == 4))
					{
						htmltext = "30068-04.htm";
					}
					else if (cond == 5)
					{
						if (st.getInt("Report") == 1)
						{
							htmltext = "30068-06.htm";
						}
						if (st.getQuestItemsCount(10331) > 0 && st.getQuestItemsCount(10332) > 0 && st.getQuestItemsCount(10333) > 0)
						{
							st.takeItems(10332, -1);
							st.takeItems(10331, -1);
							st.takeItems(10333, -1);
							st.set("Report", "1");
							htmltext = "30068-05.htm";
						}
					}
				}
				else if (npcId == 30291)
				{
					if (cond == 2)
					{
						htmltext = "30291-01.htm";
					}
					else if (cond == 3)
					{
						htmltext = "30291-07.htm";
					}
					else if (cond == 4)
					{
						if (st.getQuestItemsCount(10331) > 0 && st.getQuestItemsCount(10332) > 0)
						{
							st.setCond(5, true);
							st.takeItems(10330, -1);
							st.giveItems(10333, 1);
							htmltext = "30291-09.htm";
						}
						htmltext = "30291-08.htm";
					}
					else if (cond == 5)
					{
						htmltext = "30291-10.htm";
					}
				}
				else if (npcId == 31773)
				{
					if (cond == 4)
					{
						if (st.getQuestItemsCount(10328) < 10)
						{
							htmltext = "31773-02.htm";
						}
						st.takeItems(10328, -1);
						st.giveItems(10331, 1);
						st.playSound("ItemSound.quest_middle");
						htmltext = "31773-01.htm";
					}
				}
				else if (npcId == 30078)
				{
					if (cond == 4)
					{
						if (st.getQuestItemsCount(10329) < 10)
						{
							htmltext = "30078-02.htm";
						}
						st.takeItems(10329, -1);
						st.giveItems(10332, 1);
						st.playSound("ItemSound.quest_middle");
						htmltext = "30078-01.htm";
					}
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
		final var member = getRandomPartyMember(player, 3);
		if (member == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final var st = member.getQuestState(getName());
		if (st != null)
		{
			final List<Integer> drops = new ArrayList<>();
			if (st.getQuestItemsCount(10328) < 10)
			{
				drops.add(10328);
			}
			if (st.getQuestItemsCount(10329) < 10)
			{
				drops.add(10329);
			}
			if (st.getQuestItemsCount(10330) < 10)
			{
				drops.add(10330);
			}
			
			if (!drops.isEmpty())
			{
				final int itemId = drops.get(getRandom(drops.size()));
				st.calcDropItems(getId(), itemId, npc.getId(), 10);
				if (st.getQuestItemsCount(10328) >= 10 && st.getQuestItemsCount(10329) >= 10 && st.getQuestItemsCount(10330) >= 10)
				{
					st.setCond(4, true);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

  	public static void main(String[] args)
  	{
		new _135_TempleExecutor(135, _135_TempleExecutor.class.getSimpleName(), "");
  	}
}