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
public class _042_HelpTheUncle extends Quest
{
	public _042_HelpTheUncle(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30828);
		addTalkId(30828, 30735);

		addKillId(20068, 20266);
		
		questItemIds = new int[]
		{
		        7548, 7549
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
			htmltext = "30828-01.htm";
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("3") && st.getQuestItemsCount(291) > 0)
		{
			htmltext = "30828-03.htm";
			st.takeItems(291, 1);
			st.setCond(2, true);
		}
		else if (event.equalsIgnoreCase("4") && st.getQuestItemsCount(7548) >= 30)
		{
			htmltext = "30828-05.htm";
			st.takeItems(7548, 30);
			st.giveItems(7549, 1);
			st.setCond(4, true);
		}
		else if (event.equalsIgnoreCase("5") && st.getQuestItemsCount(7549) > 0)
		{
			htmltext = "30735-06.htm";
			st.takeItems(7549, 1);
			st.setCond(5, true);
		}
		else if(event.equalsIgnoreCase("7"))
		{
			if (st.isCond(5))
			{
				htmltext = "30828-07.htm";
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
		final int cond = st.getCond();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if(id == State.CREATED)
		{
			if (player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "30828-00.htm";
			}
			else
			{
				htmltext = "30828-00a.htm";
				st.exitQuest(true);
			}
		}
		else if(id == State.STARTED)
		{
			if (npcId == 30828)
			{
				if(cond == 1)
				{
					if(st.getQuestItemsCount(291) == 0)
					{
						htmltext = "30828-01a.htm";
					}
					else
					{
						htmltext = "30828-02.htm";
					}
				}
				else if(cond == 2)
				{
					htmltext = "30828-03a.htm";
				}
				else if(cond == 3)
				{
					htmltext = "30828-04.htm";
				}
				else if(cond == 4)
				{
					htmltext = "30828-05a.htm";
				}
				else if(cond == 5)
				{
					htmltext = "30828-06.htm";
				}
			}
			else if (npcId == 30735)
			{
				if(cond == 4 && st.getQuestItemsCount(7549) > 0)
				{
					htmltext = "30735-05.htm";
				}
				else if(cond == 5)
				{
					htmltext = "30735-06a.htm";
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
		if (st.calcDropItems(getId(), 7548, npc.getId(), 30))
		{
			st.setCond(3);
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _042_HelpTheUncle(42, _042_HelpTheUncle.class.getSimpleName(), "");
	}
}