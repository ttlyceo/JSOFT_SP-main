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
 * Rework by LordWinter 17.10.2020
 */
public class _726_LightwithintheDarkness extends Quest
{
	public _726_LightwithintheDarkness(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(35666, 35698, 35735, 35767, 35804, 35835, 35867, 35904, 35936, 35974, 36011, 36043, 36081, 36118, 36149, 36181, 36219, 36257, 36294, 36326, 36364);
		addTalkId(35666, 35698, 35735, 35767, 35804, 35835, 35867, 35904, 35936, 35974, 36011, 36043, 36081, 36118, 36149, 36181, 36219, 36257, 36294, 36326, 36364);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		if (event.equalsIgnoreCase("FortWarden-04.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("reward"))
		{
			if (st.isCond(2))
			{
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
			return null;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		final QuestState qs = player.getQuestState(_727_HopeWithinTheDarkness.class.getSimpleName());
		if (qs != null)
		{
			st.exitQuest(true);
			return "FortWarden-01b.htm";
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "FortWarden-01.htm";
				}
				else
				{
					htmltext = "FortWarden-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if (st.isCond(2))
				{
					htmltext = "FortWarden-06.htm";
				}
				else
				{
					htmltext = "FortWarden-05.htm";
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _726_LightwithintheDarkness(726, _726_LightwithintheDarkness.class.getSimpleName(), "");
	}
}