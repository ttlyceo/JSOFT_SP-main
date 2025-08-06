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
 * Created by LordWinter 06.07.2012
 * Based on L2J Eternity-World
 */
public class _121_PavelTheGiant extends Quest
{
	private static final String qn = "_121_PavelTheGiant";

	// NPCs
	private final static int NEWYEAR = 31961;
	private final static int YUMI = 32041;
	
	public _121_PavelTheGiant(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(NEWYEAR);
		addTalkId(NEWYEAR);
		addTalkId(YUMI);
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
		
		if (event.equalsIgnoreCase("31961-2.htm"))
		{
			if (st.isCreated() && player.getLevel() >= 46)
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("32041-2.htm"))
		{
			if (st.isCond(1))
			{
				st.addExpAndSp(346320, 26069);
				st.playSound("ItemSound.quest_finish");
				st.unset("cond");
				st.exitQuest(false);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if(st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= 46)
				{
					htmltext = "31961-1.htm";
				}
				else
				{
					htmltext = "31961-1a.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case NEWYEAR:
						htmltext = "31961-2a.htm";
						break;
					
					case YUMI:
						htmltext = "32041-1.htm";
						break;
				}
				break;
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _121_PavelTheGiant(121, qn, "");
	}
}