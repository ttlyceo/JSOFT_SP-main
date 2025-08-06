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
public class _246_PossessorOfAPreciousSoul_3 extends Quest
{
	private static boolean _isSubActive;
	
	public _246_PossessorOfAPreciousSoul_3(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31740);
		addTalkId(31740, 30721, 31741);
		
		addKillId(21541, 21544, 25325, 21539, 21537, 21536, 21532);
		
		_isSubActive = getQuestParams(questId).getBool("isSubActive");
		
		questItemIds = new int[]
		{
		        7591, 7592, 7593, 7594, 21725
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return null;
		}
		
		if ((player.isSubClassActive() && _isSubActive) || !_isSubActive)
		{
			if (event.equalsIgnoreCase("31740-4.htm"))
			{
				final QuestState qs = player.getQuestState(_242_PossessorOfAPreciousSoul_2.class.getSimpleName());
				if ((qs != null && qs.isCompleted()) || st.getQuestItemsCount(7678) > 0)
				{
					if (st.isCreated() && player.getLevel() >= getMinLvl(getId()))
					{
						st.startQuest();
					}
				}
			}
			else if (event.equalsIgnoreCase("31741-2.htm"))
			{
				if (st.isCond(1))
				{
					st.set("awaitsWaterbinder", "1");
					st.set("awaitsEvergreen", "1");
					st.setCond(2, true);
					st.takeItems(7678, 1);
				}
			}
			else if (event.equalsIgnoreCase("31744-2.htm"))
			{
				if (st.isCond(2))
				{
					st.setCond(3, true);
				}
			}
			else if (event.equalsIgnoreCase("31741-5.htm"))
			{
				if (st.isCond(3))
				{
					st.takeItems(7591, 1);
					st.takeItems(7592, 1);
					st.setCond(4, true);
				}
			}
			else if (event.equalsIgnoreCase("31741-9.htm"))
			{
				if (st.isCond(5))
				{
					st.takeItems(7593, -1);
					st.takeItems(21725, -1);
					st.giveItems(7594, 1);
					st.setCond(6, true);
				}
			}
			else if (event.equalsIgnoreCase("30721-2.htm"))
			{
				if (st.isCond(6))
				{
					st.takeItems(7594, 1);
					st.calcExpAndSp(getId());
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		String htmltext = getNoQuestMsg(talker);
		final QuestState st = talker.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if ((npc.getId() != 31740) && !st.isStarted())
		{
			return htmltext;
		}
		
		if ((talker.isSubClassActive() && _isSubActive) || !_isSubActive)
		{
			switch (npc.getId())
			{
				case 31740 :
					final QuestState qs = talker.getQuestState(_242_PossessorOfAPreciousSoul_2.class.getSimpleName());
					if ((qs != null && qs.isCompleted()) || st.getQuestItemsCount(7678) > 0)
					{
						switch (st.getState())
						{
							case State.CREATED :
								if (talker.getLevel() < getMinLvl(getId()))
								{
									htmltext = "31740-2.htm";
									st.exitQuest(true);
								}
								else if (talker.getLevel() >= getMinLvl(getId()))
								{
									htmltext = "31740-1.htm";
								}
								break;
							case State.STARTED :
								if (st.isCond(1))
								{
									htmltext = "31740-5.htm";
								}
								break;
							case State.COMPLETED :
								htmltext = getAlreadyCompletedMsg(talker);
								break;
						}
					}
					break;
				case 31741 :
					switch (st.getCond())
					{
						case 1 :
							htmltext = "31741-1.htm";
							break;
						case 2 :
							htmltext = "31741-4.htm";
							break;
						case 3 :
							if (st.hasQuestItems(7591) && st.hasQuestItems(7592))
							{
								htmltext = "31741-3.htm";
							}
							break;
						case 4 :
							htmltext = "31741-8.htm";
							break;
						case 5 :
							if (st.hasQuestItems(7593) || (st.getQuestItemsCount(21725) >= 100))
							{
								htmltext = "31741-7.htm";
							}
							break;
						case 6 :
							if (st.hasQuestItems(7594))
							{
								htmltext = "31741-11.htm";
							}
							break;
					}
					break;
				case 30721 :
					switch (st.getCond())
					{
						case 6 :
							htmltext = "30721-1.htm";
							break;
					}
					break;
			}
		}
		else
		{
			htmltext = "sub.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Player partyMember;
		final QuestState st;
		switch (npc.getId())
		{
			case 21541 :
				partyMember = getRandomPartyMember(killer, "awaitsWaterbinder", "1");
				if (partyMember != null)
				{
					st = getQuestState(partyMember, false);
					if (st != null && st.isCond(2) && st.calcDropItems(getId(), 7591, npc.getId(), 1))
					{
						st.unset("awaitsWaterbinder");
						if (st.hasQuestItems(7592))
						{
							st.setCond(3, true);
							
						}
					}
				}
				break;
			case 21544 :
				partyMember = getRandomPartyMember(killer, "awaitsEvergreen", "1");
				if (partyMember != null)
				{
					st = getQuestState(partyMember, false);
					if (st != null && st.isCond(2) && st.calcDropItems(getId(), 7592, npc.getId(), 1))
					{
						st.unset("awaitsEvergreen");
						if (st.hasQuestItems(7591))
						{
							st.setCond(3, true);
						}
					}
				}
				break;
			case 25325 :
				QuestState pst;
				if ((killer.getParty() != null) && !killer.getParty().getMembers().isEmpty())
				{
					for (final Player pm : killer.getParty().getMembers())
					{
						pst = getQuestState(pm, false);
						if (pst != null)
						{
							if (pst != null && pst.isCond(4) && pst.calcDropItems(getId(), 7593, npc.getId(), 1))
							{
								pst.setCond(5, true);
							}
						}
					}
				}
				else
				{
					pst = getQuestState(killer, false);
					if (pst != null && pst.isCond(4) && pst.calcDropItems(getId(), 7593, npc.getId(), 1))
					{
						pst.setCond(5, true);
					}
				}
				break;
			case 21539 :
			case 21537 :
			case 21536 :
			case 21532 :
				st = getQuestState(killer, false);
				if (st != null && st.isCond(4) && st.calcDropItems(getId(), 21725, npc.getId(), 100))
				{
					st.setCond(5, true);
				}
				break;
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _246_PossessorOfAPreciousSoul_3(246, _246_PossessorOfAPreciousSoul_3.class.getSimpleName(), "");
	}
}