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

public class _115_TheOtherSideOfTruth extends Quest
{
	public _115_TheOtherSideOfTruth(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32020);
		addTalkId(32020, 32018, 32022, 32021, 32077, 32078, 32079);
		
		questItemIds = new int[]
		{
		        8079, 8080, 8081, 8082
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
		
		if (event.equalsIgnoreCase("32020-02.htm") && npc.getId() == 32020)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		if (event.equalsIgnoreCase("32020-06.htm") || event.equalsIgnoreCase("32020-08a.htm"))
		{
			st.exitQuest(true, true);
		}
		else if (event.equalsIgnoreCase("32020-05.htm") && npc.getId() == 32020)
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
				st.takeItems(8079, 1);
			}
		}
		else if (event.equalsIgnoreCase("32020-08.htm") || event.equalsIgnoreCase("32020-07a.htm") && npc.getId() == 32020)
		{
			if (st.isCond(3))
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("32020-12.htm") && npc.getId() == 32020)
		{
			if (st.isCond(4))
			{
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("32018-04.htm") && npc.getId() == 32018)
		{
			if (st.isCond(6))
			{
				st.takeItems(8080, 1);
				st.setCond(7, true);
			}
		}
		else if (event.equalsIgnoreCase("Sculpture-04a"))
		{
			if (st.isCond(7))
			{
				st.setCond(8, true);
				if (st.getInt("32021") == 0 && st.getInt("32077") == 0)
				{
					st.giveItems(8081, 1);
				}
				htmltext = "Sculpture-04.htm";
			}
		}
		else if (event.equalsIgnoreCase("32022-02.htm") && npc.getId() == 32022)
		{
			if (st.isCond(8))
			{
				st.setCond(9, true);
				st.giveItems(8082, 1);
			}
		}
		else if (event.equalsIgnoreCase("32020-16.htm") && npc.getId() == 32020)
		{
			if (st.isCond(9))
			{
				st.setCond(10, true);
				st.takeItems(8082, 1);
			}
		}
		else if (event.equalsIgnoreCase("32020-18.htm") && npc.getId() == 32020)
		{
			if (st.hasQuestItems(8081) && st.isCond(12))
			{
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
			else
			{
				if (st.isCond(10))
				{
					st.setCond(11, true);
					htmltext = "32020-19.htm";
				}
			}
		}
		else if (event.equalsIgnoreCase("32020-19.htm") && npc.getId() == 32020)
		{
			if (st.isCond(10))
			{
				st.setCond(11, true);
			}
		}
		else if (event.startsWith("32021") || event.startsWith("32077"))
		{
			if (event.contains("-pick"))
			{
				st.set("talk", "1");
				event = event.replace("-pick", "");
			}
			st.set("talk", "1");
			st.set(event, "1");
			htmltext = "Sculpture-05.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if(st == null)
		{
			return htmltext;
		}
		
		final int npcId = npc.getId();
		
		switch (st.getState())
		{
			case State.CREATED :
				switch (npcId)
				{
					case 32020 :
						if (player.getLevel() >= getMinLvl(getId()))
						{
							htmltext = "32020-01.htm";
						}
						else
						{
							htmltext = "32020-00.htm";
							st.exitQuest(true);
						}
						break;
				}
				break;
			case State.STARTED :
				switch (npcId)
				{
					case 32020 :
						switch (st.getCond())
						{
							case 1 :
								htmltext = "32020-03.htm";
								break;
							case 2 :
								htmltext = "32020-04.htm";
								break;
							case 3 :
								htmltext = "32020-05.htm";
								break;
							case 4 :
								htmltext = "32020-11.htm";
								break;
							case 5 :
								htmltext = "32020-13.htm";
								st.giveItems(8080, 1);
								st.setCond(6, true);
								break;
							case 6 :
								htmltext = "32020-14.htm";
								break;
							case 7 :
							case 8 :
								htmltext = "32020-14a.htm";
								break;
							case 9 :
								htmltext = "32020-15.htm";
								break;
							case 10 :
								htmltext = "32020-17.htm";
								break;
							case 11 :
								htmltext = "32020-20.htm";
								break;
							case 12 :
								htmltext = "32020-18.htm";
								st.calcExpAndSp(getId());
								st.calcReward(getId());
								st.exitQuest(false, true);
								break;
						}
						break;
					case 32018 :
						switch (st.getCond())
						{
							case 1 :
								htmltext = "32018-01.htm";
								st.giveItems(8079, 1);
								st.setCond(2, true);
								break;
							case 2 :
								htmltext = "32018-02.htm";
								break;
							case 6 :
								htmltext = "32018-03.htm";
								break;
							case 7 :
								htmltext = "32018-05.htm";
								break;
						}
						break;
					case 32022 :
						switch (st.getCond())
						{
							case 8 :
								htmltext = "32022-02.htm";
								st.giveItems(8082, 1);
								st.setCond(9, true);
								break;
							default :
								htmltext = "";
								break;
						}
						break;
					case 32021 :
					case 32077 :
					case 32078 :
					case 32079 :
						switch (st.getCond())
						{
							case 7 :
								final String _npcId = String.valueOf(npcId);
								final int npcId_flag = st.getInt(_npcId);
								if (npcId == 32021 || npcId == 32077)
								{
									final int talk_flag = st.getInt("talk");
									return npcId_flag == 1 ? "Sculpture-02.htm" : talk_flag == 1 ? "Sculpture-06.htm" : "Sculpture-03-" + _npcId + ".htm";
								}
								else if (npcId_flag == 1)
								{
									htmltext = "Sculpture-02.htm";
								}
								else
								{
									st.set(_npcId, "1");
									htmltext = "Sculpture-01.htm";
								}
								break;
							case 8 :
								htmltext = "Sculpture-04.htm";
								break;
							case 11 :
								htmltext = "Sculpture-07.htm";
								st.giveItems(8081, 1);
								st.setCond(12, true);
								break;
							case 12 :
								htmltext = "Sculpture-08.htm";
								break;
						}
						break;
				}
				break;
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _115_TheOtherSideOfTruth(115, _115_TheOtherSideOfTruth.class.getSimpleName(), "");
	}
}