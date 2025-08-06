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
import l2e.gameserver.network.NpcStringId;

/**
 * Rework by LordWinter 04.12.2019
 */
public class _001_LettersOfLove extends Quest
{
	public _001_LettersOfLove(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30048);
		addTalkId(30048, 30006, 30033);
		
		questItemIds = new int[]
		{
		        687, 688, 1079, 1080
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

		if (event.equalsIgnoreCase("30048-05.htm") && npc.getId() == 30048)
		{
			st.startQuest();
			st.giveItems(687, 1);
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
		
		final int npcId = npc.getId();
		final int cond = st.getCond();

		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 30048)
				{
					if ((player.getLevel() >= getMinLvl(getId())) && (cond == 0))
					{
						htmltext = "30048-02.htm";
					}
					else
					{
						htmltext = "30048-01.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED:
				if (npcId == 30048)
				{
					switch (cond)
					{
						case 2:
							if (st.getQuestItemsCount(688) > 0)
							{
								htmltext = "30048-07.htm";
								st.takeItems(688, -1);
								st.giveItems(1079, 1);
								st.setCond(3, true);
							}
							break;
						case 3:
							if (st.getQuestItemsCount(1079) > 0)
							{
								htmltext = "30048-08.htm";
							}
							break;
						case 4:
							if (st.getQuestItemsCount(1080) > 0)
							{
								htmltext = "30048-09.htm";
								st.takeItems(1080, -1);
								st.calcExpAndSp(getId());
								st.calcReward(getId());
								st.exitQuest(false, true);
								showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
							}
							break;
						default:
							htmltext = "30048-06.htm";
							break;
					}
				}
				else if (npcId == 30006)
				{
					switch (cond)
					{
						case 1:
							if (st.getQuestItemsCount(687) > 0)
							{
								htmltext = "30006-01.htm";
								st.takeItems(687, -1);
								st.giveItems(688, 1);
								st.setCond(2, true);
							}
							break;
						default:
							if (cond > 1)
							{
								htmltext = "30006-02.htm";
							}
							break;
					}
				}
				else if (npcId == 30033)
				{
					switch (cond)
					{
						case 3:
							if (st.getQuestItemsCount(1079) > 0)
							{
								htmltext = "30033-01.htm";
								st.takeItems(1079, -1);
								st.giveItems(1080, 1);
								st.setCond(4, true);
							}
							break;
						default:
							if (cond > 3)
							{
								htmltext = "30033-02.htm";
							}
							break;
					}
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _001_LettersOfLove(1, _001_LettersOfLove.class.getSimpleName(), "");
	}
}
