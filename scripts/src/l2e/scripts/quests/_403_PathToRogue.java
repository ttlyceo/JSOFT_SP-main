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
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Updated by LordWinter 16.08.2020
 */
public final class _403_PathToRogue extends Quest
{
	private static final int[] STOLEN_ITEMS =
	{
	        1186, 1187, 1188, 1189
	};
	
	private _403_PathToRogue(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30379);
		addTalkId(30379, 30425);
		
		addKillId(20035, 20042, 20045, 20051, 20054, 20060, 27038);
		
		questItemIds = new int[]
		{
		        1180, 1181, 1182, 1183, 1184, 1185, 1186, 1187, 1188, 1189
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30379_2"))
		{
			if ((player.getClassId().getId() == 0x00) && !st.isCompleted())
			{
				if (player.getLevel() > 17)
				{
					if (player.getInventory().getInventoryItemCount(1190, -1) != 0)
					{
						htmltext = "30379-04.htm";
					}
					else
					{
						htmltext = "30379-05.htm";
					}
				}
				else
				{
					htmltext = "30379-03.htm";
				}
			}
			else if (player.getClassId().getId() == 0x07)
			{
				htmltext = "30379-02a.htm";
			}
			else
			{
				htmltext = "30379-02.htm";
			}
		}
		else if (!st.isCompleted())
		{
			if (event.equalsIgnoreCase("1"))
			{
				st.startQuest();
				st.giveItems(1180, 1);
				htmltext = "30379-06.htm";
			}
			else if (event.equalsIgnoreCase("30425_1"))
			{
				st.takeItems(1180, -1);
				if (st.getQuestItemsCount(1181) == 0)
				{
					st.giveItems(1181, 1);
				}
				if (st.getQuestItemsCount(1182) == 0)
				{
					st.giveItems(1182, 1);
				}
				st.setCond(2, true);
				htmltext = "30425-05.htm";
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
		
		final int cond = st.getCond();
		
		if (npc.getId() == 30379)
		{
			if (cond == 0)
			{
				htmltext = "30379-01.htm";
			}
			else if (st.getQuestItemsCount(1184) == 0)
			{
				if (hasQuestItems(player, 1186, 1187, 1188, 1189))
				{
					takeItems(player, 1181, 1);
					takeItems(player, 1182, 1);
					takeItems(player, 1185, 1);
					takeItems(player, 1186, 1);
					takeItems(player, 1187, 1);
					takeItems(player, 1188, 1);
					takeItems(player, 1189, 1);
					final String done = st.getGlobalQuestVar("1ClassQuestFinished");
					if (done == null || done.isEmpty())
					{
						st.calcExpAndSp(getId());
						st.calcReward(getId(), 1);
						st.saveGlobalQuestVar("1ClassQuestFinished", "1");
					}
					st.calcReward(getId(), 2);
					player.sendPacket(new SocialAction(player.getObjectId(), 3));
					st.exitQuest(false, true);
					htmltext = "30379-09.htm";
				}
				else if (st.getQuestItemsCount(1180) != 0)
				{
					htmltext = "30379-07.htm";
				}
				else if ((st.getQuestItemsCount(1181) != 0) && (st.getQuestItemsCount(1182) != 0) && (st.getQuestItemsCount(1185) == 0))
				{
					htmltext = "30379-10.htm";
				}
				else
				{
					htmltext = "30379-11.htm";
				}
			}
			else
			{
				st.takeItems(1184, -1);
				st.giveItems(1185, 1);
				st.setCond(5, true);
				htmltext = "30379-08.htm";
			}
		}
		else
		{
			if (!st.isStarted())
			{
				return htmltext;
			}
			else if (st.getQuestItemsCount(1180) != 0)
			{
				htmltext = "30425-01.htm";
			}
			else if (st.getQuestItemsCount(1184) != 0)
			{
				htmltext = "30425-08.htm";
			}
			else if (st.getQuestItemsCount(1185) != 0)
			{
				htmltext = "30425-08.htm";
			}
			else if (st.getQuestItemsCount(1183) >= 10)
			{
				st.takeItems(1183, -1);
				st.giveItems(1184, 1);
				st.setCond(4, true);
				htmltext = "30425-07.htm";
			}
			else
			{
				htmltext = "30425-06.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = getQuestState(player, false);
		if ((st != null) && st.isStarted())
		{
			if (npc.getId() == 27038)
			{
				if (hasQuestItems(player, 1185))
				{
					final int randomItem = STOLEN_ITEMS[getRandom(STOLEN_ITEMS.length)];
					if (!hasQuestItems(player, randomItem))
					{
						st.giveItems(randomItem, 1);
						if (hasQuestItems(player, STOLEN_ITEMS))
						{
							st.setCond(6, true);
						}
						else
						{
							st.playSound("ItemSound.quest_itemget");
						}
					}
				}
			}
			else
			{
				if (st.calcDropItems(getId(), 1183, npc.getId(), 10))
				{
					st.setCond(3, true);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _403_PathToRogue(403, _403_PathToRogue.class.getSimpleName(), "");
	}
}