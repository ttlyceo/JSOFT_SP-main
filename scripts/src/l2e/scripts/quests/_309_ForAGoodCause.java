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
 * Rework by LordWinter 17.06.2023
 */
public final class _309_ForAGoodCause extends Quest
{
	public _309_ForAGoodCause(int questID, String name, String description)
	{
		super(questID, name, description);
		
		addStartNpc(32647);
		addTalkId(32647);
		
		addKillId(22650, 22651, 22652, 22653, 22654, 22655);
	}
	
	private String onExchangeRequest(QuestState questState, int exchangeID)
	{
		String resultHtmlText = "32647-13.htm";
		
		long fallenMucrokianHideCount = questState.getQuestItemsCount(14874);
		if (fallenMucrokianHideCount > 0)
		{
			questState.takeItems(14874, fallenMucrokianHideCount);
			questState.giveItems(14873, fallenMucrokianHideCount * 2);
			fallenMucrokianHideCount = 0;
		}
		
		final long mucrokianHideCount = questState.getQuestItemsCount(14873);
		if (exchangeID == 240 && mucrokianHideCount >= 240)
		{
			questState.takeItems(14873, 240);
			questState.calcReward(getId(), 1, true);
			resultHtmlText = "32647-14.htm";
		}
		else if (exchangeID == 180 && mucrokianHideCount >= 180)
		{
			questState.takeItems(14873, 180);
			questState.calcReward(getId(), 2, true);
			resultHtmlText = "32647-14.htm";
		}
		return resultHtmlText;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32647-05.htm"))
		{
			if (st.isCreated() && player.getLevel() >= getMinLvl(getId()))
			{
				final QuestState prev = player.getQuestState("_308_ReedFieldMaintenance");
				if (prev != null && prev.isStarted())
				{
					htmltext = "32647-15.htm";
				}
				else
				{
					st.startQuest();
				}
			}
		}
		else if (event.equalsIgnoreCase("32647-12.htm") || event.equalsIgnoreCase("32647-07.htm"))
		{
			st.exitQuest(true);
			st.playSound("ItemSound.quest_finish");
		}
		else if (event.equalsIgnoreCase("claimreward"))
		{
			htmltext = "32647-09.htm";
		}
		else
		{
			int exchangeID = 0;
			try
			{
				exchangeID = Integer.parseInt(event);
			}
			catch (final Exception e)
			{
				exchangeID = 0;
			}
			
			if (exchangeID > 0)
			{
				htmltext = onExchangeRequest(st, exchangeID);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		String htmltext = getNoQuestMsg(talker);
		final var questState = talker.getQuestState(getName());
		if (questState != null)
		{
			final int cond = questState.getCond();
			
			final QuestState prev = talker.getQuestState("_308_ReedFieldMaintenance");
			if (prev != null && prev.isStarted())
			{
				htmltext = "32647-15.htm";
			}
			else if (cond == 0)
			{
				if (talker.getLevel() < getMinLvl(getId()))
				{
					htmltext = "32647-00.htm";
					questState.exitQuest(true);
				}
				else
				{
					htmltext = "32647-01.htm";
				}
			}
			else if (State.STARTED == questState.getState())
			{
				if (questState.getQuestItemsCount(14873) >= 1 || questState.getQuestItemsCount(14874) >= 1)
				{
					htmltext = "32647-08.htm";
				}
				else
				{
					htmltext = "32647-06.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (null == partyMember)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final var st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (npc.getId() == 22655)
			{
				st.calcDropItems(getId(), 14874, npc.getId(), Integer.MAX_VALUE);
			}
			else
			{
				st.calcDropItems(getId(), 14873, npc.getId(), Integer.MAX_VALUE);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _309_ForAGoodCause(309, _309_ForAGoodCause.class.getSimpleName(), "");
	}
}