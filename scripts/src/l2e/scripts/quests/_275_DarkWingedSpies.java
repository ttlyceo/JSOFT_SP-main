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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 09.05.2023
 */
public class _275_DarkWingedSpies extends Quest
{
	public _275_DarkWingedSpies(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30567);
		addTalkId(30567);
		
		addKillId(20316, 27043);

		questItemIds = new int[]
		{
		        1478, 1479
		};
	}
			
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30567-03.htm") && player.getRace().ordinal() == 3)
		{
			if (st.isCreated() && player.getLevel() >= getMinLvl(getId()) && player.getLevel() <= getMaxLvl(getId()))
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
		final var st = player.getQuestState(getName());
		if(st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getRace().ordinal() == 3)
				{
					if (player.getLevel() >= getMinLvl(getId()) && player.getLevel() <= getMaxLvl(getId()))
					{
						htmltext = "30567-02.htm";
					}
					else
					{
						htmltext = "30567-01.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30567-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				if (st.getQuestItemsCount(1478) < 70)
				{
					htmltext = "30567-04.htm";
				}
				else
				{
					htmltext = "30567-05.htm";
					st.takeItems(1478, -1);
					st.takeItems(1479, -1);
					st.calcReward(getId());
					st.exitQuest(true, true);
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
		
		final var st = partyMember.getQuestState(getName());
		if (st != null)
		{
			final var amount = st.getQuestItemsCount(1478);
			switch (npc.getId())
			{
				case 20316 :
					if (amount < 69 && Rnd.chance(10))
					{
						st.giveItems(1479, 1);
						st.addSpawn(27043, npc);
					}
					
					if (st.isCond(1) && st.calcDropItems(getId(), 1478, npc.getId(), 70))
					{
						st.setCond(2);
					}
					break;
				case 27043 :
					if (st.getQuestItemsCount(1479) == 1 && st.isCond(1))
					{
						st.takeItems(1479, 1);
						if (st.calcDropItems(getId(), 1478, npc.getId(), 70))
						{
							st.setCond(2);
						}
					}
					break;
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _275_DarkWingedSpies(275, _275_DarkWingedSpies.class.getSimpleName(), "");
	}
}