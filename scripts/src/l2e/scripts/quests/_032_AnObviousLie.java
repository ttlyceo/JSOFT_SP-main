
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
public class _032_AnObviousLie extends Quest
{
	public _032_AnObviousLie(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30120);
		addTalkId(30120, 30094, 31706);

		addKillId(20135);

		questItemIds = new int[]
		{
		        7166
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

		if (event.equalsIgnoreCase("30120-1.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("30094-1.htm"))
		{
			st.giveItems(7165, 1);
			st.setCond(2, false);
		}
		else if (event.equalsIgnoreCase("31706-1.htm"))
		{
			st.takeItems(7165, 1);
			st.setCond(3, false);
		}
		else if (event.equalsIgnoreCase("30094-4.htm"))
		{
			if (st.getQuestItemsCount(7166) == 20)
			{
				st.takeItems(7166, 20);
				st.setCond(5, false);
			}
			else
			{
				htmltext="no_items.htm";
				st.setCond(3, false);
			}
		}
		else if (event.equalsIgnoreCase("30094-7.htm"))
		{
			if (st.getQuestItemsCount(3031) >= 500)
			{
				st.takeItems(3031, 500);
				st.setCond(6, false);
			}
			else
			{
				htmltext="no_items.htm";
			}
		}
		else if (event.equalsIgnoreCase("31706-4.htm"))
		{
			st.setCond(7, false);
		}
		else if (event.equalsIgnoreCase("30094-10.htm"))
		{
			if (st.isCond(7))
			{
				st.setCond(8, false);
			}
		}
		else if (event.equalsIgnoreCase("30094-13.htm"))
		{
			if (st.getQuestItemsCount(1868) >= 1000 && st.getQuestItemsCount(1866) >= 500)
			{
				st.takeItems(1868, 1000);
				st.takeItems(1866, 500);
			}
			else
			{
				htmltext="no_items.htm";
			}
		}
		else if (event.equalsIgnoreCase("cat") || event.equalsIgnoreCase("racoon") || event.equalsIgnoreCase("rabbit"))
		{
			if (st.isCond(8))
			{
				if (event.equalsIgnoreCase("cat"))
				{
					st.calcReward(getId(), 1);
				}
				else if (event.equalsIgnoreCase("racoon"))
				{
					st.calcReward(getId(), 2);
				}
				else if (event.equalsIgnoreCase("rabbit"))
				{
					st.calcReward(getId(), 3);
				}
				st.exitQuest(false, true);
				htmltext = "30094-14.htm";
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
		
		switch(st.getState())
		{
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30120-0.htm";
				}
				else
				{
					htmltext = "30120-0a.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				switch (npc.getId())
				{
					case 30120 :
						if (cond == 1)
						{
							htmltext = "30120-2.htm";
						}
						break;
					case 30094 :
						switch (cond)
						{
							case 1:
								htmltext = "30094-0.htm";
								break;
							case 2:
								htmltext = "30094-2.htm";
								break;
							case 4:
								htmltext = "30094-3.htm";
								break;
							case 5:
								if (st.getQuestItemsCount(3031) < 500)
								{
									htmltext = "30094-5.htm";
								}
								else if (st.getQuestItemsCount(3031) >= 500)
								{
									htmltext = "30094-6.htm";
								}
								break;
							case 6:
								htmltext = "30094-8.htm";
								break;
							case 7:
								htmltext = "30094-9.htm";
								break;
							case 8:
								if (st.getQuestItemsCount(1868) < 1000 || st.getQuestItemsCount(1866) < 500)
								{
									htmltext = "30094-11.htm";
								}
								else if (st.getQuestItemsCount(1868) >= 1000 || st.getQuestItemsCount(1866) >= 500)
								{
									htmltext = "30094-12.htm";
								}
								break;
						}
						break;
					case 31706 :
						switch (cond)
						{
							case 2:
								htmltext = "31706-0.htm";
								break;
							case 3:
								htmltext = "31706-2.htm";
								break;
							case 6:
								htmltext = "31706-3.htm";
								break;
							case 7:
								htmltext = "31706-5.htm";
								break;
						}
						break;
				}
				break;
		}
		return htmltext;
	}


	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 3);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st.calcDropItems(getId(), 7166, npc.getId(), 20))
		{
			st.setCond(4);
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _032_AnObviousLie(32, _032_AnObviousLie.class.getSimpleName(), "");
	}
}