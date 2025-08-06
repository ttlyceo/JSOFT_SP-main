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

/**
 * Rework by LordWinter 28.01.2020
 */
public class _10296_SevenSignsPoweroftheSeal extends Quest
{
	public _10296_SevenSignsPoweroftheSeal(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32792);
		addTalkId(32792, 32787, 32784, 30832, 32593, 32597);
		
		addKillId(18949);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32792-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32784-03.htm"))
		{
			if (st.isCond(3))
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("30832-03.htm"))
		{
			if (st.isCond(4))
			{
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("32597-03.htm"))
		{
			if (st.isCond(5) && player.getLevel() >= getMinLvl(getId()))
			{
				st.unset("EtisKilled");
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
			else
			{
				htmltext = "32597-00.htm";
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
		final int EtisKilled = st.getInt("EtisKilled");
		
		if (player.isSubClassActive())
		{
			return "no_subclass-allowed.htm";
		}
		
		if (npcId == 32792)
		{
			if (cond == 0)
			{
				final QuestState qs = player.getQuestState("_10295_SevenSignsSolinasTomb");
				if ((player.getLevel() >= getMinLvl(getId())) && (qs != null) && qs.isCompleted())
				{
					htmltext = "32792-01.htm";
				}
				else
				{
					htmltext = "32792-00.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "32792-04.htm";
			}
			else if (cond == 2)
			{
				htmltext = "32792-05.htm";
			}
			else if (cond >= 3)
			{
				htmltext = "32792-06.htm";
			}
		}
		else if (npcId == 32787)
		{
			if (cond == 1)
			{
				htmltext = "32787-01.htm";
			}
			else if (cond == 2)
			{
				if (EtisKilled == 0)
				{
					htmltext = "32787-01.htm";
				}
				else
				{
					st.setCond(3, true);
					htmltext = "32787-02.htm";
				}
			}
			else if (cond >= 3)
			{
				htmltext = "32787-04.htm";
			}
		}
		else if (npcId == 32784)
		{
			if (cond == 3)
			{
				htmltext = "32784-01.htm";
			}
			else if (cond >= 4)
			{
				htmltext = "32784-03.htm";
			}
		}
		else if (npcId == 30832)
		{
			if (cond == 4)
			{
				htmltext = "30832-01.htm";
			}
			else if (cond == 5)
			{
				htmltext = "30832-04.htm";
			}
		}
		else if (npcId == 32593)
		{
			if (cond == 5)
			{
				htmltext = "32593-01.htm";
			}
		}
		else if (npcId == 32597)
		{
			if (cond == 5)
			{
				htmltext = "32597-01.htm";
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
		
		if (st != null && npc.getId() == 18949)
		{
			st.set("EtisKilled", 1);
			player.showQuestMovie(30);
			
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String args[])
	{
		new _10296_SevenSignsPoweroftheSeal(10296, _10296_SevenSignsPoweroftheSeal.class.getSimpleName(), "");
	}
}