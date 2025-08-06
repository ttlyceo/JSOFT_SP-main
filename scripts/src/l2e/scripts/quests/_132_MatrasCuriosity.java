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

import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Based on L2J Eternity-World
 */
public class _132_MatrasCuriosity extends Quest
{
	private static String qn = "_132_MatrasCuriosity";
	
	// NPC's
	private final int MATRAS = 32245;
	private final int DEMONPRINCE = 25540;
	private final int RANKU = 25542;
	
	// Item's
	private final int RANKUSBLUEPRINT = 9800;
	private final int PRINCESBLUEPRINT = 9801;
	private final int ROUGHOREOFFIRE = 10521;
	private final int ROUGHOREOFWATER = 10522;
	private final int ROUGHOREOFTHEEARTH = 10523;
	private final int ROUGHOREOFWIND = 10524;
	private final int ROUGHOREOFDARKNESS = 10525;
	private final int ROUGHOREOFDIVINITY = 10526;
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32245-02.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.playSound("ItemSound.quest_accept");
				st.setState(State.STARTED);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		final int npcId = npc.getId();
		final byte id = st.getState();
		final int cond = st.getInt("cond");
		
		if (id == State.COMPLETED)
		{
			if (npcId == MATRAS)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		else if (id == State.CREATED)
		{
			if ((npcId == MATRAS) && (cond == 0))
			{
				if (player.getLevel() >= 76)
				{
					htmltext = "32245-01.htm";
				}
				else
				{
					htmltext = "32245-00.htm";
					st.exitQuest(true);
				}
			}
		}
		else if (id == State.STARTED)
		{
			if (npcId == MATRAS)
			{
				if (cond == 1)
				{
					if ((st.getQuestItemsCount(PRINCESBLUEPRINT) == 1) && (st.getQuestItemsCount(RANKUSBLUEPRINT) == 1))
					{
						st.set("cond", "2");
						st.playSound("ItemSound.quest_middle");
					}
					else
					{
						htmltext = "32245-03.htm";
					}
				}
				else if (cond == 2)
				{
					st.takeItems(RANKUSBLUEPRINT, -1);
					st.takeItems(PRINCESBLUEPRINT, -1);
					st.set("cond", "3");
					st.playSound("ItemSound.quest_middle");
					htmltext = "32245-04.htm";
				}
				else if (cond == 3)
				{
					st.giveItems(ROUGHOREOFFIRE, 1);
					st.giveItems(ROUGHOREOFWATER, 1);
					st.giveItems(ROUGHOREOFTHEEARTH, 1);
					st.giveItems(ROUGHOREOFWIND, 1);
					st.giveItems(ROUGHOREOFDARKNESS, 1);
					st.giveItems(ROUGHOREOFDIVINITY, 1);
					st.giveItems(57, 65884);
					st.addExpAndSp(50541, 5094);
					st.unset("cond");
					st.exitQuest(false);
					st.playSound("ItemSound.quest_finish");
					htmltext = "32245-05.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final String htmltext = null;
		final int npcId = npc.getId();
		if ((npcId == DEMONPRINCE) || (npcId == RANKU))
		{
			final Party party = player.getParty();
			if (party != null)
			{
				for (final Player member : party.getMembers())
				{
					final QuestState st = member.getQuestState(qn);
					if ((st != null) && (st.getState() == State.STARTED))
					{
						if ((npcId == DEMONPRINCE) && (st.getQuestItemsCount(PRINCESBLUEPRINT) == 0))
						{
							st.giveItems(PRINCESBLUEPRINT, 1);
						}
						else if ((npcId == RANKU) && (st.getQuestItemsCount(RANKUSBLUEPRINT) == 0))
						{
							st.giveItems(RANKUSBLUEPRINT, 1);
						}
						st.playSound("ItemSound.quest_itemget");
						if ((st.getQuestItemsCount(PRINCESBLUEPRINT) > 0) && (st.getQuestItemsCount(RANKUSBLUEPRINT) > 0))
						{
							st.set("cond", "2");
							st.playSound("ItemSound.quest_middle");
						}
					}
				}
			}
			else
			{
				final QuestState st = player.getQuestState(qn);
				if ((st != null) && (st.getState() == State.STARTED))
				{
					if ((npcId == DEMONPRINCE) && (st.getQuestItemsCount(PRINCESBLUEPRINT) == 0))
					{
						st.giveItems(PRINCESBLUEPRINT, 1);
					}
					else if ((npcId == RANKU) && (st.getQuestItemsCount(RANKUSBLUEPRINT) == 0))
					{
						st.giveItems(RANKUSBLUEPRINT, 1);
					}
					st.playSound("ItemSound.quest_itemget");
					if ((st.getQuestItemsCount(PRINCESBLUEPRINT) > 0) && (st.getQuestItemsCount(RANKUSBLUEPRINT) > 0))
					{
						st.set("cond", "2");
						st.playSound("ItemSound.quest_middle");
					}
				}
			}
		}
		return htmltext;
	}
	
	public _132_MatrasCuriosity(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addTalkId(MATRAS);
		addStartNpc(MATRAS);
		addKillId(DEMONPRINCE);
		addKillId(RANKU);
	}
	
	public static void main(String args[])
	{
		new _132_MatrasCuriosity(132, qn, "");
	}
}
