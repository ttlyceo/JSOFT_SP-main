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

import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;

/**
 * Created by LordWinter 06.08.2011
 * Based on L2J Eternity-World
 */
public class _131_BirdInACage extends Quest
{
	private static final String qn = "_131_BirdInACage";
	
	// NPC's
	private static final int KANIS = 32264;
	private static final int PARME = 32271;
	
	// MOBS
	private static final int GIFTBOX = 32342;
	
	// ITEMS
	private static final int[][] GIFTBOXITEMS =
	{
	        {
	                9692, 100, 2
			},
	        {
	                9693, 50, 1
			}
	};
	private static final int KANIS_ECHO_CRY = 9783;
	private static final int PARMES_LETTER = 9784;
	
	// OTHER
	private static final int KISSOFEVA = 1073;
	
	public _131_BirdInACage(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(KANIS);
		addTalkId(KANIS);
		addTalkId(PARME);
		addKillId(GIFTBOX);
		addSpawnId(GIFTBOX);
		
		questItemIds = new int[]
		{
		        KANIS_ECHO_CRY, PARMES_LETTER
		};
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.setIsNoRndWalk(true);
		return super.onSpawn(npc);
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
		
		st.getInt("cond");
		
		if (event.equalsIgnoreCase("32264-02.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32264-08.htm"))
		{
			if (st.isCond(1))
			{
				st.set("cond", "2");
				st.playSound("ItemSound.quest_middle");
				st.giveItems(KANIS_ECHO_CRY, 1);
			}
		}
		else if (event.equalsIgnoreCase("32271-03.htm"))
		{
			if (st.isCond(2))
			{
				st.set("cond", "3");
				st.playSound("ItemSound.quest_middle");
				st.giveItems(PARMES_LETTER, 1);
				player.teleToLocation(143472 + getRandom(-100, 100), 191040 + getRandom(-100, 100), -3696, true, ReflectionManager.DEFAULT);
			}
		}
		else if (event.equalsIgnoreCase("32264-12.htm") && st.isCond(3))
		{
			st.playSound("ItemSound.quest_middle");
			st.takeItems(PARMES_LETTER, -1);
		}
		else if (event.equalsIgnoreCase("32264-13.htm") && st.isCond(3))
		{
			HellboundManager.getInstance().unlock();
			st.playSound("ItemSound.quest_finish");
			st.takeItems(KANIS_ECHO_CRY, -1);
			st.addExpAndSp(1304752, 25019);
			st.exitQuest(false);
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
		
		final int npcId = npc.getId();
		final int cond = st.getInt("cond");
		
		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (npcId == KANIS)
		{
			if (cond == 0)
			{
				if (player.getLevel() >= 78)
				{
					htmltext = "32264-01.htm";
				}
				else
				{
					htmltext = "32264-00.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "32264-03.htm";
			}
			else if (cond == 2)
			{
				htmltext = "32264-08a.htm";
			}
			else if (cond == 3)
			{
				if (st.getQuestItemsCount(PARMES_LETTER) > 0)
				{
					htmltext = "32264-11.htm";
				}
				else
				{
					htmltext = "32264-12.htm";
				}
			}
		}
		else if (npcId == PARME && cond == 2)
		{
			htmltext = "32271-01.htm";
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState st = killer.getQuestState(qn);
		if (st == null)
		{
			return null;
		}
		
		if (npc.getId() == GIFTBOX)
		{
			if (killer.getFirstEffect(KISSOFEVA) != null)
			{
				for (final int[] GIFTBOXITEM : GIFTBOXITEMS)
				{
					if (getRandom(100) < GIFTBOXITEM[1])
					{
						st.giveItems(GIFTBOXITEM[0], 1);
					}
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _131_BirdInACage(131, qn, "");
	}
}