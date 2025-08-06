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
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;

/**
 * Created by LordWinter 09.08.2011 Based on L2J Eternity-World
 */
public class _104_SpiritOfMirrors extends Quest
{
	private static final String qn = "_104_SpiritOfMirrors";

	private final static int GALLINT_OAK_WAND = 748;
	private final static int WAND_SPIRITBOUND1 = 1135;
	private final static int WAND_SPIRITBOUND2 = 1136;
	private final static int WAND_SPIRITBOUND3 = 1137;

	// REWARDS
	private final static int LONG_SWORD = 2;
	private final static int WAND_OF_ADEPT = 747;
	private final static int SPIRITSHOT_NO_GRADE_FOR_BEGINNERS = 5790;
	private final static int SOULSHOT_NO_GRADE_FOR_BEGINNERS = 5789;
	private final static int SPIRITSHOT_NO_GRADE = 2509;
	private final static int SOULSHOT_NO_GRADE = 1835;
	private final static int LESSER_HEALING_POT = 1060;

	// NPC
	private final static int GALLINT = 30017;
	private final static int ARNOLD = 30041;
	private final static int JOHNSTONE = 30043;
	private final static int KENYOS = 30045;

	private final int[] talkNpc =
	{
	                30017, 30041, 30043, 30045,
	};

	private static final int[][] DROPLIST_COND =
	{
	                {
	                                27003, WAND_SPIRITBOUND1
			},
			{
			                27004, WAND_SPIRITBOUND2
			},
			{
			                27005, WAND_SPIRITBOUND3
			}
	};

	public _104_SpiritOfMirrors(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(GALLINT);
		for (final int npcId : talkNpc)
		{
			addTalkId(npcId);
		}

		for (final int[] element : DROPLIST_COND)
		{
			addKillId(element[0]);
		}

		questItemIds = new int[]
		{
		                WAND_SPIRITBOUND1, WAND_SPIRITBOUND2, WAND_SPIRITBOUND3, GALLINT_OAK_WAND
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30017-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
				st.giveItems(GALLINT_OAK_WAND, 3);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}

		final int npcId = npc.getId();
		final int cond = st.getInt("cond");
		final int id = st.getState();

		if ((id == State.CREATED) && (cond == 0))
		{
			if (npcId == GALLINT)
			{
				if (player.getRace().ordinal() != 0)
				{
					htmltext = "30017-00.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= 10)
				{
					htmltext = "30017-02.htm";
					return htmltext;
				}
				else
				{
					htmltext = "30017-06.htm";
					st.exitQuest(true);
				}
			}
		}
		else if (id == State.STARTED)
		{
			if (npcId == GALLINT)
			{
				if (((cond == 1) && (st.getQuestItemsCount(GALLINT_OAK_WAND) >= 1) && (st.getQuestItemsCount(WAND_SPIRITBOUND1) == 0)) || (st.getQuestItemsCount(WAND_SPIRITBOUND2) == 0) || (st.getQuestItemsCount(WAND_SPIRITBOUND3) == 0))
				{
					htmltext = "30017-04.htm";
				}
				else if ((cond == 3) && (st.getQuestItemsCount(WAND_SPIRITBOUND1) == 1) && (st.getQuestItemsCount(WAND_SPIRITBOUND2) == 1) && (st.getQuestItemsCount(WAND_SPIRITBOUND3) == 1))
				{
					st.takeItems(WAND_SPIRITBOUND1, 1);
					st.takeItems(WAND_SPIRITBOUND2, 1);
					st.takeItems(WAND_SPIRITBOUND3, 1);
					st.giveItems(LESSER_HEALING_POT, 100);
					for (int ECHO_CHRYSTAL = 4412; ECHO_CHRYSTAL <= 4416; ECHO_CHRYSTAL++)
					{
						st.giveItems(ECHO_CHRYSTAL, 10);
					}
					if (player.getClassId().isMage())
					{
						st.giveItems(SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 3000);
						st.giveItems(SPIRITSHOT_NO_GRADE, 500);
						st.giveItems(WAND_OF_ADEPT, 1);
					}
					else
					{
						st.giveItems(SOULSHOT_NO_GRADE_FOR_BEGINNERS, 6000);
						st.giveItems(SOULSHOT_NO_GRADE, 1000);
						st.giveItems(LONG_SWORD, 1);
					}
					showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_RACE_SPECIFIC_WEAPON_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
					st.addExpAndSp(39750, 3407);
					st.giveItems(57, 16866);
					htmltext = "30017-05.htm";
					st.unset("cond");
					st.exitQuest(false);
					st.playSound("ItemSound.quest_finish");
				}
			}
			else if ((npcId == ARNOLD) || (npcId == JOHNSTONE) || ((npcId == KENYOS) && (cond >= 1)))
			{
				st.set("cond", "2");
				st.playSound("ItemSound.quest_middle");
				htmltext = npcId + "-01.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return null;
		}

		final int npcId = npc.getId();
		for (final int[] element : DROPLIST_COND)
		{
			if ((st.getInt("cond") >= 1) && (st.getItemEquipped(Inventory.PAPERDOLL_RHAND) == GALLINT_OAK_WAND) && (npcId == element[0]) && (st.getQuestItemsCount(element[1]) == 0))
			{
				st.takeItems(GALLINT_OAK_WAND, 1);
				st.giveItems(element[1], 1);

				final long HaveAllQuestItems = st.getQuestItemsCount(WAND_SPIRITBOUND1) + st.getQuestItemsCount(WAND_SPIRITBOUND2) + st.getQuestItemsCount(WAND_SPIRITBOUND3);
				if (HaveAllQuestItems == 3)
				{
					st.set("cond", "3");
					st.playSound("ItemSound.quest_middle");
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _104_SpiritOfMirrors(104, qn, "");
	}
}
