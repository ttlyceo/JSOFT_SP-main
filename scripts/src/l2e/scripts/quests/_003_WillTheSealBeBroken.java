
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
public class _003_WillTheSealBeBroken extends Quest
{
	public _003_WillTheSealBeBroken(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30141);
		addTalkId(30141);

		addKillId(20031, 20041, 20046, 20048, 20052, 20057);
		
		questItemIds = new int[]
		{
		        1081, 1082, 1083
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

		if (event.equalsIgnoreCase("30141-03.htm") && npc.getId() == 30141)
		{
			st.startQuest();
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

		if (npcId == 30141)
		{
			switch (st.getState())
			{
				case State.COMPLETED:
					htmltext = getAlreadyCompletedMsg(player);
					break;
				case State.CREATED:
					if (player.getRace().ordinal() != 2)
					{
						htmltext = "30141-00.htm";
						st.exitQuest(true);
					}
					else if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "30141-02.htm";
					}
					else
					{
						htmltext = "30141-01.htm";
						st.exitQuest(true);
					}
					break;
				case State.STARTED:
					switch (cond)
					{
						case 2:
							if (st.getQuestItemsCount(1081) > 0 && st.getQuestItemsCount(1082) > 0 && st.getQuestItemsCount(1083) > 0)
							{
								htmltext = "30141-06.htm";
								st.takeItems(1081, 1);
								st.takeItems(1082, 1);
								st.takeItems(1083, 1);
								st.calcReward(getId());
								st.exitQuest(false, true);
							}
							else
							{
								htmltext = "30141-04.htm";
							}
							break;
					}
					break;
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			final int npcId = npc.getId();
			if (npcId == 20031)
			{
				if (st.getQuestItemsCount(1081) == 0)
				{
					st.giveItems(1081, 1);
					st.playSound("Itemsound.quest_itemget");
				}
			}
			else if (npcId == 20041 || npcId == 20046)
			{
				if (st.getQuestItemsCount(1082) == 0)
				{
					st.giveItems(1082, 1);
					st.playSound("Itemsound.quest_itemget");
				}
			}
			else if (npcId == 20048 || npcId == 20052 || npcId == 20057)
			{
				if (st.getQuestItemsCount(1083) == 0)
				{
					st.giveItems(1083, 1);
					st.playSound("Itemsound.quest_itemget");
				}
			}
			
			if (st.getQuestItemsCount(1081) == 1 && st.getQuestItemsCount(1082) == 1 && st.getQuestItemsCount(1083) == 1)
			{
				st.setCond(2, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _003_WillTheSealBeBroken(3, _003_WillTheSealBeBroken.class.getSimpleName(), "");
	}
}