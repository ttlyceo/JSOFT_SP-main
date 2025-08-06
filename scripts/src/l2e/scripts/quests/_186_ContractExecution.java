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

import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;

/**
 * Rework by LordWinter 28.07.2020
 */
public final class _186_ContractExecution extends Quest
{
	public _186_ContractExecution(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30673);
		addTalkId(30673, 31437, 30621);

		for(int mobs = 20577; mobs <= 20583; mobs++)
		{
			addKillId(mobs);
		}

		questItemIds = new int[]
		{
		        10366, 10367
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
			case "30673-03.htm" :
			{
				if ((player.getLevel() >= getMinLvl(getId())) && hasQuestItems(player, 10362))
				{
					qs.startQuest();
					qs.setMemoState(1);
					giveItems(player, 10366, 1);
					takeItems(player, 10362, -1);
					htmltext = event;
				}
				break;
			}
			case "30621-02.htm" :
			{
				if (qs.isMemoState(1))
				{
					htmltext = event;
				}
				break;
			}
			case "30621-03.htm" :
			{
				if (qs.isMemoState(1))
				{
					qs.setMemoState(2);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "31437-03.htm" :
			{
				if (qs.isMemoState(2) && hasQuestItems(player, 10367))
				{
					htmltext = event;
				}
				break;
			}
			case "31437-04.htm" :
			{
				if (qs.isMemoState(2) && hasQuestItems(player, 10367))
				{
					qs.setMemoState(3);
					htmltext = event;
				}
				break;
			}
			case "31437-06.htm" :
			{
				if (qs.isMemoState(3))
				{
					if (player.getLevel() < 47)
					{
						qs.calcExpAndSp(getId());
					}
					qs.calcReward(getId());
					qs.exitQuest(false, true);
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
			if (npc.getId() == 30673)
			{
				final QuestState q184 = player.getQuestState(_184_NikolasCooperationContract.class.getSimpleName());
				if ((q184 != null) && q184.isCompleted() && hasQuestItems(player, 10362))
				{
					htmltext = player.getLevel() >= getMinLvl(getId()) ? "30673-01.htm" : "30673-02.htm";
				}
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 30673 :
				{
					if (memoState >= 1)
					{
						htmltext = "30673-04.htm";
					}
					break;
				}
				case 30621 :
				{
					if (memoState == 1)
					{
						htmltext = "30621-01.htm";
					}
					else if (memoState == 2)
					{
						htmltext = "30621-04.htm";
					}
					break;
				}
				case 31437 :
				{
					if (memoState == 2)
					{
						if (hasQuestItems(player, 10367))
						{
							htmltext = "31437-02.htm";
						}
						else
						{
							htmltext = "31437-01.htm";
						}
					}
					else if (memoState == 3)
					{
						htmltext = "31437-05.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == 30673)
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isMemoState(2) && Util.checkIfInRange(1500, npc, player, false))
		{
			if (qs.calcDropItems(getId(), 10367, npc.getId(), 1))
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _186_ContractExecution(186, _186_ContractExecution.class.getSimpleName(), "");
	}
}