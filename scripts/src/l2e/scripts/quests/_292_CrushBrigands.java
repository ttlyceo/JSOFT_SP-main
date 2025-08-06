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
 * Created by LordWinter 01.01.2013
 * Based on L2J Eternity-World
 */
public class _292_CrushBrigands extends Quest
{
	private static final String qn = "_292_CrushBrigands";
	
	private static final int GOBLIN_NECKLACE = 1483;
	private static final int GOBLIN_PENDANT = 1484;
	private static final int GOBLIN_LORD_PENDANT = 1485;
	private static final int SUSPICIOUS_MEMO = 1486;
	private static final int SUSPICIOUS_CONTRACT = 1487;
	
	public _292_CrushBrigands(int scriptId, String name, String descr)
	{
		super(scriptId, name, descr);
		
		addStartNpc(30532);
		addTalkId(30532, 30533);
		
		addKillId(20322, 20323, 20324, 20327, 20528);
		
		questItemIds = new int[]
		{
		        GOBLIN_NECKLACE, GOBLIN_PENDANT, GOBLIN_LORD_PENDANT, SUSPICIOUS_CONTRACT, SUSPICIOUS_MEMO
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30532-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30532-06.htm"))
		{
			st.takeItems(SUSPICIOUS_MEMO, -1);
			st.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg(player);
		if (st == null)
    		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.CREATED :
				if (npcId == 30532)
				{
					if (player.getRace().ordinal() != 4)
					{
						htmltext = "30532-00.htm";
						st.exitQuest(true);
					}
					else
					{
						if (player.getLevel() >= 5)
						{
							htmltext = "30532-02.htm";
							return htmltext;
						}
						htmltext = "30532-01.htm";
						st.exitQuest(true);
					}
          				}
				break;
			case State.STARTED :
				if (npcId == 30532)
          				{
					final long neckl = st.getQuestItemsCount(GOBLIN_NECKLACE);
					final long penda = st.getQuestItemsCount(GOBLIN_PENDANT);
					final long lordp = st.getQuestItemsCount(GOBLIN_LORD_PENDANT);
					final long smemo = st.getQuestItemsCount(SUSPICIOUS_MEMO);
					final long scont = st.getQuestItemsCount(SUSPICIOUS_CONTRACT);
					if ((neckl == 0) || (penda == 0) || (lordp == 0) || (smemo == 0) || (scont == 0))
					{
						htmltext = "30532-04.htm";
					}
					else
					{
						st.takeItems(GOBLIN_NECKLACE, -1);
						st.takeItems(GOBLIN_PENDANT, -1);
						st.takeItems(GOBLIN_LORD_PENDANT, -1);
						if (scont == 0)
						{
							if (smemo == 1)
							{
								htmltext = "30532-08.htm";
							}
							else if (smemo >= 2)
							{
								htmltext = "30532-09.htm";
							}
							else
							{
								htmltext = "30532-05.htm";
							}
						}
						else
						{
							htmltext = "30532-10.htm";
							st.takeItems(SUSPICIOUS_CONTRACT, -1);
						}
						st.giveItems(57, 12 * neckl + 36 * penda + 33 * lordp + 100 * scont);
					}
          				}
				else if (npcId == 30533)
				{
					if (st.getQuestItemsCount(SUSPICIOUS_CONTRACT) == 0)
					{
						htmltext = "30533-01.htm";
					}
					else
					{
						htmltext = "30533-02.htm";
						st.giveItems(57, st.getQuestItemsCount(SUSPICIOUS_CONTRACT) * 120);
						st.takeItems(SUSPICIOUS_CONTRACT, -1);
					}
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			int item = 0;
			final int npcId = npc.getId();
			
			if ((npcId == 20322) || (npcId == 20323))
			{
				item = GOBLIN_NECKLACE;
			}
			
			if ((npcId == 20324) || (npcId == 20327))
			{
				item = GOBLIN_PENDANT;
			}
			
			if (npcId == 20528)
			{
				item = GOBLIN_LORD_PENDANT;
			}
			
			final int n = getRandom(10);
			if (n > 5)
			{
				st.giveItems(item, 1);
				st.playSound("ItemSound.quest_itemget");
			}
			else if (n > 4)
			{
				if (st.getQuestItemsCount(SUSPICIOUS_CONTRACT) == 0)
				{
					if (st.getQuestItemsCount(SUSPICIOUS_MEMO) < 3)
					{
						st.giveItems(SUSPICIOUS_MEMO, 1);
						st.playSound("ItemSound.quest_itemget");
					}
					else
					{
						st.giveItems(SUSPICIOUS_CONTRACT, 1);
						st.takeItems(SUSPICIOUS_MEMO, -1);
						st.playSound("ItemSound.quest_middle");
						st.set("cond", "2");
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _292_CrushBrigands(292, qn, "");
	}
}