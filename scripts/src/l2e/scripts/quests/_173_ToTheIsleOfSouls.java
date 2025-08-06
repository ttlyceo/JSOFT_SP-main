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

public final class _173_ToTheIsleOfSouls extends Quest
{
	private static final String qn = "_173_ToTheIsleOfSouls";
	
	private static int GALLADUCCI = 30097;
	private static int GENTLER = 30094;
	
	private static int SCROLL_OF_ESCAPE_KAMAEL_VILLAGE = 9716;
	private static int MARK_OF_TRAVELER = 7570;
	private static int GWAINS_DOCUMENT = 7563;
	private static int MAGIC_SWORD_HILT = 7568;
	
	public _173_ToTheIsleOfSouls(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(GALLADUCCI);
		addTalkId(GALLADUCCI);
		addTalkId(GENTLER);
		
		questItemIds = new int[]
		{
		        GWAINS_DOCUMENT, MAGIC_SWORD_HILT
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
		
		if (event.equalsIgnoreCase("30097-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.giveItems(GWAINS_DOCUMENT, 1);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30094-02.htm"))
		{
			if (st.isCond(1))
			{
				st.set("cond", "2");
				st.takeItems(GWAINS_DOCUMENT, -1);
				st.giveItems(MAGIC_SWORD_HILT, 1);
				st.playSound("ItemSound.quest_middle");
			}
		}
		else if (event.equalsIgnoreCase("30097-06.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(MAGIC_SWORD_HILT, -1);
				st.takeItems(MARK_OF_TRAVELER, -1);
				st.giveItems(SCROLL_OF_ESCAPE_KAMAEL_VILLAGE, 1);
				st.exitQuest(false, true);
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
		
		final int cond = st.getInt("cond");
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.CREATED :
				if (npcId == GALLADUCCI)
				{
					if ((st.getQuestItemsCount(MARK_OF_TRAVELER) > 0L) && (player.getRace().ordinal() == 5))
					{
						htmltext = "30097-02.htm";
					}
					else
					{
						htmltext = "30097-01.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED :
				if (npcId == GALLADUCCI)
				{
					if (cond == 1)
					{
						htmltext = "30097-04.htm";
					}
					else if (cond == 2)
					{
						htmltext = "30097-05.htm";
					}
				}
				else if (npcId == GENTLER)
				{
					if (cond == 1)
					{
						htmltext = "30094-01.htm";
					}
					else if (cond == 2)
					{
						htmltext = "30094-03.htm";
					}
				}
				break;
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _173_ToTheIsleOfSouls(173, qn, "");
	}
}