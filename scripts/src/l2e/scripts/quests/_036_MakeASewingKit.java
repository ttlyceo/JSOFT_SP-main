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
 * Rework by LordWinter 06.12.2019
 */
public class _036_MakeASewingKit extends Quest
{
	public _036_MakeASewingKit(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(30847);
		addTalkId(30847);
		
		addKillId(20566);
		
		questItemIds = new int[]
		{
		        7163
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int cond = st.getCond();
		
		if (event.equalsIgnoreCase("30847-1.htm") && cond == 0)
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30847-3.htm") && cond == 2)
		{
			st.takeItems(7163, 5);
			st.setCond(3, true);
		}
		else if (event.equalsIgnoreCase("30847-4a.htm") && cond == 3)
		{
			if ((st.getQuestItemsCount(1893) >= 10) && (st.getQuestItemsCount(1891) >= 10))
			{
				st.takeItems(1893, 10);
				st.takeItems(1891, 10);
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
			else
			{
				htmltext = "30847-4b.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());

		final int cond = st.getCond();

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}

		if (cond == 0 && st.getQuestItemsCount(7078) == 0)
		{
			if (player.getLevel() >= getMinLvl(getId()))
			{
				final QuestState fwear = player.getQuestState("_037_PleaseMakeMeFormalWear");
				if (fwear != null && fwear.getState() == State.STARTED)
				{
					if (fwear.get("cond").equals("6"))
					{
						htmltext = "30847-0.htm";
					}
					else
					{
						htmltext = "30847-5.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30847-5.htm";
					st.exitQuest(true);
				}
			}
			else
			{
				htmltext = "30847-5.htm";
			}
		}
		else if (cond == 1 && st.getQuestItemsCount(7163) < 5)
		{
			htmltext = "30847-1a.htm";
		}
		else if (cond == 2 && st.getQuestItemsCount(7163) == 5)
		{
			htmltext = "30847-2.htm";
		}
		else if (cond == 3 && st.getQuestItemsCount(1893) >= 10 && st.getQuestItemsCount(1891) >= 10)
		{
			htmltext = "30847-4.htm";
		}
		else
		{
			htmltext = "30847-3a.htm";
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null && st.calcDropItems(getId(), 7163, npc.getId(), 5))
		{
			st.setCond(2);
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _036_MakeASewingKit(36, _036_MakeASewingKit.class.getSimpleName(), "");
	}
}