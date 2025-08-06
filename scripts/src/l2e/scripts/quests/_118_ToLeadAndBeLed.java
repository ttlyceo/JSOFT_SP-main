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
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Created by LordWinter 13.10.2012 Based on L2J Eternity-World
 */
public class _118_ToLeadAndBeLed extends Quest
{
	private static final String qn = "_118_ToLeadAndBeLed";

	private static final int PINTER = 30298;
	private static final int MAILLE_LIZARDMAN = 20919;
	private static final int MAILLE_LIZARDMAN_SCOUT = 20920;
	private static final int MAILLE_LIZARDMAN_GUARD = 20921;
	private static final int BLOOD_OF_MAILLE_LIZARDMAN = 8062;
	private static final int KING_OF_THE_ARANEID = 20927;
	private static final int KING_OF_THE_ARANEID_LEG = 8063;
	private static final int D_CRY = 1458;
	private static final int D_CRY_COUNT_HEAVY = 721;
	private static final int D_CRY_COUNT_LIGHT_MAGIC = 604;

	private static final int CLAN_OATH_HELM = 7850;

	private static final int CLAN_OATH_ARMOR = 7851;
	private static final int CLAN_OATH_GAUNTLETS = 7852;
	private static final int CLAN_OATH_SABATON = 7853;

	private static final int CLAN_OATH_BRIGANDINE = 7854;
	private static final int CLAN_OATH_LEATHER_GLOVES = 7855;
	private static final int CLAN_OATH_BOOTS = 7856;

	private static final int CLAN_OATH_AKETON = 7857;
	private static final int CLAN_OATH_PADDED_GLOVES = 7858;
	private static final int CLAN_OATH_SANDALS = 7859;

	public _118_ToLeadAndBeLed(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(PINTER);
		addTalkId(PINTER);

		addKillId(MAILLE_LIZARDMAN, MAILLE_LIZARDMAN_SCOUT, MAILLE_LIZARDMAN_GUARD);
		addKillId(KING_OF_THE_ARANEID);

		questItemIds = new int[]
		{
			BLOOD_OF_MAILLE_LIZARDMAN,
			KING_OF_THE_ARANEID_LEG
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30298-02.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30298-05a.htm"))
		{
			if (st.isCond(2))
			{
				st.set("choose", "1");
				st.set("cond", "3");
			}
		}
		else if (event.equalsIgnoreCase("30298-05b.htm"))
		{
			if (st.isCond(3))
			{
				st.set("choose", "2");
				st.set("cond", "4");
			}
		}
		else if (event.equalsIgnoreCase("30298-05c.htm"))
		{
			if (st.isCond(4))
			{
				st.set("choose", "3");
				st.set("cond", "5");
			}
		}
		else if (event.equalsIgnoreCase("30298-08.htm"))
		{
			final int choose = st.getInt("choose");
			final int need_dcry = choose == 1 ? D_CRY_COUNT_HEAVY : D_CRY_COUNT_LIGHT_MAGIC;
			if (st.getQuestItemsCount(D_CRY) >= need_dcry)
			{
				st.takeItems(D_CRY, need_dcry);
				st.set("cond", "7");
				st.playSound("ItemSound.quest_middle");
			} else {
				htmltext = "30298-07.htm";
			}
		}
		return htmltext;
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		final int cond = st.getCond();

		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() < 19)
				{
					st.exitQuest(true);
					htmltext = "30298-00.htm";
				}
				else if (player.getClan() == null || player.getSponsor() == 0)
				{
					st.exitQuest(true);
					htmltext = "30298-00a.htm";
				}
				else if (player.getSponsor() == 0)
				{
					st.exitQuest(true);
					htmltext = "30298-00b.htm";
				}
				else
				{
					htmltext = "30298-01.htm";
				}
				break;
			case State.STARTED:
				if (cond == 1)
				{
					htmltext = "30298-02a.htm";
				}
				else if (cond == 2)
				{
					if (st.getQuestItemsCount(BLOOD_OF_MAILLE_LIZARDMAN) < 10)
					{
						htmltext = "30298-02a.htm";
					}
					st.takeItems(BLOOD_OF_MAILLE_LIZARDMAN, -1);
					htmltext = "30298-04.htm";
				}
				else if (cond == 3)
				{
					htmltext = "30298-05a.htm";
				}
				else if (cond == 4)
				{
					htmltext = "30298-05b.htm";
				}
				else if (cond == 5)
				{
					htmltext = "30298-05c.htm";
				}
				else if (cond == 7)
				{
					htmltext = "30298-08a.htm";
				}
				else if (cond == 8)
				{
					if (st.getQuestItemsCount(KING_OF_THE_ARANEID_LEG) < 8)
					{
						st.set("cond", "7");
						htmltext = "30298-08a.htm";
					}
					st.takeItems(KING_OF_THE_ARANEID_LEG, -1);
					st.giveItems(CLAN_OATH_HELM, 1);
					final int choose = st.getInt("choose");
					if (choose == 1)
					{
						st.giveItems(CLAN_OATH_ARMOR, 1);
						st.giveItems(CLAN_OATH_GAUNTLETS, 1);
						st.giveItems(CLAN_OATH_SABATON, 1);
					}
					else if (choose == 2)
					{
						st.giveItems(CLAN_OATH_BRIGANDINE, 1);
						st.giveItems(CLAN_OATH_LEATHER_GLOVES, 1);
						st.giveItems(CLAN_OATH_BOOTS, 1);
					}
					else
					{
						st.giveItems(CLAN_OATH_AKETON, 1);
						st.giveItems(CLAN_OATH_PADDED_GLOVES, 1);
						st.giveItems(CLAN_OATH_SANDALS, 1);
					}
					st.unset("cond");
					st.playSound("ItemSound.quest_finish");
					st.exitQuest(false);
					htmltext = "30298-09.htm";
				}
				break;
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

		final int sponsor = player.getSponsor();
		if (sponsor == 0)
		{
			st.exitQuest(true);
			return null;
		}

		final int npcId = npc.getId();
		final int cond = st.getCond();
		switch (npcId)
		{
			case MAILLE_LIZARDMAN:
			case MAILLE_LIZARDMAN_SCOUT:
			case MAILLE_LIZARDMAN_GUARD:
				if ((cond == 1) && (st.getQuestItemsCount(BLOOD_OF_MAILLE_LIZARDMAN) < 10) && Rnd.chance(50))
				{
					st.giveItems(BLOOD_OF_MAILLE_LIZARDMAN, 1);
					if (st.getQuestItemsCount(BLOOD_OF_MAILLE_LIZARDMAN) == 10)
					{
						st.playSound("ItemSound.quest_middle");
						st.set("cond", "2");
					}
					else
					{
						st.playSound("ItemSound.quest_itemget");
					}
				}
				break;
			case KING_OF_THE_ARANEID:
				if ((cond == 7) && (st.getQuestItemsCount(KING_OF_THE_ARANEID_LEG) < 8) && Rnd.chance(50))
				{
					st.giveItems(KING_OF_THE_ARANEID_LEG, 1);
					if (st.getQuestItemsCount(KING_OF_THE_ARANEID_LEG) == 8)
					{
						st.playSound("ItemSound.quest_middle");
						st.set("cond", "8");
					}
					else
					{
						st.playSound("ItemSound.quest_itemget");
					}
				}
				break;
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _118_ToLeadAndBeLed(118, qn, "");
	}
}
