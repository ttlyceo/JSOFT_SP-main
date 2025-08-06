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
public final class _189_ContractCompletion extends Quest
{
	public _189_ContractCompletion(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31437);
		addTalkId(31437, 30512, 30673, 30068);

		questItemIds = new int[]
		{
		        10370
		};
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
			case "31437-03.htm" :
			{
				if (qs.isCreated() && npc.getId() == 31437)
				{
					qs.startQuest();
					qs.setMemoState(1);
					giveItems(player, 10370, 1);
					htmltext = event;
				}
				break;
			}
			case "30512-02.htm" :
			{
				if (qs.isMemoState(4) && npc.getId() == 30512)
				{
					if (player.getLevel() < 48)
					{
						qs.calcExpAndSp(getId());
					}
					qs.calcReward(getId());
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
			case "30673-02.htm" :
			{
				if (qs.isMemoState(1) && npc.getId() == 30673)
				{
					qs.setMemoState(2);
					qs.setCond(2, true);
					takeItems(player, 10370, -1);
					htmltext = event;
				}
				break;
			}
			case "30068-02.htm" :
			{
				if (qs.isMemoState(2) && npc.getId() == 30068)
				{
					htmltext = event;
				}
				break;
			}
			case "30068-03.htm" :
			{
				if (qs.isMemoState(2) && npc.getId() == 30068)
				{
					qs.setMemoState(3);
					qs.setCond(3, true);
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
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			if (npc.getId() == 31437)
			{
				final QuestState q186 = player.getQuestState(_186_ContractExecution.class.getSimpleName());
				if ((q186 != null) && q186.isCompleted())
				{
					htmltext = (player.getLevel() >= getMinLvl(getId())) ? "31437-01.htm" : "31437-02.htm";
				}
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 31437 :
				{
					if (qs.getMemoState() >= 1)
					{
						htmltext = "31437-04.htm";
					}
					break;
				}
				case 30512 :
				{
					if (qs.isMemoState(4))
					{
						htmltext = "30512-01.htm";
					}
					break;
				}
				case 30673 :
				{
					switch (qs.getCond())
					{
						case 1 :
						{
							htmltext = "30673-01.htm";
							break;
						}
						case 2 :
						{
							htmltext = "30673-03.htm";
							break;
						}
						case 3 :
						{
							qs.setMemoState(4);
							qs.setCond(4, true);
							htmltext = "30673-04.htm";
							break;
						}
						case 4 :
						{
							htmltext = "30673-05.htm";
							break;
						}
					}
					break;
				}
				case 30068 :
				{
					switch (qs.getCond())
					{
						case 2 :
						{
							htmltext = "30068-01.htm";
							break;
						}
						case 3 :
						{
							htmltext = "30068-04.htm";
							break;
						}
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == 31437)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _189_ContractCompletion(189, _189_ContractCompletion.class.getSimpleName(), "");
	}
}