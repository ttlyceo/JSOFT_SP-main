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
 * Created by LordWinter 19.10.2020
 */
public final class _512_BladeUnderFoot extends Quest
{
	public _512_BladeUnderFoot(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(36403, 36404, 36405, 36406, 36407, 36408, 36409, 36410, 36411);
		addTalkId(36403, 36404, 36405, 36406, 36407, 36408, 36409, 36410, 36411);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if (event.equalsIgnoreCase("CastleWarden-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("CastleWarden-09.htm"))
		{
			st.exitQuest(true, true);
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
		
		switch (st.getState())
		{
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "CastleWarden-03.htm";
				}
				else
				{
					htmltext = "CastleWarden-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				if (st.isCond(1))
				{
					final long count = st.getQuestItemsCount(9797);
					if (count > 0)
					{
						htmltext = "CastleWarden-08.htm";
						st.takeItems(9797, -1);
						st.rewardItems(9912, count);
					}
					else
					{
						htmltext = "CastleWarden-04.htm";
					}
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _512_BladeUnderFoot(512, _512_BladeUnderFoot.class.getSimpleName(), "");
	}
}