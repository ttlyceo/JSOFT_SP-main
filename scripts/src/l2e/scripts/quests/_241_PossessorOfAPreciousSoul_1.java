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
 * Rework by LordWinter 25.05.2021
 */
public class _241_PossessorOfAPreciousSoul_1 extends Quest
{
	private static boolean _isSubActive;
	
	public _241_PossessorOfAPreciousSoul_1(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31739);
		addTalkId(30692, 30753, 30754, 31042, 31272, 31336, 31739, 31740, 31742, 31743, 31744);
		
		addKillId(20244, 20245, 20283, 21508, 21509, 21510, 21511, 21512, 27113, 20669);
		
		_isSubActive = getQuestParams(questId).getBool("isSubActive");
		
		questItemIds = new int[]
		{
		        7587, 7597, 7589, 7588, 7598, 7599
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return null;
		}
		
		if (!player.isSubClassActive() && _isSubActive)
		{
			return "sub.htm";
		}
		
		if ((_isSubActive && player.isSubClassActive()) || !_isSubActive)
		{
			if (event.equalsIgnoreCase("31739-4.htm"))
			{
				if (player.getLevel() >= getMinLvl(getId()) && st.isCreated())
				{
					st.startQuest();
				}
			}
			else if (event.equalsIgnoreCase("30753-2.htm"))
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
				}
			}
			else if (event.equalsIgnoreCase("30754-2.htm"))
			{
				if (st.isCond(2))
				{
					st.setCond(3, true);
				}
			}
			else if (event.equalsIgnoreCase("31739-8.htm"))
			{
				if (st.isCond(4) && st.getQuestItemsCount(7587) > 0)
				{
					st.takeItems(7587, 1);
					st.setCond(5, true);
				}
			}
			else if (event.equalsIgnoreCase("31042-2.htm"))
			{
				if (st.isCond(5))
				{
					st.setCond(6, true);
				}
			}
			else if (event.equalsIgnoreCase("31042-5.htm"))
			{
				if (st.isCond(7) && st.getQuestItemsCount(7597) >= 10)
				{
					st.takeItems(7597, 10);
					st.giveItems(7589, 1);
					st.setCond(8, true);
				}
			}
			else if (event.equalsIgnoreCase("31739-12.htm"))
			{
				if (st.isCond(8) && st.getQuestItemsCount(7589) > 0)
				{
					st.takeItems(7589, 1);
					st.setCond(9, true);
				}
			}
			else if (event.equalsIgnoreCase("30692-2.htm"))
			{
				if (st.isCond(9) && !(st.getQuestItemsCount(7588) > 0))
				{
					st.giveItems(7588, 1);
					st.setCond(10, true);
				}
			}
			else if (event.equalsIgnoreCase("31739-15.htm"))
			{
				if (st.isCond(10) && st.getQuestItemsCount(7588) > 0)
				{
					st.takeItems(7588, 1);
					st.setCond(11, true);
				}
			}
			else if (event.equalsIgnoreCase("31742-2.htm"))
			{
				if (st.isCond(11))
				{
					st.setCond(12, true);
				}
			}
			else if (event.equalsIgnoreCase("31744-2.htm"))
			{
				if (st.isCond(12))
				{
					st.setCond(13, true);
				}
			}
			else if (event.equalsIgnoreCase("31336-2.htm"))
			{
				if (st.isCond(13))
				{
					st.setCond(14, true);
				}
			}
			else if (event.equalsIgnoreCase("31336-5.htm"))
			{
				if (st.isCond(15) && st.getQuestItemsCount(7598) > 0)
				{
					st.takeItems(7598, 5);
					st.giveItems(7599, 1);
					st.setCond(16, true);
				}
			}
			else if (event.equalsIgnoreCase("31743-2.htm"))
			{
				if (st.isCond(16) && st.getQuestItemsCount(7599) > 0)
				{
					st.takeItems(7599, 1);
					st.setCond(17, true);
				}
			}
			else if (event.equalsIgnoreCase("31742-5.htm"))
			{
				if (st.isCond(17))
				{
					st.setCond(18, true);
				}
			}
			else if (event.equalsIgnoreCase("31740-2.htm"))
			{
				if (st.isCond(18))
				{
					st.setCond(19, true);
				}
			}
			else if (event.equalsIgnoreCase("31272-2.htm"))
			{
				if (st.isCond(19))
				{
					st.setCond(20, true);
				}
			}
			else if (event.equalsIgnoreCase("31272-5.htm"))
			{
				if (st.isCond(20) && st.getQuestItemsCount(6029) >= 5 && st.getQuestItemsCount(6033) > 0)
				{
					st.takeItems(6029, 5);
					st.takeItems(6033, 1);
					st.setCond(21, true);
				}
				else
				{
					htmltext = "31272-4.htm";
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
		
		if (npc.getId() != 31739 && !st.isStarted())
		{
			return htmltext;
		}
		
		if (!player.isSubClassActive() && _isSubActive)
		{
			return "sub.htm";
		}
		
		final int cond = st.getCond();
		
		if (npc.getId() == 31739)
		{
			switch (st.getState())
			{
				case State.CREATED :
					if (player.getLevel() >= getMinLvl(getId()) && (_isSubActive && player.isSubClassActive() || !_isSubActive))
					{
						htmltext = "31739-1.htm";
					}
					else
					{
						htmltext = "31739-2.htm";
						st.exitQuest(true);
					}
					break;
				case State.STARTED :
					switch (st.getCond())
					{
						case 1 :
							htmltext = "31739-5.htm";
							break;
						case 4 :
							if (st.getQuestItemsCount(7587) == 1)
							{
								htmltext = "31739-6.htm";
							}
							break;
						case 5 :
							htmltext = "31739-9.htm";
							break;
						case 8 :
							if (st.getQuestItemsCount(7589) == 1)
							{
								htmltext = "31739-11.htm";
							}
							break;
						case 9 :
							htmltext = "31739-13.htm";
							break;
						case 10 :
							if (st.getQuestItemsCount(7588) == 1)
							{
								htmltext = "31739-14.htm";
							}
							break;
						case 11 :
							htmltext = "31739-16.htm";
							break;
					}
					break;
				case State.COMPLETED :
					htmltext = getAlreadyCompletedMsg(player);
					break;
			}
		}
		else
		{
			switch (npc.getId())
			{
				case 30753 :
				{
					switch (cond)
					{
						case 1:
							htmltext = "30753-1.htm";
							break;
						case 2:
							htmltext = "30753-3.htm";
							break;
					}
					break;
				}
				case 30754 :
				{
					switch (cond)
					{
						case 2:
							htmltext = "30754-1.htm";
							break;
						case 3:
							htmltext = "30754-3.htm";
							break;
					}
					break;
				}
				case 31042 :
				{
					switch (cond)
					{
						case 5:
							htmltext = "31042-1.htm";
							break;
						case 6:
							htmltext = "31042-4.htm";
							break;
						case 7:
							if (st.getQuestItemsCount(7597) == 10)
							{
								htmltext = "31042-3.htm";
							}
							break;
						case 8:
							htmltext = "31042-6.htm";
							break;
					}
					break;
				}
				case 30692 :
				{
					switch (cond)
					{
						case 9:
							htmltext = "30692-1.htm";
							break;
						case 10:
							htmltext = "30692-3.htm";
							break;
					}
					break;
				}
				case 31742 :
				{
					switch (cond)
					{
						case 11:
							htmltext = "31742-1.htm";
							break;
						case 12:
							htmltext = "31742-3.htm";
							break;
						case 17:
							htmltext = "31742-4.htm";
							break;
						case 18:
						case 19:
						case 20:
						case 21:
							htmltext = "31742-6.htm";
							break;
					}
					break;
				}
				case 31744 :
				{
					switch (cond)
					{
						case 12:
							htmltext = "31744-1.htm";
							break;
						case 13:
							htmltext = "31744-3.htm";
							break;
					}
					break;
				}
				case 31336 :
				{
					switch (cond)
					{
						case 13:
							htmltext = "31336-1.htm";
							break;
						case 14:
							htmltext = "31336-4.htm";
							break;
						case 15:
							if (st.getQuestItemsCount(7598) == 5)
							{
								htmltext = "31336-3.htm";
							}
							break;
						case 16:
							htmltext = "31336-6.htm";
							break;
					}
					break;
				}
				case 31743 :
				{
					switch (cond)
					{
						case 16:
							if (st.getQuestItemsCount(7599) == 1)
							{
								htmltext = "31743-1.htm";
							}
							break;
						case 17:
							htmltext = "31743-3.htm";
							break;
					}
					break;
				}
				case 31740 :
				{
					switch (cond)
					{
						case 18:
						case 19:
						case 20:
						case 21:
							st.calcExpAndSp(getId());
							st.calcReward(getId());
							st.exitQuest(false, true);
							htmltext = "31740-5.htm";
							break;
					}
					break;
				}
				case 31272 :
				{
					switch (cond)
					{
						case 18:
						case 19:
						case 20:
						case 21:
							htmltext = "31272-7.htm";
							break;
					}
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case 27113 :
			{
				final Player ptMember = getRandomPartyMember(killer, 3);
				if (ptMember != null)
				{
					final QuestState st = ptMember.getQuestState(getName());
					if (st != null && st.calcDropItems(getId(), 7587, npc.getId(), 1))
					{
						st.setCond(4, true);
					}
				}
				break;
			}
			case 20244:
			case 20245:
			case 20283:
			case 20284:
			{
				final Player ptMember = getRandomPartyMember(killer, 6);
				if (ptMember != null)
				{
					final QuestState st = ptMember.getQuestState(getName());
					if (st != null && st.calcDropItems(getId(), 7597, npc.getId(), 10))
					{
						st.setCond(7, true);
					}
				}
				break;
			}
			case 20669:
			{
				final Player ptMember = getRandomPartyMember(killer, 14);
				if (ptMember != null)
				{
					final QuestState st = ptMember.getQuestState(getName());
					if (st != null && st.calcDropItems(getId(), 7598, npc.getId(), 5))
					{
						st.setCond(15, true);
					}
				}
				break;
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _241_PossessorOfAPreciousSoul_1(241, _241_PossessorOfAPreciousSoul_1.class.getSimpleName(), "");
	}
}