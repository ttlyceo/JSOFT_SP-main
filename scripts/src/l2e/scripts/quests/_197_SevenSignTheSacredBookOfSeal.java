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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Updated by LordWinter 03.10.2011 Based on L2J Eternity-World
 */
public class _197_SevenSignTheSacredBookOfSeal extends Quest
{
	private static final String qn = "_197_SevenSignTheSacredBookOfSeal";
	
	// NPCs
	private static final int WOOD = 32593;
	private static final int ORVEN = 30857;
	private static final int LEOPARD = 32594;
	private static final int LAWRENCE = 32595;
	private static final int SOFIA = 32596;
	
	// MOB
	private static final int SHILENSEVIL = 27396;
	
	// ITEMS
	private static final int TEXT = 13829;
	private static final int SCULPTURE = 14355;
	
	public _197_SevenSignTheSacredBookOfSeal(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(WOOD);
		addTalkId(WOOD);
		addTalkId(ORVEN);
		addTalkId(LEOPARD);
		addTalkId(LAWRENCE);
		addTalkId(SOFIA);
		addKillId(SHILENSEVIL);
		
		questItemIds = new int[]
		{
		        TEXT, SCULPTURE
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
		
		if (npc.getId() == WOOD)
		{
			if (event.equalsIgnoreCase("32593-04.htm"))
			{
				final QuestState qs = player.getQuestState("_196_SevenSignSealOfTheEmperor");
				if ((qs != null && qs.isCompleted()) && (player.getLevel() >= 79))
				{
					if (st.isCreated())
					{
						st.setState(State.STARTED);
						st.set("cond", "1");
						st.playSound("ItemSound.quest_accept");
					}
				}
			}
			else if (event.equalsIgnoreCase("32593-08.htm") && st.isCond(6))
			{
				st.takeItems(TEXT, 1);
				st.takeItems(SCULPTURE, 1);
				st.addExpAndSp(25000000, 2500000);
				st.exitQuest(false, true);
			}
		}
		else if (npc.getId() == ORVEN)
		{
			if (event.equalsIgnoreCase("30857-04.htm") && st.isCond(1))
			{
				st.set("cond", "2");
				st.playSound("ItemSound.quest_middle");
			}
		}
		else if (npc.getId() == LEOPARD)
		{
			if (event.equalsIgnoreCase("32594-03.htm") && st.isCond(2))
			{
				st.set("cond", "3");
				st.playSound("ItemSound.quest_middle");
			}
		}
		else if (npc.getId() == LAWRENCE)
		{
			if (event.equalsIgnoreCase("32595-04.htm"))
			{
				if (st.getLong("ShilensevilOnSpawn") < System.currentTimeMillis())
				{
					final MonsterInstance monster = (MonsterInstance) addSpawn(SHILENSEVIL, 152520, -57486, -3430, 0, false, 300000, true);
					monster.broadcastPacketToOthers(2000, new NpcSay(monster.getObjectId(), 0, monster.getId(), NpcStringId.YOU_ARE_NOT_THE_OWNER_OF_THAT_ITEM));
					monster.setRunning();
					monster.addDamageHate(player, 0, 999);
					monster.getAI().setIntention(CtrlIntention.ATTACK, player);
					st.set("ShilensevilOnSpawn", (System.currentTimeMillis() + 300000));
				}
			}
			else if (event.equalsIgnoreCase("32595-08.htm") && st.isCond(4))
			{
				st.set("cond", "5");
				st.playSound("ItemSound.quest_middle");
			}
		}
		else if (npc.getId() == SOFIA)
		{
			if (event.equalsIgnoreCase("32596-04.htm") && st.isCond(5))
			{
				st.set("cond", "6");
				st.giveItems(TEXT, 1);
				st.playSound("ItemSound.quest_middle");
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
		
		if (npc.getId() == WOOD)
		{
			switch (st.getState())
			{
				case State.CREATED :
					final QuestState qs = player.getQuestState("_196_SevenSignSealOfTheEmperor");
					if ((qs != null && qs.isCompleted()) && (player.getLevel() >= 79))
					{
						htmltext = "32593-01.htm";
					}
					else
					{
						htmltext = "32593-00.htm";
						st.exitQuest(true);
					}
					break;
				
				case State.STARTED :
					if ((st.getInt("cond") >= 1) && (st.getInt("cond") <= 5))
					{
						htmltext = "32593-05.htm";
					}
					else if (st.getInt("cond") == 6)
					{
						htmltext = "32593-06.htm";
					}
					break;
			}
		}
		else if (npc.getId() == ORVEN)
		{
			if (st.getState() == State.STARTED)
			{
				if (st.getInt("cond") == 1)
				{
					htmltext = "30857-01.htm";
				}
				else if (st.getInt("cond") >= 2)
				{
					htmltext = "30857-05.htm";
				}
			}
		}
		else if (npc.getId() == LEOPARD)
		{
			if (st.getState() == State.STARTED)
			{
				if (st.getInt("cond") == 2)
				{
					htmltext = "32594-01.htm";
				}
				else if (st.getInt("cond") >= 3)
				{
					htmltext = "32594-04.htm";
				}
			}
		}
		else if (npc.getId() == LAWRENCE)
		{
			if (st.getState() == State.STARTED)
			{
				if (st.getInt("cond") == 3)
				{
					htmltext = "32595-01.htm";
				}
				else if (st.getInt("cond") == 4)
				{
					htmltext = "32595-05.htm";
				}
				else if (st.getInt("cond") >= 5)
				{
					htmltext = "32595-09.htm";
				}
			}
		}
		else if (npc.getId() == SOFIA)
		{
			if (st.getState() == State.STARTED)
			{
				if (st.getInt("cond") == 5)
				{
					htmltext = "32596-01.htm";
				}
				else if (st.getInt("cond") == 6)
				{
					htmltext = "32596-05.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 3);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), NpcStringId.S1_YOU_MAY_HAVE_WON_THIS_TIME_BUT_NEXT_TIME_I_WILL_SURELY_CAPTURE_YOU));
			st.giveItems(SCULPTURE, 1);
			st.set("cond", "4");
			st.playSound("ItemSound.quest_middle");
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _197_SevenSignTheSacredBookOfSeal(197, qn, "");
	}
}
