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

/**
 * Rework by LordWinter 28.07.2020
 */
public final class _190_LostDream extends Quest
{
	public _190_LostDream(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30512);
		addTalkId(30512, 30673, 30621, 30113);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "30512-03.htm" :
			{
				if (qs.isCreated() && npc.getId() == 30512)
				{
					qs.startQuest();
					qs.setMemoState(1);
					htmltext = event;
				}
				break;
			}
			case "30512-06.htm" :
			{
				if (qs.isMemoState(2) && npc.getId() == 30512)
				{
					qs.setMemoState(3);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "30113-02.htm" :
			{
				if (qs.isMemoState(1) && npc.getId() == 30113)
				{
					htmltext = event;
				}
				break;
			}
			case "30113-03.htm" :
			{
				if (qs.isMemoState(1) && npc.getId() == 30113)
				{
					qs.setMemoState(2);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		final int memoState = qs.getMemoState();
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			if (npc.getId() == 30512)
			{
				final QuestState q187 = player.getQuestState(_187_NikolasHeart.class.getSimpleName());
				if ((q187 != null) && q187.isCompleted())
				{
					htmltext = (player.getLevel() >= getMinLvl(getId())) ? "30512-01.htm" : "30512-02.htm";
				}
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 30512 :
				{
					if (memoState == 1)
					{
						htmltext = "30512-04.htm";
					}
					else if (memoState == 2)
					{
						htmltext = "30512-05.htm";
					}
					else if ((memoState >= 3) && (memoState <= 4))
					{
						htmltext = "30512-07.htm";
					}
					else if (memoState == 5)
					{
						htmltext = "30512-08.htm";
						if (player.getLevel() < 48)
						{
							qs.calcExpAndSp(getId());
						}
						qs.calcReward(getId());
						qs.exitQuest(false, true);
					}
					break;
				}
				case 30113 :
				{
					if (memoState == 1)
					{
						htmltext = "30113-01.htm";
					}
					else if (memoState == 2)
					{
						htmltext = "30113-04.htm";
					}
					break;
				}
				case 30621 :
				{
					if (memoState == 4)
					{
						qs.setMemoState(5);
						qs.setCond(5, true);
						htmltext = "30621-01.htm";
					}
					else if (memoState == 5)
					{
						htmltext = "30621-02.htm";
					}
					break;
				}
				case 30673 :
				{
					if (memoState == 3)
					{
						qs.setMemoState(4);
						qs.setCond(4, true);
						htmltext = "30673-01.htm";
					}
					else if (memoState == 4)
					{
						htmltext = "30673-02.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == 30512)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _190_LostDream(190, _190_LostDream.class.getSimpleName(), "");
	}
}