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
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 14.11.2021
 */
public class _360_PlunderTheirSupplies extends Quest
{
	public _360_PlunderTheirSupplies(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30873);
		addTalkId(30873);

		addKillId(20666, 20669);

		questItemIds = new int[]
		{
		        5870, 5872, 5871
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
		
		if (event.equalsIgnoreCase("30873-2.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("30873-6.htm"))
		{
			st.takeItems(5872, -1);
			st.takeItems(5871, -1);
			st.takeItems(5870, -1);
			st.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()) && player.getLevel() <= getMaxLvl(getId()))
				{
					htmltext = "30873-0.htm";
				}
				else
				{
					htmltext = "30873-0a.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if (st.getQuestItemsCount(5872) == 0)
				{
					htmltext = "30873-3.htm";
				}
				else
				{
					final ItemHolder supplyItems = st.calcRewardPerItemHolder(getId(), 1, (int) st.getQuestItemsCount(5872));
					final ItemHolder supplyReceipt = st.calcRewardPerItemHolder(getId(), 2, (int) st.getQuestItemsCount(5870));
					if (supplyItems != null && supplyReceipt != null)
					{
						if (supplyItems.getId() == supplyReceipt.getId())
						{
							final long count = supplyItems.getCount() + supplyReceipt.getCount();
							if (count > 0)
							{
								st.giveItems(supplyItems.getId(), 6000 + count);
							}
						}
					}
					else
					{
						if (supplyItems.getCount() > 0)
						{
							st.giveItems(supplyItems.getId(), 6000 + supplyItems.getCount());
						}
						if (supplyReceipt.getCount() > 0)
						{
							st.giveItems(supplyReceipt.getId(), supplyReceipt.getCount());
						}
					}
					st.takeItems(5872, -1);
					st.takeItems(5870, -1);
					htmltext = "30873-5.htm";
				}
				break;
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
		if (st != null)
		{
			st.calcDropItems(getId(), 5872, npc.getId(), Integer.MAX_VALUE);
			st.calcDropItems(getId(), 5871, npc.getId(), Integer.MAX_VALUE);
			final long totalAmount = st.getQuestItemsCount(5871);
			if (totalAmount >= 5)
			{
				final int amount = (int) (totalAmount / 5);
				if (amount > 0)
				{
					st.takeItems(5871, (amount * 5));
					st.giveItems(5870, amount);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _360_PlunderTheirSupplies(360, _360_PlunderTheirSupplies.class.getSimpleName(), "");
	}
}