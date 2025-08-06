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
 * Rework by LordWinter 19.04.2020
 */
public final class _187_NikolasHeart extends Quest
{
	public _187_NikolasHeart(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30673);
		addTalkId(30673, 30512, 30621);

		questItemIds = new int[]
		{
		        10368
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
				
		if (event.equalsIgnoreCase("30673-02.htm") && npc.getId() == 30673)
		{
			if (st.isCreated())
			{
				st.startQuest();
				st.takeItems(10362, -1);
				st.giveItems(10368, 1);
			}
		}
		else if (event.equalsIgnoreCase("30621-03.htm") && npc.getId() == 30621)
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30512-03.htm") && npc.getId() == 30512)
		{
			if (st.isCond(2))
			{
				if (player.getLevel() < 47)
				{
					st.calcExpAndSp(getId());
				}
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
		if(st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final int cond = st.getCond();
		
		switch (st.getState())
		{
			case State.CREATED:
				final QuestState qs = player.getQuestState("_185_NikolasCooperationConsideration");
				if (npcId == 30673 && ((qs != null) && qs.isCompleted() && hasQuestItems(player, 10362)))
				{
					if (player.getLevel() < getMinLvl(getId()))
					{
						htmltext = "30673-00.htm";
					}
					else
					{
						htmltext = "30673-01.htm";
					}
				}
				break;
			case State.STARTED:
				if (npcId == 30673)
				{
					if(cond == 1)
					{
						htmltext = "30673-03.htm";
					}
				}
				else if (npcId == 30621)
				{
					if (cond == 1)
					{
						htmltext = "30621-01.htm";
					}
					else if(cond == 2)
					{
						htmltext = "30621-04.htm";
					}
				}
				else if (npcId == 30512)
				{
					if (cond == 2)
					{
						htmltext = "30512-01.htm";
					}
				}
				break;
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
		
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _187_NikolasHeart(187, _187_NikolasHeart.class.getSimpleName(), "");
	}
}