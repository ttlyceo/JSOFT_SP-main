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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 17.10.2020
 */
public final class _727_HopeWithinTheDarkness extends Quest
{
	public _727_HopeWithinTheDarkness(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(36403, 36404, 36405, 36406, 36407, 36408, 36409, 36410, 36411);
		addTalkId(36403, 36404, 36405, 36406, 36407, 36408, 36409, 36410, 36411);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if (event.equalsIgnoreCase("CastleWarden-05.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if ((player.getClan() == null) || (npc.getCastle() == null) || (player.getClan().getCastleId() != npc.getCastle().getId()))
		{
			return "CastleWarden-03.htm";
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "CastleWarden-01.htm";
				}
				else
				{
					htmltext = "CastleWarden-04.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				if (st.isCond(1))
				{
					htmltext = Rnd.chance(50) ? "CastleWarden-06.htm" : "CastleWarden-13.htm";
				}
				else if (st.isCond(2))
				{
					st.calcReward(getId());
					st.exitQuest(true, true);
					htmltext = "CastleWarden-14.htm";
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _727_HopeWithinTheDarkness(727, _727_HopeWithinTheDarkness.class.getSimpleName(), "");
	}
}