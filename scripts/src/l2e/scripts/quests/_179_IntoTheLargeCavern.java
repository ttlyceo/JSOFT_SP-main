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
 * Based on L2J Eternity-World
 */
public class _179_IntoTheLargeCavern extends Quest
{
	private static final String qn = "_179_IntoTheLargeCavern";
	// NPC's
	private static final int _kekropus = 32138;
	private static final int _nornil = 32258;
	
	public _179_IntoTheLargeCavern(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(_kekropus);
		addTalkId(_kekropus);
		addTalkId(_nornil);
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
		
		if (npc.getId() == _kekropus)
		{
			if (event.equalsIgnoreCase("32138-03.htm"))
			{
				if (st.isCreated())
				{
					st.setState(State.STARTED);
					st.set("cond", "1");
					st.playSound("ItemSound.quest_accept");
				}
			}
		}
		else if (npc.getId() == _nornil)
		{
			if (event.equalsIgnoreCase("32258-08.htm"))
			{
				if (st.isCond(1))
				{
					st.giveItems(391, 1);
					st.giveItems(413, 1);
					st.exitQuest(false, true);
				}
			}
			else if (event.equalsIgnoreCase("32258-09.htm"))
			{
				if (st.isCond(1))
				{
					st.giveItems(847, 2);
					st.giveItems(890, 2);
					st.giveItems(910, 1);
					st.exitQuest(false, true);
				}
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
		
		final QuestState _prev = player.getQuestState("178_IconicTrinity");
		if (player.getRace().ordinal() != 5)
		{
			return "32138-00a.htm";
		}
		
		if (_prev != null && _prev.getState() == State.COMPLETED && player.getLevel() >= 17 && player.getClassId().level() == 0)
		{
			if (npc.getId() == _kekropus)
			{
				switch (st.getState())
				{
					case State.CREATED :
						htmltext = "32138-01.htm";
						break;
					case State.STARTED :
						if (st.getInt("cond") == 1)
						{
							htmltext = "32138-05.htm";
						}
						break;
					case State.COMPLETED :
						htmltext = getAlreadyCompletedMsg(player);
						break;
				}
			}
			else if (npc.getId() == _nornil && st.getState() == State.STARTED)
			{
				htmltext = "32258-01.htm";
			}
			else if (npc.getId() == _nornil && st.getState() == State.COMPLETED)
			{
				htmltext = "32258-exit.htm";
			}
		}
		else
		{
			htmltext = "32138-00.htm";
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _179_IntoTheLargeCavern(179, qn, "");
	}
}