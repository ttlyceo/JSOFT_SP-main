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
 * Rework by LordWinter 13.03.2023
 */
public class _142_FallenAngelRequestOfDawn extends Quest
{
	public _142_FallenAngelRequestOfDawn(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addTalkId(30894, 30289, 30612, 32368);
		
		addKillId(20079, 20080, 20081, 20082, 20084, 20086, 20087, 20088, 20089, 20090, 27338);
		
		questItemIds = new int[]
		{
		        10351, 10352, 10353
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30894-01.htm") && npc.getId() == 30894)
		{
			st.setCond(1, true);
		}
		else if (event.equalsIgnoreCase("30894-03.htm") && npc.getId() == 30894)
		{
			if (st.isCond(1))
			{
				st.giveItems(10351, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30289-04.htm") && npc.getId() == 30289)
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("30612-07.htm") && npc.getId() == 30612)
		{
			if (st.isCond(3))
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("32368-02.htm") && npc.getId() == 32368)
		{
			if (st.getLong("isAngelSpawned") < System.currentTimeMillis())
			{
				addSpawn(27338, -21882, 186730, -4320, 0, false, 900000);
				st.set("isAngelSpawned", (System.currentTimeMillis() + 900000));
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		final int cond = st.getCond();
		final int npcId = npc.getId();
		final int id = st.getState();
		
		if (id == State.CREATED)
		{
			return htmltext;
		}
		
		if (id == State.COMPLETED)
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (npcId == 30894)
		{
			if (cond == 1)
			{
				htmltext = "30894-01.htm";
			}
			else if (cond == 2)
			{
				htmltext = "30894-04.htm";
			}
		}
		else if (npcId == 30289)
		{
			if (cond == 2)
			{
				if (st.getInt("talk") == 1)
				{
					htmltext = "30289-02.htm";
				}
				else
				{
					htmltext = "30289-01.htm";
					st.takeItems(10351, -1);
					st.set("talk", "1");
				}
			}
			else if (cond == 3)
			{
				htmltext = "30289-05.htm";
			}
			else if (cond == 6)
			{
				htmltext = "30289-06.htm";
				st.takeItems(10353, -1);
				if ((player.getLevel() >= 38) && (player.getLevel() <= 43))
				{
					st.calcExpAndSp(getId());
				}
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		else if (npcId == 30612)
		{
			if (cond == 3)
			{
				htmltext = "30612-01.htm";
			}
			else if (cond == 4)
			{
				htmltext = "30612-07.htm";
			}
		}
		else if (npcId == 32368)
		{
			if (cond == 5)
			{
				htmltext = "32368-01.htm";
			}
			if (st.getInt("talk") != 1)
			{
				st.takeItems(10353, -1);
				st.set("talk", "1");
			}
			else if (cond == 6)
			{
				htmltext = "32368-03.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (npc.getId() == 27338)
			{
				if (st.isCond(5) && st.calcDropItems(getId(), 10353, npc.getId(), 1))
				{
					st.setCond(6);
				}
			}
			else
			{
				if (st.isCond(4) && st.calcDropItems(getId(), 10352, npc.getId(), 30))
				{
					st.setCond(5);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _142_FallenAngelRequestOfDawn(142, _142_FallenAngelRequestOfDawn.class.getSimpleName(), "");
	}
}