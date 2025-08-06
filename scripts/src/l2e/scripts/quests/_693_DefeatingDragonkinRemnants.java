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

import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Created by LordWinter 20.12.2020
 */
public class _693_DefeatingDragonkinRemnants extends Quest
{
	public _693_DefeatingDragonkinRemnants(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32527);
		addTalkId(32527);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32527-05.htm"))
		{
			if (st.isCreated())
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
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				if ((player.getLevel() >= getMinLvl(getId())) && (player.getLevel() <= getMaxLvl(getId())))
				{
					htmltext = "32527-01.htm";
				}
				else
				{
					htmltext = "32527-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				switch (st.getCond())
				{
					case 1 :
						final Party party = player.getParty();
						if (party == null || party.getMemberCount() < 2)
						{
							htmltext = "32527-07.htm";
						}
						else if (!party.getLeader().equals(player))
						{
							htmltext = "32527-08.htm";
						}
						else
						{
							htmltext = "32527-06.htm";
						}
						break;
					case 2 :
						if (st.getInt("timeDiff") > 0)
						{
							if (giveReward(st, st.getInt("timeDiff")))
							{
								htmltext = "32527-13.htm";
							}
							else
							{
								htmltext = "32527-14.htm";
							}
							st.unset("timeDiff");
							st.unset("reflectionId");
							st.exitQuest(true, true);
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	private boolean giveReward(QuestState st, int finishDiff)
	{
		if (finishDiff == 0)
		{
			return false;
		}
		if (finishDiff < 5)
		{
			switch (st.getInt("reflectionId"))
			{
				case 123 :
					st.calcReward(getId(), 1);
					break;
				case 124 :
					st.calcReward(getId(), 5);
					break;
				case 125 :
					st.calcReward(getId(), 9);
					break;
				case 126 :
					st.calcReward(getId(), 13);
					break;
			}
		}
		else if (finishDiff < 10)
		{
			switch (st.getInt("reflectionId"))
			{
				case 123 :
					st.calcReward(getId(), 2);
					break;
				case 124 :
					st.calcReward(getId(), 6);
					break;
				case 125 :
					st.calcReward(getId(), 10);
					break;
				case 126 :
					st.calcReward(getId(), 14);
					break;
			}
		}
		else if (finishDiff < 15)
		{
			switch (st.getInt("reflectionId"))
			{
				case 123 :
					st.calcReward(getId(), 3);
					break;
				case 124 :
					st.calcReward(getId(), 7);
					break;
				case 125 :
					st.calcReward(getId(), 11);
					break;
				case 126 :
					st.calcReward(getId(), 15);
					break;
			}
		}
		else if (finishDiff < 20)
		{
			switch (st.getInt("reflectionId"))
			{
				case 123 :
					st.calcReward(getId(), 4);
					break;
				case 124 :
					st.calcReward(getId(), 8);
					break;
				case 125 :
					st.calcReward(getId(), 12);
					break;
				case 126 :
					st.calcReward(getId(), 16);
					break;
			}
		}
		return true;
	}
	
	public static void main(String[] args)
	{
		new _693_DefeatingDragonkinRemnants(693, _693_DefeatingDragonkinRemnants.class.getSimpleName(), "");
	}
}