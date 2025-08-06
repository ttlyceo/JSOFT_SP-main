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
 * Rework by LordWinter 29.03.2020
 */
public class _627_HeartInSearchOfPower extends Quest
{
	public _627_HeartInSearchOfPower(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31518);
		addTalkId(31518);
		addTalkId(31519);

		for(int mobs = 21520; mobs <= 21541; mobs++)
		{
			addKillId(mobs);
		}

		questItemIds = new int[]
		{
		        7171
		};
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

		if (event.equalsIgnoreCase("31518-1.htm") && npc.getId() == 31518)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31518-3.htm") && npc.getId() == 31518)
		{
			if (st.isCond(2))
			{
				st.takeItems(7171, 300);
				st.giveItems(7170, 1);
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("31519-1.htm") && npc.getId() == 31519)
		{
			if (st.isCond(3))
			{
				st.takeItems(7170, 1);
				st.giveItems(7172, 1);
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("31518-5.htm") && st.getQuestItemsCount(7172) == 1 && npc.getId() == 31518)
		{
			if (st.isCond(4))
			{
				st.takeItems(7172, 1);
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("31518-6.htm") && npc.getId() == 31518)
		{
			if (st.isCond(5))
			{
				st.calcReward(getId(), 1);
				st.exitQuest(true, true);
			}
		}
		else if (event.equalsIgnoreCase("31518-7.htm") && npc.getId() == 31518)
		{
			if (st.isCond(5))
			{
				st.calcReward(getId(), 2);
				st.exitQuest(true, true);
			}
		}
		else if (event.equalsIgnoreCase("31518-8.htm") && npc.getId() == 31518)
		{
			if (st.isCond(5))
			{
				st.calcReward(getId(), 3);
				st.exitQuest(true, true);
			}
		}
		else if (event.equalsIgnoreCase("31518-9.htm") && npc.getId() == 31518)
		{
			if (st.isCond(5))
			{
				st.calcReward(getId(), 4);
				st.exitQuest(true, true);
			}
		}
		else if (event.equalsIgnoreCase("31518-10.htm") && npc.getId() == 31518)
		{
			if (st.isCond(5))
			{
				st.calcReward(getId(), 5);
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

		final int npcId = npc.getId();
		final int cond = st.getCond();

		switch (st.getState())
		{
			case State.CREATED :
				if (npcId == 31518)
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "31518-0.htm";
					}
					else
					{
						htmltext = "31518-0a.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED :
				if (npcId == 31518)
				{
					if(cond == 1)
					{
						htmltext = "31518-1a.htm";
					}
					else if (st.getQuestItemsCount(7171) >= 300)
					{
						htmltext = "31518-2.htm";
					}
					else if (st.getQuestItemsCount(7172) > 0)
					{
						htmltext = "31518-4.htm";
					}
					else if(cond == 5)
					{
						htmltext = "31518-5.htm";
					}
				}
				else if (npcId == 31519 && st.getQuestItemsCount(7170) > 0)
				{
					htmltext = "31519-0.htm";
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (st.calcDropItems(getId(), 7171, npc.getId(), 300))
			{
				st.setCond(2);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _627_HeartInSearchOfPower(627, _627_HeartInSearchOfPower.class.getSimpleName(), "");
	}
}