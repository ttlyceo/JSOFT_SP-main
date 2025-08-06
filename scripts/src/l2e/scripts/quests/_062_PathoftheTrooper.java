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
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Rework by LordWinter 13.12.2019
 */
public class _062_PathoftheTrooper extends Quest
{
	public _062_PathoftheTrooper(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32197);
		addTalkId(32197, 32194);

		addKillId(20014, 20038, 20062);

		questItemIds = new int[]
		{
		        9749, 9750, 9752, 9751
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if(event.equalsIgnoreCase("32197-02.htm"))
		{
			st.startQuest();
		}
		else if(event.equalsIgnoreCase("32194-02.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
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

		if (id == State.COMPLETED)
		{
			htmltext = "32197-07.htm";
		}

		if (npcId == 32197)
		{
			if (id == State.CREATED)
			{
				if(player.getClassId() != ClassId.maleSoldier)
				{
					htmltext = "32197-00b.htm";
					st.exitQuest(false);
				}
				else if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "32197-00a.htm";
					st.exitQuest(false);
				}
				else
				{
					htmltext = "32197-01.htm";
				}
			}
			else if(cond < 4)
			{
				htmltext = "32197-03.htm";
			}
			else if(cond == 4)
			{
				st.takeItems(9752, -1);
				st.setCond(5, true);
				htmltext = "32197-04.htm";
			}
			else if(cond == 5)
			{
				if (st.getQuestItemsCount(9751) < 1)
				{
					htmltext = "32197-05.htm";
				}
				else
				{
					st.takeItems(9751, -1);
					st.giveItems(9753, 1);
					final String isFinished = st.getGlobalQuestVar("1ClassQuestFinished");
					if (isFinished.equals(""))
					{
						st.calcReward(getId());
						st.calcExpAndSp(getId());
						st.saveGlobalQuestVar("1ClassQuestFinished", "1");
					}
					st.exitQuest(false, true);
					player.sendPacket(new SocialAction(player.getObjectId(), 3));
					htmltext = "32197-06.htm";
				}
			}
		}
		else if (npcId == 32194)
		{
			if(cond == 1)
			{
				htmltext = "32194-01.htm";
			}
			else if(cond == 2)
			{
				if (st.getQuestItemsCount(9749) < 5)
				{
					htmltext = "32194-03.htm";
				}
				else
				{
					st.takeItems(9749, -1);
					st.setCond(3, true);
					htmltext = "32194-04.htm";
				}
			}
			else if(cond == 3)
			{
				if (st.getQuestItemsCount(9750) < 10)
				{
					htmltext = "32194-05.htm";
				}
				else
				{
					st.takeItems(9750, -1);
					st.giveItems(9752, 1);
					st.setCond(4, true);
					htmltext = "32194-06.htm";
				}
			}
			else if (cond > 3)
			{
				htmltext = "32194-07.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		npc.getId();
		st.getCond();

		if (st.isCond(2))
		{
			if (npc.getId() == 20014)
			{
				st.calcDoDropItems(getId(), 9749, npc.getId(), 5);
				final long count = st.getQuestItemsCount(9749);
				if (count >= 5)
				{
					st.playSound("ItemSound.quest_middle");
				}
			}
		}
		
		if (st.isCond(3))
		{
			if (npc.getId() == 20038)
			{
				st.calcDoDropItems(getId(), 9750, npc.getId(), 10);
				final long count = st.getQuestItemsCount(9749);
				if (count >= 10)
				{
					st.playSound("ItemSound.quest_middle");
				}
			}
		}
		
		if (st.isCond(5))
		{
			if (npc.getId() == 20062)
			{
				st.calcDoDropItems(getId(), 9751, npc.getId(), 1);
				final long count = st.getQuestItemsCount(9749);
				if (count >= 1)
				{
					st.playSound("ItemSound.quest_middle");
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _062_PathoftheTrooper(62, _062_PathoftheTrooper.class.getSimpleName(), "");
	}
}