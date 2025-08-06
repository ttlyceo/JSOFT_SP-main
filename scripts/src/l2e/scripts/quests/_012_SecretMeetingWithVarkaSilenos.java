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
 * Rework by LordWinter 05.12.2019
 */
public class _012_SecretMeetingWithVarkaSilenos extends Quest
{
	public _012_SecretMeetingWithVarkaSilenos(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31296);
		addTalkId(31296, 31258, 31378);
		
		questItemIds = new int[]
		{
		        7232
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
		
		if (event.equalsIgnoreCase("31296-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31258-02.htm"))
		{
			if (st.isCond(1))
			{
				st.giveItems(7232, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("31378-02.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(7232, 1);
				st.calcExpAndSp(getId());
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		String htmltext = getNoQuestMsg(player);
		
		final int cond = st.getCond();
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 31296)
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "31296-01.htm";
					}
					else
					{
						htmltext = "31296-02.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED:
				switch (npcId)
				{
					case 31296 :
						switch (cond)
						{
							case 1:
								htmltext = "31296-04.htm";
								break;
						}
						break;
					case 31258 :
						switch (cond)
						{
							case 1:
								htmltext = "31258-01.htm";
								break;
							case 2:
								htmltext = "31258-03.htm";
								break;
						}
						break;
					case 31378 :
						switch (cond)
						{
							case 2:
								if (st.getQuestItemsCount(7232) > 0)
								{
									htmltext = "31378-01.htm";
								}
								break;
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _012_SecretMeetingWithVarkaSilenos(12, _012_SecretMeetingWithVarkaSilenos.class.getSimpleName(), "");
	}
}