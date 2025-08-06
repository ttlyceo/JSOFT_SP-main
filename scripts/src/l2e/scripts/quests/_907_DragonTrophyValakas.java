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
 * Rework by LordWinter 08.02.2020
 */
public class _907_DragonTrophyValakas extends Quest
{
	public _907_DragonTrophyValakas(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31540);
		addTalkId(31540);

		addKillId(29028);
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

		if(event.equalsIgnoreCase("31540-04.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if(event.equalsIgnoreCase("31540-07.htm"))
		{
			if (st.isCond(2))
			{
				st.calcReward(getId());
				st.exitQuest(true, true);
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

		final int cond = st.getCond();

		if (npc.getId() == 31540)
		{
			switch(st.getState())
			{
				case State.CREATED:
					if (player.getLevel() >= getMinLvl(getId()))
					{
						if(st.getQuestItemsCount(7267) > 0)
						{
							htmltext = "31540-01.htm";
						}
						else
						{
							htmltext = "31540-00b.htm";
						}
					}
					else
					{
						htmltext = "31540-00.htm";
						st.exitQuest(true);
					}
					break;
				case State.STARTED:
					if(cond == 1)
					{
						htmltext = "31540-05.htm";
					}
					else if(cond == 2)
					{
						htmltext = "31540-06.htm";
					}
					break;
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == 29028)
		{
			if (player.getParty() != null)
			{
				if (player.getParty().getCommandChannel() != null)
				{
					for (final Player ccMember : player.getParty().getCommandChannel())
					{
						if (ccMember == null || !ccMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2))
						{
							continue;
						}
						rewardPlayer(ccMember);
					}
				}
				else
				{
					for (final Player partyMember : player.getParty().getMembers())
					{
						if (partyMember == null || !partyMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2))
						{
							continue;
						}
						rewardPlayer(partyMember);
					}
				}
			}
			else
			{
				rewardPlayer(player);
			}
		}
		return null;
	}

	private void rewardPlayer(Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st != null && st.isCond(1))
		{
			st.setCond(2, true);
		}
	}

	public static void main(String[] args)
	{
		new _907_DragonTrophyValakas(907, _907_DragonTrophyValakas.class.getSimpleName(), "");
	}
}