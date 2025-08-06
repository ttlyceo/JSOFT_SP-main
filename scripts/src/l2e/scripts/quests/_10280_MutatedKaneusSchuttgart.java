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
public class _10280_MutatedKaneusSchuttgart extends Quest
{
	public _10280_MutatedKaneusSchuttgart(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31981);
		addTalkId(31981, 31972);
		
		addKillId(18571, 18573);
		
		questItemIds = new int[]
		{
		        13838, 13839
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
			case "31981-03.htm":
				if (st.isCreated() && npc.getId() == 31981)
				{
					st.startQuest();
				}
				break;
			case "31972-03.htm":
				if (st.isCond(2) && npc.getId() == 31972)
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
			case 31981 :
				if (st.isCompleted())
				{
					htmltext = "31981-06.htm";
				}
				else if (st.isCreated())
				{
					htmltext = (player.getLevel() >= getMinLvl(getId())) ? "31981-01.htm" : "31981-00.htm";
				}
				else
				{
					if (st.isCond(2))
					{
						htmltext = "31981-05.htm";
					}
					else
					{
						htmltext = "31981-04.htm";
					}
				}
				break;
			case 31972 :
				if (st.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				else
				{
					if (st.isCond(2))
					{
						htmltext = "31972-02.htm";
					}
					else
					{
						htmltext = "31972-01.htm";
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
		else
		{
			rewardItem(npcId, st);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private final void rewardItem(int npcId, QuestState st)
	{
		if (npcId == 18571)
		{
			st.calcDoDropItems(getId(), 13838, npcId, 1);
		}
		else if (npcId == 18573)
		{
			st.calcDoDropItems(getId(), 13839, npcId, 1);
		}
		
		if (st.hasQuestItems(13838) && st.hasQuestItems(13839))
		{
			st.setCond(2, true);
		}
	}
	
	public static void main(String[] args)
	{
		new _10280_MutatedKaneusSchuttgart(10280, _10280_MutatedKaneusSchuttgart.class.getSimpleName(), "");
	}
}