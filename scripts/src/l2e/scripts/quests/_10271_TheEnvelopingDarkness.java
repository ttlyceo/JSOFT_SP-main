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
 * Rework by LordWinter 15.12.2019
 */
public class _10271_TheEnvelopingDarkness extends Quest
{
	public _10271_TheEnvelopingDarkness(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32560);
		addTalkId(32560, 32556, 32528);

		questItemIds = new int[]
		{
		        13852
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

		if (event.equalsIgnoreCase("32560-02.htm") && npc.getId() == 32560)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32556-02.htm") && npc.getId() == 32556)
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("32556-05.htm") && npc.getId() == 32556)
		{
			if (st.isCond(3))
			{
				st.takeItems(13852, 1);
				st.setCond(4, true);
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

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (npc.getId() == 32560)
		{
			if (st.getCond() == 0)
			{
				final QuestState _prev = player.getQuestState("_10269_ToTheSeedOfDestruction");
				if ((_prev != null) && (_prev.getState() == State.COMPLETED) && (player.getLevel() >= getMinLvl(getId())))
				{
					htmltext = "32560-01.htm";
				}
				else
				{
					htmltext = "32560-00.htm";
				}
			}
			else if (st.getCond() >= 1 && st.getCond() < 4)
			{
				htmltext = "32560-03.htm";
			}
			else if (st.isCond(4))
			{
				htmltext = "32560-04.htm";
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		else if (npc.getId() == 32556)
		{
			if (st.isCond(1))
			{
				htmltext = "32556-01.htm";
			}
			else if (st.isCond(2))
			{
				htmltext = "32556-03.htm";
			}
			else if (st.isCond(3))
			{
				htmltext = "32556-04.htm";
			}
			else if (st.isCond(4))
			{
				htmltext = "32556-06.htm";
			}
		}
		else if (npc.getId() == 32528)
		{
			if (st.isCond(2))
			{
				htmltext = "32528-01.htm";
				st.giveItems(13852, 1);
				st.setCond(3, true);
			}
			else if (st.isCond(3))
			{
				htmltext = "32528-02.htm";
			}
	 	}
		return htmltext;
  	}
	
	public static void main(String[] args)
	{
		new _10271_TheEnvelopingDarkness(10271, _10271_TheEnvelopingDarkness.class.getSimpleName(), "");
	}
}