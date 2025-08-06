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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 25.03.2020
 */
public class _10503_FrintezzaEmbroideredSoulCloak extends Quest
{
	public _10503_FrintezzaEmbroideredSoulCloak(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32612);
		addTalkId(32612);
		
		addKillId(29047);
		
		questItemIds = new int[]
		{
		        21724
		};
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
		
		if (event.equalsIgnoreCase("32612-01.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32612-03.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(21724, -1);
				st.calcReward(getId());
				st.exitQuest(false, true);
				htmltext = "32612-reward.htm";
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
		
		switch (st.getState())
		{
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED :
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "32612-level_error.htm";
				}
				else
				{
					htmltext = "32612-00.htm";
				}
				break;
			case State.STARTED :
				switch (st.getCond())
				{
					case 1 :
						htmltext = "32612-error.htm";
						break;
					case 2 :
						htmltext = "32612-02.htm";
						break;
				}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == 29047)
		{
			if (player.getParty() != null)
			{
				if (player.getParty().getCommandChannel() != null)
				{
					for (final Player ccMember : player.getParty().getCommandChannel())
					{
						if (ccMember == null || !ccMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2) || ccMember.getReflectionId() != player.getReflectionId())
						{
							continue;
						}
						rewardPlayer(ccMember, npc.getId());
					}
				}
				else
				{
					for (final Player partyMember : player.getParty().getMembers())
					{
						if (partyMember == null || !partyMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2) || partyMember.getReflectionId() != player.getReflectionId())
						{
							continue;
						}
						rewardPlayer(partyMember, npc.getId());
					}
				}
			}
			else
			{
				rewardPlayer(player, npc.getId());
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	private void rewardPlayer(Player player, int npcId)
	{
		final QuestState st = player.getQuestState(getName());
		if (st != null)
		{
			if (st.isCond(1) && st.calcDropItems(getId(), 21724, npcId, 20))
			{
				st.setCond(2, true);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new _10503_FrintezzaEmbroideredSoulCloak(10503, _10503_FrintezzaEmbroideredSoulCloak.class.getSimpleName(), "");
	}
}