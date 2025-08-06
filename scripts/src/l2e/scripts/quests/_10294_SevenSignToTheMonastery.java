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
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Rework by LordWinter 28.12.2019
 */
public final class _10294_SevenSignToTheMonastery extends Quest
{
	public _10294_SevenSignToTheMonastery(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32784);
		addTalkId(32784, 32792, 32787, 32803, 32804, 32805, 32806, 32807, 32821, 32825, 32829, 32833);
		addFirstTalkId(32822, 32823, 32824, 32826, 32827, 32828, 32830, 32831, 32832, 32834, 32835, 32836);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		
		if (npcId == 32784)
		{
			if (event.equalsIgnoreCase("32784-03.htm"))
			{
				if (st.isCreated())
				{
					st.startQuest();
				}
			}
		}
		else if (npcId == 32792)
		{
			if (event.equalsIgnoreCase("32792-03.htm"))
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
				}
			}
			else if (event.equalsIgnoreCase("32792-08.htm"))
			{
				if (player.isSubClassActive())
				{
					htmltext = "32792-10.htm";
				}
				else
				{
					if (st.isCond(3))
					{
						st.unset("book_32821");
						st.unset("book_32825");
						st.unset("book_32829");
						st.unset("book_32833");
						st.unset("first");
						st.unset("second");
						st.unset("third");
						st.unset("fourth");
						st.unset("movie");
						player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
						st.calcExpAndSp(getId());
						st.exitQuest(false, true);
						htmltext = "32792-08.htm";
					}
				}
			}
		}
		else if (npcId == 32821)
		{
			if (event.equalsIgnoreCase("32821-02.htm"))
			{
				st.playSound("ItemSound.quest_middle");
				st.set("book_" + npc.getId(), 1);
				st.set("first", "1");
				if (isAllBooksFinded(st))
				{
					npc.setDisplayEffect(1);
					player.showQuestMovie(25);
					st.set("movie", "1");
					return "";
				}
			}
		}
		else if (npcId == 32825)
		{
			if (event.equalsIgnoreCase("32825-02.htm"))
			{
				st.playSound("ItemSound.quest_middle");
				st.set("book_" + npc.getId(), 1);
				st.set("second", "1");
				if (isAllBooksFinded(st))
				{
					npc.setDisplayEffect(1);
					player.showQuestMovie(25);
					st.set("movie", "1");
					return "";
				}
			}
		}
		else if (npcId == 32829)
		{
			if (event.equalsIgnoreCase("32829-02.htm"))
			{
				st.playSound("ItemSound.quest_middle");
				st.set("book_" + npc.getId(), 1);
				st.set("third", "1");
				if (isAllBooksFinded(st))
				{
					npc.setDisplayEffect(1);
					player.showQuestMovie(25);
					st.set("movie", "1");
					return "";
				}
			}
		}
		else if (npcId == 32833)
		{
			if (event.equalsIgnoreCase("32833-02.htm"))
			{
				st.playSound("ItemSound.quest_middle");
				st.set("book_" + npc.getId(), 1);
				st.set("fourth", "1");
				if (isAllBooksFinded(st))
				{
					npc.setDisplayEffect(1);
					player.showQuestMovie(25);
					st.set("movie", "1");
					return "";
				}
			}
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
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		final int first = st.getInt("first");
		final int second = st.getInt("second");
		final int third = st.getInt("third");
		final int fourth = st.getInt("fourth");
		final int movie = st.getInt("movie");
		
		if (st.getState() == State.CREATED)
		{
			if (npcId == 32784)
			{
				final QuestState qs = player.getQuestState("_10293_SevenSignsForbiddenBook");
				if (cond == 0)
				{
					if ((player.getLevel() >= getMinLvl(getId())) && (qs != null) && qs.isCompleted())
					{
						htmltext = "32784-01.htm";
					}
					else
					{
						htmltext = "32784-00.htm";
						st.exitQuest(true);
					}
				}
			}
		}
		else if (st.getState() == State.STARTED)
		{
			if (npcId == 32784)
			{
				if (cond == 1)
				{
					htmltext = "32784-04.htm";
				}
			}
			else if ((npcId == 32804) && (cond == 2))
			{
				if (st.getInt("book_" + 32821) > 0)
				{
					htmltext = "32804-05.htm";
				}
				else
				{
					htmltext = "32804-01.htm";
				}
			}
			else if ((npcId == 32805) && (cond == 2))
			{
				if (st.getInt("book_" + 32825) > 0)
				{
					htmltext = "32805-05.htm";
				}
				else
				{
					htmltext = "32805-01.htm";
				}
			}
			else if ((npcId == 32806) && (cond == 2))
			{
				if (st.getInt("book_" + 32829) > 0)
				{
					htmltext = "32806-05.htm";
				}
				else
				{
					htmltext = "32806-01.htm";
				}
			}
			else if ((npcId == 32807) && (cond == 2))
			{
				if (st.getInt("book_" + 32833) > 0)
				{
					htmltext = "32807-05.htm";
				}
				else
				{
					htmltext = "32807-01.htm";
				}
			}
			else if (npcId == 32787)
			{
				if (cond == 1)
				{
					htmltext = "32787-01.htm";
				}
				else if (cond == 2)
				{
					htmltext = "32787-02.htm";
				}
				else if (cond == 3)
				{
					htmltext = "32787-03.htm";
				}
			}
			else if (npcId == 32792)
			{
				if (cond == 1)
				{
					htmltext = "32792-01.htm";
				}
				else if (cond == 2)
				{
					htmltext = "32792-06.htm";
				}
				else if (cond == 3)
				{
					htmltext = "32792-07.htm";
				}
			}
			else if (npcId == 32803)
			{
				if (cond == 2)
				{
					if (isAllBooksFinded(st))
					{
						htmltext = "32803-04.htm";
						st.setCond(3, true);
					}
					else
					{
						htmltext = "32803-01.htm";
					}
				}
				else if (cond == 3)
				{
					htmltext = "32803-05.htm";
				}
			}
			else if (npcId == 32804)
			{
				htmltext = "32804-01.htm";
			}
			else if (npcId == 32805)
			{
				htmltext = "32805-01.htm";
			}
			else if (npcId == 32806)
			{
				htmltext = "32806-01.htm";
			}
			else if (npcId == 32807)
			{
				htmltext = "32807-01.htm";
			}
			else if (npcId == 32821)
			{
				if ((movie == 1) || (first == 1))
				{
					htmltext = "empty_desk.htm";
				}
				else
				{
					htmltext = "32821-01.htm";
				}
			}
			else if (npcId == 32825)
			{
				if ((movie == 1) || (second == 1))
				{
					htmltext = "empty_desk.htm";
				}
				else
				{
					htmltext = "32825-01.htm";
				}
			}
			else if (npcId == 32829)
			{
				if ((movie == 1) || (third == 1))
				{
					htmltext = "empty_desk.htm";
				}
				else
				{
					htmltext = "32829-01.htm";
				}
			}
			else if (npcId == 32833)
			{
				if ((movie == 1) || (fourth == 1))
				{
					htmltext = "empty_desk.htm";
				}
				else
				{
					htmltext = "32833-01.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext = getNoQuestMsg(player);
		
		final int npcId = npc.getId();
		
		if ((npcId == 32822) || (npcId == 32823) || (npcId == 32824) || (npcId == 32826) || (npcId == 32827) || (npcId == 32828) || (npcId == 32830) || (npcId == 32831) || (npcId == 32832) || (npcId == 32834) || (npcId == 32835) || (npcId == 32836))
		{
			return "empty_desk.htm";
		}
		return htmltext;
	}
	
	private boolean isAllBooksFinded(QuestState st)
	{
		return (st.getInt("book_" + 32821) + st.getInt("book_" + 32825) + st.getInt("book_" + 32829) + st.getInt("book_" + 32833)) >= 4;
	}
	
	public static void main(String[] args)
	{
		new _10294_SevenSignToTheMonastery(10294, _10294_SevenSignToTheMonastery.class.getSimpleName(), "");
	}
}