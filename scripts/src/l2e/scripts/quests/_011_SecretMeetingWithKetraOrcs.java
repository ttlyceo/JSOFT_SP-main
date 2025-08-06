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
public class _011_SecretMeetingWithKetraOrcs extends Quest
{
	public _011_SecretMeetingWithKetraOrcs(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31296);
		addTalkId(31296, 31256, 31371);
		
		questItemIds = new int[]
		{
		        7231
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
		
		if(event.equalsIgnoreCase("31296-03.htm"))
		{
			st.startQuest();
		}
		else if(event.equalsIgnoreCase("31256-02.htm"))
		{
			if (st.isCond(1))
			{
				st.giveItems(7231, 1);
				st.setCond(2, true);
			}
		}
		else if(event.equalsIgnoreCase("31371-02.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(7231, 1);
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
					case 31256 :
						switch (cond)
						{
							case 1:
								htmltext = "31256-01.htm";
								break;
							case 2:
								htmltext = "31256-03.htm";
								break;
						}
						break;
					case 31371 :
						switch (cond)
						{
							case 2:
								if (st.getQuestItemsCount(7231) > 0)
								{
									htmltext = "31371-01.htm";
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
		new _011_SecretMeetingWithKetraOrcs(11, _011_SecretMeetingWithKetraOrcs.class.getSimpleName(), "");
	}
}