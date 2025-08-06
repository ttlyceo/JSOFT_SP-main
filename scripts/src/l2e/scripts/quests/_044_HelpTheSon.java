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
 * Rework by LordWinter 08.12.2019
 */
public class _044_HelpTheSon extends Quest
{
	public _044_HelpTheSon(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30827);
		addTalkId(30827, 30505);

		addKillId(20921, 20920, 20919);

		questItemIds = new int[]
		{
		        7553, 7552
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

		if(event.equalsIgnoreCase("1"))
		{
			htmltext = "30827-01.htm";
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("3") && st.getQuestItemsCount(168) > 0)
		{
			htmltext = "30827-03.htm";
			st.takeItems(168, 1);
			st.setCond(2, true);
		}
		else if (event.equalsIgnoreCase("4") && st.getQuestItemsCount(7552) >= 30)
		{
			if (st.isCond(3))
			{
				htmltext = "30827-05.htm";
				st.takeItems(7552, 30);
				st.giveItems(7553, 1);
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("5") && st.getQuestItemsCount(7553) > 0)
		{
			if (st.isCond(4))
			{
				htmltext = "30505-06.htm";
				st.takeItems(7553, 1);
				st.setCond(5, true);
			}
		}
		else if(event.equalsIgnoreCase("7"))
		{
			if (st.isCond(5))
			{
				htmltext = "30827-07.htm";
				st.calcReward(getId());
				st.exitQuest(false, true);
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
		final byte id = st.getState();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if(id == State.CREATED)
		{
			if (player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "30827-00.htm";
			}
			else
			{
				st.exitQuest(true);
				htmltext = "30827-00a.htm";
			}
		}
		else if(id == State.STARTED)
		{
			final int cond = st.getCond();
			if (npcId == 30827)
			{
				if(cond == 1)
				{
					if (st.getQuestItemsCount(168) == 0)
					{
						htmltext = "30827-01a.htm";
					}
					else
					{
						htmltext = "30827-02.htm";
					}
				}
				else if(cond == 2)
				{
					htmltext = "30827-03a.htm";
				}
				else if(cond == 3)
				{
					htmltext = "30827-04.htm";
				}
				else if(cond == 4)
				{
					htmltext = "30827-05a.htm";
				}
				else if(cond == 5)
				{
					htmltext = "30827-06.htm";
				}
			}
			else if (npcId == 30505)
			{
				if(cond == 4 && st.getQuestItemsCount(7553) > 0)
				{
					htmltext = "30505-05.htm";
				}
				else if(cond == 5)
				{
					htmltext = "30505-06a.htm";
				}
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 2);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st.calcDropItems(getId(), 7552, npc.getId(), 30))
		{
			st.setCond(3);
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _044_HelpTheSon(44, _044_HelpTheSon.class.getSimpleName(), "");
	}
}