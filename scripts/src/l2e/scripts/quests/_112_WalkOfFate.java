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
 * Rework by LordWinter 09.06.2021
 */
public class _112_WalkOfFate extends Quest
{
	public _112_WalkOfFate(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30572);
		addTalkId(30572, 32017);
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
		
		if (event.equalsIgnoreCase("32017-02.htm") && npc.getId() == 32017)
		{
			if (st.isCond(1))
	    	{
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
	    	}
		}
		else if (event.equalsIgnoreCase("30572-02.htm") && npc.getId() == 30572)
		{
			if (st.isCreated() && (player.getLevel() >= getMinLvl(getId()) && player.getLevel() <= getMaxLvl(getId())))
			{
				st.startQuest();
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if(st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 30572)
				{
					if (player.getLevel() >= getMinLvl(getId()) && player.getLevel() <= getMaxLvl(getId()))
					{
						htmltext = "30572-01.htm";
					}
					else
					{
						htmltext = "30572-00.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED:
				if (npcId == 30572)
				{
					htmltext = "30572-03.htm";
				}
				else if (npcId == 32017)
				{
					htmltext = "32017-01.htm";
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _112_WalkOfFate(112, _112_WalkOfFate.class.getSimpleName(), "");
	}
}
