
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
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 06.12.2019
 */
public class _023_LidiasHeart extends Quest
{
	public Npc _ghost;

	public _023_LidiasHeart(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31328);
		addTalkId(31328, 31526, 31524, 31523, 31386, 31530);

		questItemIds = new int[]
		{
		        7063, 7149, 7148, 7064, 7150
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

		if (event.equalsIgnoreCase("31328-02.htm") && npc.getId() == 31328)
		{
			st.giveItems(7063, 1);
			st.giveItems(7149, 1);
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31328-03.htm") && npc.getId() == 31328)
		{
			if (st.isCond(1))
			{
				st.setCond(2, false);
			}
		}
		else if (event.equalsIgnoreCase("31526-01.htm") && npc.getId() == 31526)
		{
			if (st.isCond(2))
			{
				st.setCond(3, false);
			}
		}
		else if (event.equalsIgnoreCase("31526-05.htm") && npc.getId() == 31526)
		{
			st.giveItems(7148, 1);
			if (st.getQuestItemsCount(7064) != 0)
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("31526-11.htm") && npc.getId() == 31526)
		{
			st.giveItems(7064, 1);
			if (st.getQuestItemsCount(7148) != 0)
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("31328-19.htm") && npc.getId() == 31328)
		{
			if (st.getCond() >= 4)
			{
				st.setCond(6, false);
			}
		}
		else if (event.equalsIgnoreCase("31524-04.htm") && npc.getId() == 31524)
		{
			st.setCond(7, true);
			st.takeItems(7064, -1);
		}
		else if (event.equalsIgnoreCase("31523-02.htm") && npc.getId() == 31523)
		{
			despawnGhost(st);
			spawnGhost(st);
			st.playSound("SkillSound5.horror_02");
		}
		else if (event.equalsIgnoreCase("31523-05.htm") && npc.getId() == 31523)
		{
			st.startQuestTimer("viwer_timer", 10000);
		}
		else if (event.equalsIgnoreCase("viwer_timer"))
		{
			htmltext = "31523-06.htm";
			st.setCond(8, true);
		}
		else if (event.equalsIgnoreCase("31530-02.htm") && npc.getId() == 31530)
		{
			if (st.getCond() > 6 && st.getQuestItemsCount(7149) != 0)
			{
				st.takeItems(7149, -1);
				st.giveItems(7150, 1);
				st.setCond(10, true);
			}
		}
		else if (event.equalsIgnoreCase("i7064-02.htm"))
		{
			htmltext = "i7064-02.htm";
		}
		else if (event.equalsIgnoreCase("31526-13.htm"))
		{
			st.startQuestTimer("read_book", 120000);
		}
		else if (event.equalsIgnoreCase("read_book"))
		{
			htmltext = "i7064.htm";
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
		
		switch (st.getState())
		{
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED :
				final QuestState qs = player.getQuestState("_022_TragedyInVonHellmannForest");
				if (qs != null && qs.isCompleted())
				{
					htmltext = "31328-01.htm";
				}
				else
				{
					htmltext = "31328-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				switch (npc.getId())
				{
					case 31328 :
						switch (cond)
						{
							case 1:
								htmltext = "31328-03.htm";
								break;
							case 2:
								htmltext = "31328-07.htm";
								break;
							case 4:
								htmltext = "31328-08.htm";
								break;
							case 6:
								htmltext = "31328-19.htm";
								break;
						}
						break;
					case 31526 :
						switch (cond)
						{
							case 2:
								if (st.getQuestItemsCount(7149) != 0)
								{
									htmltext = "31526-00.htm";
								}
								break;
							case 3:
								if (st.getQuestItemsCount(7148) == 0)
								{
									if (st.getQuestItemsCount(7064) == 0)
									{
										htmltext = "31526-02.htm";
									}
									else
									{
										htmltext = "31526-12.htm";
									}
								}
								else if (st.getQuestItemsCount(7064) == 0)
								{
									htmltext = "31526-06.htm";
								}
								break;
							case 4:
								htmltext = "31526-13.htm";
								break;
						}
						break;
					case 31524 :
						switch (cond)
						{
							case 6:
								htmltext = "31524-01.htm";
								break;
							case 7:
								htmltext = "31524-05.htm";
								break;
						}
						break;
					case 31523 :
						switch (cond)
						{
							case 6:
								if (st.getQuestTimer("spawn_timer") != null)
								{
									htmltext = "31523-03.htm";
								}
								else
								{
									htmltext = "31523-01.htm";
								}
								break;
							case 7:
								htmltext = "31523-04.htm";
								break;
							case 8:
								htmltext = "31523-06.htm";
								break;
						}
						break;
					case 31386 :
						switch (cond)
						{
							case 8:
								htmltext = "31386-01.htm";
								st.setCond(9, false);
								break;
							case 9:
								htmltext = "31386-02.htm";
								break;
							case 10:
								if (st.getQuestItemsCount(7150) != 0)
								{
									htmltext = "31386-03.htm";
									st.takeItems(7150, -1);
									st.calcExpAndSp(getId());
									st.calcReward(getId());
									st.exitQuest(false, true);
								}
								else
								{
									htmltext = "31386-03a.htm";
								}
								break;
						}
						break;
					case 31530 :
						switch (cond)
						{
							case 9:
								if (st.getQuestItemsCount(7149) != 0)
								{
									htmltext = "31530-01.htm";
								}
								else
								{
									htmltext = "31530-01a.htm";
								}
								break;
							case 10:
								htmltext = "31386-03.htm";
								break;
						}
						break;
				}
				break;
		}
		
		return htmltext;
	}
	
	private void spawnGhost(QuestState st)
	{
		_ghost = st.addSpawn(31524, 51432, -54570, -3136, getRandom(0, 20), false, 180000);
		_ghost.broadcastPacketToOthers(2000, new NpcSay(_ghost.getObjectId(), 0, _ghost.getId(), NpcStringId.WHO_AWOKE_ME));
	}

	private void despawnGhost(QuestState st)
	{
		if (_ghost != null)
		{
			_ghost.deleteMe();
		}
		_ghost = null;
	}
	
	public static void main(String[] args)
	{
		new _023_LidiasHeart(23, _023_LidiasHeart.class.getSimpleName(), "");
	}
}