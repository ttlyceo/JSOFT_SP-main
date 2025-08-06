
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
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 06.12.2019
 */
public class _025_HidingBehindTheTruth extends Quest
{
	public _025_HidingBehindTheTruth(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31349);
		addTalkId(31348, 31349, 31533, 31534, 31535, 31522, 31532, 31531, 31536);
		
		addKillId(27218);
		
		questItemIds = new int[]
		{
		        7157, 7066, 7155, 7158, 7156
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		String htmltext = event;
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31349-02.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31349-03.htm"))
		{
			if (st.getQuestItemsCount(7156) >= 1)
			{
				htmltext = "31349-05.htm";
			}
			else
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31349-10.htm"))
		{
			st.setCond(4, true);
		}
		else if (event.equalsIgnoreCase("31348-02.htm"))
		{
			st.takeItems(7156, -1);
		}
		else if (event.equalsIgnoreCase("31348-07.htm"))
		{
			st.setCond(5, true);
			st.giveItems(7157, 1);
		}
		else if (event.equalsIgnoreCase("31522-04.htm"))
		{
			st.setCond(6, true);
		}
		else if (event.equalsIgnoreCase("31535-03.htm"))
		{
			if (st.getInt("step") == 0)
			{
				st.set("step", "1");
				final Npc triol = st.addSpawn(27218, 59712, -47568, -2712, 0, false, 300000, true);
				triol.broadcastPacketToOthers(2000, new NpcSay(triol.getObjectId(), 0, triol.getId(), NpcStringId.THAT_BOX_WAS_SEALED_BY_MY_MASTER_S1_DONT_TOUCH_IT));
				triol.setRunning();
				((Attackable) triol).addDamageHate(player, 0, 999);
				triol.getAI().setIntention(CtrlIntention.ATTACK, player);
				st.setCond(7, true);
			}
			else if (st.getInt("step") == 2)
			{
				htmltext = "31535-04.htm";
			}
		}
		else if (event.equalsIgnoreCase("31535-05.htm"))
		{
			if (st.getQuestItemsCount(7157) != 0)
			{
				st.giveItems(7066, 1);
				st.takeItems(7157, -1);
				st.setCond(9, true);
			}
		}
		else if (event.equalsIgnoreCase("31532-02.htm"))
		{
			st.takeItems(7066, -1);
		}
		else if (event.equalsIgnoreCase("31532-06.htm"))
		{
			st.setCond(11, true);
		}
		else if (event.equalsIgnoreCase("31531-02.htm"))
		{
			st.setCond(12, true);
			st.addSpawn(31536, 60104, -35820, -664, 0, false, 20000, true);
		}
		else if (event.equalsIgnoreCase("31532-18.htm"))
		{
			st.setCond(15, true);
		}
		else if (event.equalsIgnoreCase("31522-12.htm"))
		{
			st.setCond(16, true);
		}
		else if (event.equalsIgnoreCase("31348-10.htm"))
		{
			st.takeItems(7158, -1);
		}
		else if (event.equalsIgnoreCase("31348-15.htm"))
		{
			if (st.isCond(16))
			{
				st.setCond(17, true);
			}
		}
		else if (event.equalsIgnoreCase("31348-16.htm"))
		{
			if (st.isCond(16))
			{
				st.setCond(18, true);
			}
		}
		else if (event.equalsIgnoreCase("31532-20.htm"))
		{
			if (st.isCond(17))
			{
				st.takeItems(7063, -1);
				st.calcExpAndSp(getId());
				st.calcReward(getId(), 1);
				st.exitQuest(false, true);
			}
		}
		else if (event.equalsIgnoreCase("31522-15.htm"))
		{
			if (st.isCond(18))
			{
				st.takeItems(7063, -1);
				st.calcExpAndSp(getId());
				st.calcReward(getId(), 2);
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		final byte id = st.getState();

		if (id == State.COMPLETED)
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (id == State.CREATED)
		{
			if (npcId == 31349)
			{
				final QuestState st2 = player.getQuestState("_024_InhabitantsOfTheForrestOfTheDead");
				if (st2 != null)
				{
					if (st2.isCompleted() && (player.getLevel() >= getMinLvl(getId())))
					{
						htmltext = "31349-01.htm";
					}
					else
					{
						htmltext = "31349-00.htm";
					}
				}
				else
				{
					htmltext = "31349-00.htm";
				}
			}
		}
		else if (id == State.STARTED)
		{
			if (npcId == 31349)
			{
				if (cond == 1)
				{
					htmltext = "31349-02.htm";
				}
				else if ((cond == 2) || (cond == 3))
				{
					htmltext = "31349-04.htm";
				}
				else if (cond == 4)
				{
					htmltext = "31349-10.htm";
				}
			}
			else if (npcId == 31522)
			{
				if (cond == 2)
				{
					htmltext = "31522-01.htm";
					st.setCond(3, true);
					st.giveItems(7156, 1);
				}
				else if (cond == 3)
				{
					htmltext = "31522-02.htm";
				}
				else if (cond == 5)
				{
					htmltext = "31522-03.htm";
				}
				else if (cond == 6)
				{
					htmltext = "31522-04.htm";
				}
				else if (cond == 9)
				{
					htmltext = "31522-05.htm";
					st.setCond(10, true);
				}
				else if (cond == 10)
				{
					htmltext = "31522-05.htm";
				}
				else if (cond == 15)
				{
					htmltext = "31522-06.htm";
				}
				else if (cond == 16)
				{
					htmltext = "31522-13.htm";
				}
				else if (cond == 17)
				{
					htmltext = "31522-16.htm";
				}
				else if (cond == 18)
				{
					htmltext = "31522-14.htm";
				}
			}
			else if (npcId == 31348)
			{
				if (cond == 4)
				{
					htmltext = "31348-01.htm";
				}
				else if (cond == 5)
				{
					htmltext = "31348-08.htm";
				}
				else if (cond == 16)
				{
					htmltext = "31348-09.htm";
				}
				else if (cond == 17)
				{
					htmltext = "31348-17.htm";
				}
				else if (cond == 18)
				{
					htmltext = "31348-18.htm";
				}
			}
			else if (npcId == 31533)
			{
				if (cond == 6)
				{
					htmltext = "31533-01.htm";
				}
			}
			else if (npcId == 31534)
			{
				if (cond == 6)
				{
					htmltext = "31534-01.htm";
				}
			}
			else if (npcId == 31535)
			{
				if ((cond >= 6) && (cond <= 8))
				{
					htmltext = "31535-01.htm";
				}
				else if (cond == 9)
				{
					htmltext = "31535-06.htm";
				}
			}
			else if (npcId == 31532)
			{
				if (cond == 10)
				{
					htmltext = "31532-01.htm";
				}
				else if ((cond == 11) || (cond == 12))
				{
					htmltext = "31532-06.htm";
				}
				else if (cond == 13)
				{
					htmltext = "31532-07.htm";
					st.setCond(14, false);
					st.takeItems(7155, -1);
				}
				else if (cond == 14)
				{
					htmltext = "31532-08.htm";
				}
				else if (cond == 15)
				{
					htmltext = "31532-18.htm";
				}
				else if (cond == 17)
				{
					htmltext = "31532-19.htm";
				}
				else if (cond == 18)
				{
					htmltext = "31532-21.htm";
				}
			}
			else if (npcId == 31531)
			{
				if ((cond == 11) || (cond == 12))
				{
					htmltext = "31531-01.htm";
				}
				else if (cond == 13)
				{
					htmltext = "31531-03.htm";
				}
			}
			else if (npcId == 31536)
			{
				if (cond == 12)
				{
					htmltext = "31536-01.htm";
					st.giveItems(7155, 1);
					st.setCond(13, true);
					npc.deleteMe();
				}
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		if (st.isCond(7))
		{
			st.playSound("ItemSound.quest_itemget");
			st.setCond(8, false);
			npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), NpcStringId.YOUVE_ENDED_MY_IMMORTAL_LIFE_YOURE_PROTECTED_BY_THE_FEUDAL_LORD_ARENT_YOU));
			st.giveItems(7158, 1);
			st.set("step", "2");
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _025_HidingBehindTheTruth(25, _025_HidingBehindTheTruth.class.getSimpleName(), "");
	}
}