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
 * Rework by LordWinter 18.09.2020
 */
public final class _696_ConquertheHallofErosion extends Quest
{
	public _696_ConquertheHallofErosion(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32603);
		addTalkId(32603);

		addKillId(25634);
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

		if (event.equalsIgnoreCase("32603-02.htm"))
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
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					if (st.getQuestItemsCount(13691) > 0 || st.getQuestItemsCount(13692) > 0)
					{
						htmltext = "32603-01.htm";
					}
					else
					{
						htmltext = "32603-05.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "32603-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if(st.getInt("cohemenesDone") != 0)
				{
					if (st.getQuestItemsCount(13692) < 1)
					{
						st.takeItems(13691, 1);
						st.calcReward(getId());
					}
					htmltext = "32603-04.htm";
					st.exitQuest(true, true);
				}
				else
				{
					htmltext = "32603-01a.htm";
				}
				break;
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st == null)
		{
			return null;
		}

		if (st.isCond(1))
		{
			st.set("cohemenesDone", 1);
		}

		if (player.getParty() != null)
		{
			QuestState st2;
			for (final Player pmember : player.getParty().getMembers())
			{
				st2 = pmember.getQuestState(getName());
				if ((st2 != null) && (st2.isCond(1)) && (pmember.getObjectId() != partyMember.getObjectId()))
				{
					st.set("cohemenesDone", 1);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
  	}

	public static void main(String[] args)
	{
		new _696_ConquertheHallofErosion(696, _696_ConquertheHallofErosion.class.getSimpleName(), "");
	}
}