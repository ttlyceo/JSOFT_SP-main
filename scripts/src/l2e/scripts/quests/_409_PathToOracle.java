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
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Rework by LordWinter 24.01.2023
 */
public final class _409_PathToOracle extends Quest
{
	private _409_PathToOracle(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30293);
		addTalkId(30293, 30424, 30428);
		
		addKillId(27032, 27033, 27034, 27035);
		
		questItemIds = new int[]
		{
		        1231, 1232, 1233, 1234, 1236, 1275
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		final var npcId = npc.getId();
		if (event.equalsIgnoreCase("1") && npcId == 30293)
		{
			if ((player.getClassId().getId() == 0x19) && st.isCreated())
			{
				if (player.getLevel() > 17)
				{
					if (player.getInventory().getInventoryItemCount(1235, -1) == 0)
					{
						st.startQuest();
						st.giveItems(1231, 1);
						htmltext = "30293-05.htm";
					}
					else
					{
						htmltext = "30293-04.htm";
					}
				}
				else
				{
					htmltext = "30293-03.htm";
				}
			}
			else if (player.getClassId().getId() == 0x1d)
			{
				htmltext = "30293-02a.htm";
			}
			else
			{
				htmltext = "30293-02.htm";
			}
		}
		else if (!st.isCompleted())
		{
			if (event.equalsIgnoreCase("30424_1") && npcId == 30424)
			{
				if (st.isCond(1))
				{
					st.setCond(2, false);
					st.addSpawn(27032);
					st.addSpawn(27033);
					st.addSpawn(27034);
				}
				return null;
			}
			else if (event.equalsIgnoreCase("30428_1") && npcId == 30428)
			{
				htmltext = "30428-02.htm";
			}
			else if (event.equalsIgnoreCase("30428_2") && npcId == 30428)
			{
				htmltext = "30428-03.htm";
			}
			else if (event.equalsIgnoreCase("30428_3") && npcId == 30428)
			{
				final var now = System.currentTimeMillis();
				if (st.getLong("spawnDelay") < now && st.getQuestItemsCount(1275) == 0)
				{
					st.set("spawnDelay", (now + 30000L));
					st.addSpawn(27035);
				}
				return null;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		
		if (npcId == 30293)
		{
			if (st.getQuestItemsCount(1231) != 0)
			{
				if ((st.getQuestItemsCount(1233) == 0) && (st.getQuestItemsCount(1234) == 0) && (st.getQuestItemsCount(1232) == 0) && (st.getQuestItemsCount(1236) == 0))
				{
					if (cond == 0)
					{
						htmltext = "30293-06.htm";
					}
					else
					{
						htmltext = "30293-09.htm";
					}
				}
				else
				{
					if ((st.getQuestItemsCount(1233) != 0) && (st.getQuestItemsCount(1234) != 0) && (st.getQuestItemsCount(1232) != 0) && (st.getQuestItemsCount(1236) == 0))
					{
						st.takeItems(1232, -1);
						st.takeItems(1233, -1);
						st.takeItems(1234, -1);
						st.takeItems(1231, -1);
						final String done = st.getGlobalQuestVar("1ClassQuestFinished");
						if (done == null || done.isEmpty())
						{
							st.addExpAndSp(228064, 16455);
							st.giveItems(57, 163800);
							st.saveGlobalQuestVar("1ClassQuestFinished", "1");
						}
						st.giveItems(1235, 1);
						player.sendPacket(new SocialAction(player.getObjectId(), 3));
						st.exitQuest(false, true);
						htmltext = "30293-08.htm";
					}
					else
					{
						htmltext = "30293-07.htm";
					}
				}
			}
			else if (cond == 0)
			{
				if (st.getQuestItemsCount(1235) == 0)
				{
					htmltext = "30293-01.htm";
				}
				else
				{
					htmltext = "30293-04.htm";
				}
			}
		}
		else if ((cond != 0) && (st.getQuestItemsCount(1231) != 0))
		{
			if (npcId == 30424)
			{
				if ((st.getQuestItemsCount(1233) == 0) && (st.getQuestItemsCount(1234) == 0) && (st.getQuestItemsCount(1232) == 0) && (st.getQuestItemsCount(1236) == 0))
				{
					if (cond > 2)
					{
						htmltext = "30424-05.htm";
					}
					else
					{
						htmltext = "30424-01.htm";
					}
				}
				else if ((st.getQuestItemsCount(1233) == 0) && (st.getQuestItemsCount(1234) != 0) && (st.getQuestItemsCount(1232) == 0) && (st.getQuestItemsCount(1236) == 0))
				{
					st.giveItems(1236, 1);
					st.set("cond", "4");
					htmltext = "30424-02.htm";
				}
				else if ((st.getQuestItemsCount(1233) == 0) && (st.getQuestItemsCount(1234) != 0) && (st.getQuestItemsCount(1232) == 0) && (st.getQuestItemsCount(1236) != 0))
				{
					if (st.getQuestItemsCount(1275) == 0)
					{
						htmltext = "30424-06.htm";
					}
					else
					{
						htmltext = "30424-03.htm";
					}
				}
				else if ((st.getQuestItemsCount(1233) == 0) && (st.getQuestItemsCount(1234) != 0) && (st.getQuestItemsCount(1232) != 0) && (st.getQuestItemsCount(1236) != 0))
				{
					st.takeItems(1236, -1);
					st.giveItems(1233, 1);
					st.set("cond", "7");
					htmltext = "30424-04.htm";
				}
				else if ((st.getQuestItemsCount(1233) != 0) && (st.getQuestItemsCount(1234) != 0) && (st.getQuestItemsCount(1232) != 0) && (st.getQuestItemsCount(1236) == 0))
				{
					htmltext = "30424-05.htm";
				}
			}
			else if (st.getQuestItemsCount(1234) != 0)
			{
				if (st.getQuestItemsCount(1275) != 0)
				{
					st.takeItems(1275, -1);
					st.giveItems(1232, 1);
					st.set("cond", "6");
					htmltext = "30428-04.htm";
				}
				else
				{
					if (st.getQuestItemsCount(1232) == 0)
					{
						if (cond > 4)
						{
							htmltext = "30428-06.htm";
						}
						else
						{
							htmltext = "30428-01.htm";
						}
					}
					else
					{
						htmltext = "30428-05.htm";
					}
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final int npcId = npc.getId();
		
		if (npcId == 27035)
		{
			if (st.getQuestItemsCount(1275) == 0)
			{
				st.giveItems(1275, 1);
				st.setCond(5, true);
			}
		}
		else if (st.getQuestItemsCount(1234) == 0)
		{
			st.giveItems(1234, 1);
			st.setCond(3, true);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _409_PathToOracle(409, _409_PathToOracle.class.getSimpleName(), "");
	}
}