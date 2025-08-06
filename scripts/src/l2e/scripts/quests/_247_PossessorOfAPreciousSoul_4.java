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

import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.serverpackets.updatetype.UserInfoType;

/**
 * Rework by LordWinter 25.05.2021
 */
public class _247_PossessorOfAPreciousSoul_4 extends Quest
{
	private static boolean _isSubActive;
	
	public _247_PossessorOfAPreciousSoul_4(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(31740);
		addTalkId(31740, 31745);
		
		_isSubActive = getQuestParams(id).getBool("isSubActive");
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return null;
		}
		
		if ((player.isSubClassActive() && _isSubActive) || !_isSubActive)
		{
			if (event.equals("31740-3.htm"))
			{
				final QuestState qs = player.getQuestState(_246_PossessorOfAPreciousSoul_3.class.getSimpleName());
				if ((qs != null && qs.isCompleted()) || st.getQuestItemsCount(7679) > 0)
				{
					if (st.isCreated() && player.getLevel() >= getMinLvl(getId()))
					{
						st.startQuest();
					}
				}
			}
			else if (event.equals("31740-5.htm"))
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
					st.takeItems(7679, -1);
					player.teleToLocation(143209, 43968, -3038, true, player.getReflection());
				}
			}
			else if (event.equals("31745-5.htm"))
			{
				if (st.isCond(2))
				{
					Olympiad.addNoble(player);
					player.setNoble(true);
					if (player.getClan() != null)
					{
						player.setPledgeClass(ClanMember.calculatePledgeClass(player));
					}
					else
					{
						player.setPledgeClass(5);
					}
					player.sendUserInfo(UserInfoType.SOCIAL, UserInfoType.CLAN);
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
			}
		}
		return event;
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
		
		if ((npc.getId() != 31740) && (st.getState() != State.STARTED))
		{
			return htmltext;
		}
		
		if (st.getState() == State.CREATED)
		{
			st.setCond(0);
		}
		
		if ((player.isSubClassActive() && _isSubActive) || !_isSubActive)
		{
			if (npc.getId() == 31740)
			{
				if (st.getState() == State.COMPLETED)
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				else
				{
					final QuestState qs = player.getQuestState(_246_PossessorOfAPreciousSoul_3.class.getSimpleName());
					if ((qs != null && qs.isCompleted()) || st.getQuestItemsCount(7679) > 0)
					{
						if (st.isCond(0) || st.isCond(1))
						{
							if (player.getLevel() < getMinLvl(getId()))
							{
								htmltext = "31740-2.htm";
								st.exitQuest(true);
							}
							else if (player.getLevel() >= getMinLvl(getId()))
							{
								htmltext = "31740-1.htm";
							}
						}
						else if (st.isCond(2))
						{
							htmltext = "31740-6.htm";
						}
					}
				}
			}
			else if ((npc.getId() == 31745) && st.isCond(2))
			{
				htmltext = "31745-1.htm";
			}
		}
		else
		{
			htmltext = "31740-0.htm";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _247_PossessorOfAPreciousSoul_4(247, _247_PossessorOfAPreciousSoul_4.class.getSimpleName(), "");
	}
}