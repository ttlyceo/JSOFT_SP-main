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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;

/**
 * Rework by LordWinter 22.12.2019
 */
public class _10277_MutatedKaneusDion extends Quest
{
	public _10277_MutatedKaneusDion(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30071);
		addTalkId(30071, 30461);
		
		addKillId(18558, 18559);
		
		questItemIds = new int[]
		{
		        13832, 13833
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30071-03.htm":
				if (st.isCreated() && npc.getId() == 30071)
				{
					st.startQuest();
				}
				break;
			case "30461-03.htm":
				if (st.isCond(2) && npc.getId() == 30461)
				{
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
				break;
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
		
		switch (npc.getId())
		{
			case 30071 :
				if (st.isCompleted())
				{
					htmltext = "30071-06.htm";
				}
				else if (st.isCreated())
				{
					htmltext = (player.getLevel() >= getMinLvl(getId())) ? "30071-01.htm" : "30071-00.htm";
				}
				else
				{
					if (st.isCond(2))
					{
						htmltext = "30071-05.htm";
					}
					else
					{
						htmltext = "30071-04.htm";
					}
				}
				break;
			case 30461 :
				if (st.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				else
				{
					if (st.isCond(2))
					{
						htmltext = "30461-02.htm";
					}
					else
					{
						htmltext = "30461-01.htm";
					}
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		QuestState st = killer.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		final int npcId = npc.getId();
		if (killer.getParty() != null)
		{
			final List<QuestState> members = new ArrayList<>();
			for (final Player member : killer.getParty().getMembers())
			{
				st = member.getQuestState(getName());
				if ((st != null) && st.isStarted())
				{
					members.add(st);
				}
			}
			
			if (!members.isEmpty())
			{
				for (final QuestState member : members)
				{
					rewardItem(npcId, member);
				}
			}
		}
		else if (st.isStarted())
		{
			rewardItem(npcId, st);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private final void rewardItem(int npcId, QuestState st)
	{
		if (npcId == 18558)
		{
			st.calcDoDropItems(getId(), 13832, npcId, 1);
		}
		else if (npcId == 18559)
		{
			st.calcDoDropItems(getId(), 13833, npcId, 1);
		}
		
		if (st.hasQuestItems(13832) && st.hasQuestItems(13833))
		{
			st.setCond(2, true);
		}
	}
	
	public static void main(String[] args)
	{
		new _10277_MutatedKaneusDion(10277, _10277_MutatedKaneusDion.class.getSimpleName(), "");
	}
}