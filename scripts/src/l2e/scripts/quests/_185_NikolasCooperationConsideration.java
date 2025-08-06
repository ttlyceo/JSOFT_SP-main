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
public final class _185_NikolasCooperationConsideration extends Quest
{
	public _185_NikolasCooperationConsideration(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30621);
		addTalkId(30621, 30673, 32366);

		questItemIds = new int[]
		{
		        10363, 10364, 10365
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
			case "30621-06.htm" :
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					qs.setMemoState(1);
					giveItems(player, 10365, 1);
					htmltext = event;
				}
				break;
			}
			case "30621-03.htm" :
			{
				if (player.getLevel() >= 40)
				{
					htmltext = event;
				}
				else
				{
					htmltext = "30621-03a.htm";
				}
				break;
			}
			case "30621-04.htm" :
			case "30621-05.htm" :
			{
				htmltext = event;
				break;
			}
			case "30673-02.htm" :
			{
				if (qs.isMemoState(1))
				{
					htmltext = event;
				}
				break;
			}
			case "30673-03.htm" :
			{
				if (qs.isMemoState(1))
				{
					takeItems(player, 10365, -1);
					qs.setMemoState(2);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "30673-05.htm" :
			{
				if (qs.isMemoState(2))
				{
					qs.setMemoState(3);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "30673-08.htm" :
			{
				if (qs.isMemoState(6))
				{
					htmltext = event;
				}
				break;
			}
			case "30673-09.htm" :
			{
				if (qs.isMemoState(6))
				{
					if (player.getLevel() < 46)
					{
						qs.calcExpAndSp(getId());
						qs.calcReward(getId());
					}
					else
					{
						qs.calcReward(getId());
					}
					
					if (hasQuestItems(player, 10363))
					{
						giveItems(player, 10362, 1);
						qs.exitQuest(false, true);
						htmltext = event;
					}
					else
					{
						htmltext = "30673-10.htm";
						qs.exitQuest(false, true);
					}
				}
				break;
			}
			case "32366-03.htm" :
			{
				if (qs.isMemoState(3) && !npc.getVariables().getBool("SPAWNED", false))
				{
					npc.getVariables().set("SPAWNED", true);
					npc.getVariables().set("PLAYER_ID", player.getObjectId());
					final Npc alarm = addSpawn(32367, player.getX() + 80, player.getY() + 60, player.getZ(), 16384, false, 0);
					alarm.getVariables().set("player0", player);
					alarm.getVariables().set("npc0", npc);
				}
				break;
			}
			case "32366-06.htm" :
			{
				if (qs.isMemoState(4))
				{
					giveItems(player, 10363, 1);
					qs.setMemoState(6);
					qs.setCond(4, true);
					htmltext = event;
				}
				break;
			}
			case "32366-08.htm" :
			{
				if (qs.isMemoState(5))
				{
					giveItems(player, 10364, 1);
					qs.setMemoState(6);
					qs.setCond(5, true);
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
			if (npc.getId() == 30621)
			{
				final QuestState q183 = player.getQuestState(_183_RelicExploration.class.getSimpleName());
				final QuestState q184 = player.getQuestState(_184_NikolasCooperationContract.class.getSimpleName());
				final QuestState q185 = player.getQuestState(_185_NikolasCooperationConsideration.class.getSimpleName());
				if ((q183 != null) && q183.isCompleted() && (q184 != null) && (q185 != null))
				{
					htmltext = (player.getLevel() >= getMinLvl(getId())) ? "30621-01.htm" : "30621-02.htm";
				}
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 30621 :
				{
					if (memoState == 1)
					{
						htmltext = "30621-07.htm";
					}
					break;
				}
				case 30673 :
				{
					if (memoState == 1)
					{
						htmltext = "30673-01.htm";
					}
					else if (memoState == 2)
					{
						htmltext = "30673-04.htm";
					}
					else if ((memoState >= 3) && (memoState <= 5))
					{
						htmltext = "30673-06.htm";
					}
					else if (memoState == 6)
					{
						htmltext = "30673-07.htm";
					}
					break;
				}
				case 32366 :
				{
					if (memoState == 3)
					{
						if (!npc.getVariables().getBool("SPAWNED", false))
						{
							htmltext = "32366-01.htm";
						}
						else if (npc.getVariables().getInteger("PLAYER_ID") == player.getObjectId())
						{
							htmltext = "32366-03.htm";
						}
						else
						{
							htmltext = "32366-04.htm";
						}
					}
					else if (memoState == 4)
					{
						htmltext = "32366-05.htm";
					}
					else if (memoState == 5)
					{
						htmltext = "32366-07.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == 30621)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _185_NikolasCooperationConsideration(185, _185_NikolasCooperationConsideration.class.getSimpleName(), "");
	}
}