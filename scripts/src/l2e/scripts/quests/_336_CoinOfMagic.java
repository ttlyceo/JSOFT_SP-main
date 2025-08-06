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
 * Rework by LordWinter 26.05.2021
 */
public class _336_CoinOfMagic extends Quest
{
	private static final int[][] PROMOTE =
	{
	        {},
	        {},
	        {
	                3492, 3474, 3476, 3495, 3484, 3486
			},
			{
			        3473, 3485, 3491, 3475, 3483, 3494
			}
	};

	private static final int[][] EXCHANGE_LEVEL =
	{
	        {
	                30696, 3
			},
			{
			        30673, 3
			},
			{
			        30183, 3
			},
			{
			        30165, 2
			},
			{
			        30200, 2
			},
			{
			        30688, 2
			},
			{
			        30847, 1
			},
			{
			        30092, 1
			},
			{
			        30078, 1
			}
	};

	private static final int[][] DROPLIST =
	{
	        {
	                20584, 3472
			},
			{
			        20585, 3472
			},
			{
			        20587, 3472
			},
			{
			        20604, 3472
			},
			{
			        20678, 3472
			},
			{
			        20583, 3482
			},
			{
			        20663, 3482
			},
			{
			        20235, 3482
			},
			{
			        20146, 3482
			},
			{
			        20240, 3482
			},
			{
			        20245, 3482
			},
			{
			        20568, 3490
			},
			{
			        20569, 3490
			},
			{
			        20685, 3490
			},
			{
			        20572, 3490
			},
			{
			        20161, 3490
			},
			{
			        20575, 3490
			},
			{
			        21003, 3472
			},
			{
			        21006, 3482
			},
			{
			        21008, 3472
			},
			{
			        20674, 3482
			},
			{
			        21282, 3472
			},
			{
			        21284, 3472
			},
			{
			        21283, 3472
			},
			{
			        21287, 3482
			},
			{
			        21288, 3482
			},
			{
			        21286, 3482
			},
			{
			        21521, 3490
			},
			{
			        21526, 3472
			},
			{
			        21531, 3472
			},
			{
			        21539, 3490
			}
	};

	public _336_CoinOfMagic(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30232);

		addTalkId(30232, 30702, 30696, 30183, 30200, 30165, 30847, 30092, 30078, 30688, 30673);

		for (final int mob[] : DROPLIST)
		{
			addKillId(mob[0]);
		}

		addKillId(20644, 20645);

		questItemIds = new int[]
		{
		        3811, 3812, 3813, 3814, 3815
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

		final int cond = st.getCond();

		if (event.equalsIgnoreCase("30702-06.htm"))
		{
			if (cond > 0 && cond < 7)
			{
				st.setCond(7, true);
			}
		}
		else if (event.equalsIgnoreCase("30232-22.htm"))
		{
			if (cond > 0 && cond < 6)
			{
				st.setCond(6, true);
			}
		}
		else if (event.equalsIgnoreCase("30232-23.htm"))
		{
			if (cond > 0 && cond < 5)
			{
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("30702-02.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30232-05.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
				st.giveItems(3811, 1);
			}
		}
		else if (event.equalsIgnoreCase("30232-04.htm") || event.equalsIgnoreCase("30232-18a.htm"))
		{
			st.exitQuest(true);
			st.playSound("ItemSound.quest_giveup");
		}
		else if (event.equalsIgnoreCase("raise"))
		{
			htmltext = promote(st);
		}
		return htmltext;
	}

	private String promote(QuestState st)
	{
		final int grade = st.getInt("grade");
		String html;
		if (grade == 1)
		{
			html = "30232-15.htm";
		}
		else
		{
			int h = 0;
			for (final int i : PROMOTE[grade])
			{
				if (st.getQuestItemsCount(i) > 0)
				{
					h += 1;
				}
			}
			if (h == 6)
			{
				for (final int i : PROMOTE[grade])
				{
					st.takeItems(i, 1);
				}
				html = "30232-" + String.valueOf(19 - grade) + ".htm";
				st.takeItems(3812 + grade, -1);
				st.giveItems(3811 + grade, 1);
				st.set("grade", String.valueOf(grade - 1));
				if (grade == 3)
				{
					st.setCond(9, true);
				}
				else if (grade == 2)
				{
					st.setCond(11, true);
				}
			}
			else
			{
				html = "30232-" + String.valueOf(16 - grade) + ".htm";
				if (grade == 3)
				{
					st.setCond(8, true);
				}
				else if (grade == 2)
				{
					st.setCond(9, true);
				}
			}
		}
		return html;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final byte id = st.getState();
		final int grade = st.getInt("grade");

		if (npcId == 30232)
		{
			if (id == State.CREATED)
			{
				if (st.getPlayer().getLevel() < getMinLvl(getId()))
				{
					htmltext = "30232-01.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "30232-02.htm";
				}
			}
			else if (st.getQuestItemsCount(3811) > 0)
			{
				if (st.getQuestItemsCount(3812) > 0)
				{
					st.takeItems(3812, -1);
					st.takeItems(3811, -1);
					st.giveItems(3815, 1);
					st.set("grade", "3");
					st.setCond(4);
					st.playSound("ItemSound.quest_fanfare_middle");
					htmltext = "30232-07.htm";
				}
				else
				{
					htmltext = "30232-06.htm";
				}
			}
			else if (grade == 3)
			{
				htmltext = "30232-12.htm";
			}
			else if (grade == 2)
			{
				htmltext = "30232-11.htm";
			}
			else if (grade == 1)
			{
				htmltext = "30232-10.htm";
			}
		}
		else if (npcId == 30702)
		{
			if ((st.getQuestItemsCount(3811) > 0) && (grade == 0))
			{
				htmltext = "30702-01.htm";
			}
			else if (grade == 3)
			{
				htmltext = "30702-05.htm";
			}
		}
		else
		{
			for (final int e[] : EXCHANGE_LEVEL)
			{
				if ((npcId == e[0]) && (grade <= e[1]))
				{
					htmltext = npcId + "-01.htm";
				}
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		switch (npc.getId())
		{
			case 20644 :
			case 20645 :
				final Player member = getRandomPartyMember(player, 2);
				if (member != null)
				{
					final QuestState st = member.getQuestState(getName());
					if (st != null && st.calcDropItems(getId(), 3812, npc.getId(), 1))
					{
						st.setCond(3, true);
					}
				}
				return super.onKill(npc, player, isSummon);
		}
		
		final Player member = getRandomPartyMemberState(player, State.STARTED);
		if (member != null)
		{
			final QuestState st = member.getQuestState(getName());
			if (st != null)
			{
				for (final int[] info : DROPLIST)
				{
					if (info[0] == npc.getId())
					{
						st.calcDropItems(getId(), info[1], npc.getId(), Integer.MAX_VALUE);
						break;
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _336_CoinOfMagic(336, _336_CoinOfMagic.class.getSimpleName(), "");
	}
}