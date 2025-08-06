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
 * Created by LordWinter 26.09.2012
 * Based on L2J Eternity-World
 */
public class _413_PathToShillienOracle extends Quest
{
	private static final String qn = "_413_PathToShillienOracle";
	
	// Npcs
	private static final int SIDRA = 30330;
	private static final int ADONIUS = 30375;
	private static final int TALBOT = 30377;
	
	private static final int[] TALKERS =
	{
		SIDRA,
		ADONIUS,
		TALBOT
	};
	
	// Mobs
	private static final int ZOMBIE_SOLDIER = 20457;
	private static final int ZOMBIE_WARRIOR = 20458;
	private static final int SHIELD_SKELETON = 20514;
	private static final int SKELETON_INFANTRYMAN = 20515;
	private static final int DARK_SUCCUBUS = 20776;
	
	private static final int[] KILLS =
	{
		ZOMBIE_SOLDIER,
		ZOMBIE_WARRIOR,
		SHIELD_SKELETON,
		SKELETON_INFANTRYMAN,
		DARK_SUCCUBUS
	};
	
	// Quest Items
	private static final int SIDRAS_LETTER1 = 1262;
	private static final int BLANK_SHEET1 = 1263;
	private static final int BLOODY_RUNE1 = 1264;
	private static final int GARMIEL_BOOK = 1265;
	private static final int PRAYER_OF_ADON = 1266;
	private static final int PENITENTS_MARK = 1267;
	private static final int ASHEN_BONES = 1268;
	private static final int ANDARIEL_BOOK = 1269;
	
	private static final int[] QUESTITEMS =
	{
		SIDRAS_LETTER1,
		BLANK_SHEET1,
		BLOODY_RUNE1,
		GARMIEL_BOOK,
		PRAYER_OF_ADON,
		PENITENTS_MARK,
		ASHEN_BONES,
		ANDARIEL_BOOK
	};
	
	// Reward
	private static final int ORB_OF_ABYSS = 1270;
	
	public _413_PathToShillienOracle(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(SIDRA);
		
		for (final int talkId : TALKERS)
		{
			addTalkId(talkId);
		}
		
		for (final int killId : KILLS)
		{
			addKillId(killId);
		}
		
		questItemIds = QUESTITEMS;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		
		if (st == null || st.isCompleted())
		{
			return super.onAdvEvent(event, npc, player);
		}
		
		final int level = player.getLevel();
		final int classId = player.getClassId().getId();
		if (event.equalsIgnoreCase("1"))
		{
			st.set("id", "0");
			st.set("cond", "1");
			st.setState(State.STARTED);
			st.playSound("ItemSound.quest_accept");
			st.giveItems(SIDRAS_LETTER1, 1);
			htmltext = "30330-06.htm";
		}
		else if (event.equalsIgnoreCase("413_1"))
		{
			if ((level >= 18) && (classId == 0x26) && (st.getQuestItemsCount(ORB_OF_ABYSS) == 0))
			{
				htmltext = "30330-05.htm";
			}
			else if (classId != 0x26)
			{
				htmltext = classId == 0x2a ? "30330-02a.htm" : "30330-03.htm";
			}
			else if ((level < 18) && (classId == 0x26))
			{
				htmltext = "30330-02.htm";
			}
			else if ((level >= 18) && (classId == 0x26) && (st.getQuestItemsCount(ORB_OF_ABYSS) == 1))
			{
				htmltext = "30330-04.htm";
			}
		}
		else if (event.equalsIgnoreCase("30377_1"))
		{
			st.takeItems(SIDRAS_LETTER1, 1);
			st.giveItems(BLANK_SHEET1, 5);
			st.set("cond", "2");
			htmltext = "30377-02.htm";
		}
		else if (event.equalsIgnoreCase("30375_1"))
		{
			htmltext = "30375-02.htm";
		}
		else if (event.equalsIgnoreCase("30375_2"))
		{
			htmltext = "30375-03.htm";
		}
		else if (event.equalsIgnoreCase("30375_3"))
		{
			st.takeItems(PRAYER_OF_ADON, 1);
			st.giveItems(PENITENTS_MARK, 1);
			st.set("cond", "5");
			htmltext = "30375-04.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		String htmltext = Quest.getNoQuestMsg(talker);
		final QuestState st = talker.getQuestState(qn);
		
		if (st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		final int id = st.getState();
		final int cond = st.getInt("cond");
		if ((npcId != SIDRA) && (id != State.STARTED))
		{
			return htmltext;
		}
		
		if ((npcId == SIDRA) && (cond == 0))
		{
			htmltext = "30330-01.htm";
		}
		else if ((npcId == SIDRA) && (cond > 0))
		{
			if (st.getQuestItemsCount(SIDRAS_LETTER1) == 1)
			{
				htmltext = "30330-07.htm";
			}
			else if ((st.getQuestItemsCount(BLANK_SHEET1) > 0) || (st.getQuestItemsCount(BLOODY_RUNE1) == 1))
			{
				htmltext = "30330-08.htm";
			}
			else if ((st.getQuestItemsCount(ANDARIEL_BOOK) == 0) && ((st.getQuestItemsCount(PRAYER_OF_ADON) + st.getQuestItemsCount(GARMIEL_BOOK) + st.getQuestItemsCount(PENITENTS_MARK) + st.getQuestItemsCount(ASHEN_BONES)) > 0))
			{
				htmltext = "30330-09.htm";
			}
			else if ((st.getQuestItemsCount(ANDARIEL_BOOK) == 1) && (st.getQuestItemsCount(GARMIEL_BOOK) == 1))
			{
				st.takeItems(ANDARIEL_BOOK, 1);
				st.takeItems(GARMIEL_BOOK, 1);
				final String done = st.getGlobalQuestVar("1ClassQuestFinished");
				if (done == null || done.isEmpty())
				{
					st.addExpAndSp(228064, 16455);
					st.giveItems(57, 163800);
					st.saveGlobalQuestVar("1ClassQuestFinished", "1");
				}
				st.giveItems(ORB_OF_ABYSS, 1);
				st.exitQuest(false, true);
				htmltext = "30330-10.htm";
			}
		}
		else if ((npcId == TALBOT) && (cond > 0))
		{
			if (st.getQuestItemsCount(SIDRAS_LETTER1) == 1)
			{
				htmltext = "30377-01.htm";
			}
			else if ((st.getQuestItemsCount(BLANK_SHEET1) == 5) && (st.getQuestItemsCount(BLOODY_RUNE1) == 0))
			{
				htmltext = "30377-03.htm";
			}
			else if ((st.getQuestItemsCount(BLOODY_RUNE1) > 0) && (st.getQuestItemsCount(BLOODY_RUNE1) < 5))
			{
				htmltext = "30377-04.htm";
			}
			else if (st.getQuestItemsCount(BLOODY_RUNE1) >= 5)
			{
				st.takeItems(BLOODY_RUNE1, st.getQuestItemsCount(BLOODY_RUNE1));
				st.giveItems(GARMIEL_BOOK, 1);
				st.giveItems(PRAYER_OF_ADON, 1);
				st.set("cond", "4");
				htmltext = "30377-05.htm";
			}
			else if ((st.getQuestItemsCount(PRAYER_OF_ADON) + st.getQuestItemsCount(PENITENTS_MARK) + st.getQuestItemsCount(ASHEN_BONES)) > 0)
			{
				htmltext = "30377-06.htm";
			}
			else if ((st.getQuestItemsCount(ANDARIEL_BOOK) == 1) && (st.getQuestItemsCount(GARMIEL_BOOK) == 1))
			{
				htmltext = "30377-07.htm";
			}
		}
		else if ((npcId == ADONIUS) && (cond > 0))
		{
			if (st.getQuestItemsCount(PRAYER_OF_ADON) == 1)
			{
				htmltext = "30375-01.htm";
			}
			else if ((st.getQuestItemsCount(PENITENTS_MARK) == 1) && (st.getQuestItemsCount(ASHEN_BONES) == 0) && (st.getQuestItemsCount(ANDARIEL_BOOK) == 0))
			{
				htmltext = "30375-05.htm";
			}
			else if ((st.getQuestItemsCount(PENITENTS_MARK) == 1) && (st.getQuestItemsCount(ASHEN_BONES) < 10) && (st.getQuestItemsCount(ASHEN_BONES) > 0))
			{
				htmltext = "30375-06.htm";
			}
			else if ((st.getQuestItemsCount(PENITENTS_MARK) == 1) && (st.getQuestItemsCount(ASHEN_BONES) >= 10))
			{
				st.takeItems(ASHEN_BONES, st.getQuestItemsCount(ASHEN_BONES));
				st.takeItems(PENITENTS_MARK, st.getQuestItemsCount(PENITENTS_MARK));
				st.giveItems(ANDARIEL_BOOK, 1);
				st.set("cond", "7");
				htmltext = "30375-07.htm";
			}
			else if (st.getQuestItemsCount(ANDARIEL_BOOK) == 1)
			{
				htmltext = "30375-08.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState st = killer.getQuestState(qn);
		
		if (st == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		if (st.getState() != State.STARTED)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		final int npcId = npc.getId();
		final int cond = st.getInt("cond");
		if (npcId == 20776)
		{
			st.set("id", "0");
			if ((cond > 0) && (st.getQuestItemsCount(BLANK_SHEET1) > 0))
			{
				st.giveItems(BLOODY_RUNE1, 1);
				st.takeItems(BLANK_SHEET1, 1);
				if (st.getQuestItemsCount(BLANK_SHEET1) == 0)
				{
					st.playSound("ItemSound.quest_middle");
					st.set("cond", "3");
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		else if (npcId == 20514)
		{
			st.set("id", "0");
			if ((cond > 0) && (st.getQuestItemsCount(PENITENTS_MARK) == 1) && (st.getQuestItemsCount(ASHEN_BONES) < 10))
			{
				st.giveItems(ASHEN_BONES, 1);
				if (st.getQuestItemsCount(ASHEN_BONES) == 10)
				{
					st.playSound("ItemSound.quest_middle");
					st.set("cond", "6");
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		else if (npcId == 20515)
		{
			st.set("id", "0");
			if ((cond > 0) && (st.getQuestItemsCount(PENITENTS_MARK) == 1) && (st.getQuestItemsCount(ASHEN_BONES) < 10))
			{
				st.giveItems(ASHEN_BONES, 1);
				if (st.getQuestItemsCount(ASHEN_BONES) == 10)
				{
					st.playSound("ItemSound.quest_middle");
					st.set("cond", "6");
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		else if (npcId == 20457)
		{
			st.set("id", "0");
			if ((cond > 0) && (st.getQuestItemsCount(PENITENTS_MARK) == 1) && (st.getQuestItemsCount(ASHEN_BONES) < 10))
			{
				st.giveItems(ASHEN_BONES, 1);
				if (st.getQuestItemsCount(ASHEN_BONES) == 10)
				{
					st.playSound("ItemSound.quest_middle");
					st.set("cond", "6");
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		else if (npcId == 20458)
		{
			st.set("id", "0");
			if ((cond > 0) && (st.getQuestItemsCount(PENITENTS_MARK) == 1) && (st.getQuestItemsCount(ASHEN_BONES) < 10))
			{
				st.giveItems(ASHEN_BONES, 1);
				if (st.getQuestItemsCount(ASHEN_BONES) == 10)
				{
					st.playSound("ItemSound.quest_middle");
					st.set("cond", "6");
				}
				else
				{
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _413_PathToShillienOracle(413, qn, "");
	}
}