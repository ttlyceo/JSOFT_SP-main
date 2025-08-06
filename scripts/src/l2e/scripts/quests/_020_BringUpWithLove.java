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
 * Rework by LordWinter 05.12.2019
 */
public class _020_BringUpWithLove extends Quest
{
	public _020_BringUpWithLove(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31537);
		addTalkId(31537);
		addFirstTalkId(31537);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return getNoQuestMsg(player);
		}
		
		final String htmltext = event;
		switch (event)
		{
			case "31537-12.htm":
				st.startQuest();
				break;
			case "31537-03.htm":
				if (hasQuestItems(player, 15473))
				{
					return "31537-03a.htm";
				}
				giveItems(player, 15473, 1);
				break;
			
			case "31537-15.htm":
				if (st.isCond(2))
				{
					takeItems(player, 7185, -1);
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
				break;
			case "31537-21.htm" :
				if (player.getLevel() < getMinLvl(getId()))
				{
					return "31537-23.htm";
				}
				if (hasQuestItems(player, 15473))
				{
					return "31537-22.htm";
				}
				giveItems(player, 15473, 1);
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			newQuestState(player);
		}
		return "31537-20.htm";
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
		
		switch (st.getState())
		{
			case State.CREATED:
				htmltext = ((player.getLevel() < getMinLvl(getId())) ? "31537-00.htm" : "31537-01.htm");
				break;
			case State.STARTED:
				switch (st.getCond())
				{
					case 1:
						htmltext = "31537-13.htm";
						break;
					case 2:
						htmltext = "31537-14.htm";
						break;
				}
				break;
		}
		return htmltext;
	}
	
	public static void checkJewelOfInnocence(Player player)
	{
		final QuestState st = player.getQuestState(_020_BringUpWithLove.class.getSimpleName());
		if ((st != null) && st.isCond(1) && !st.hasQuestItems(7185) && (getRandom(20) == 0))
		{
			st.giveItems(7185, 1);
			st.setCond(2, true);
		}
	}
	
	public static void main(String[] args)
	{
		new _020_BringUpWithLove(20, _020_BringUpWithLove.class.getSimpleName(), "");
	}
}