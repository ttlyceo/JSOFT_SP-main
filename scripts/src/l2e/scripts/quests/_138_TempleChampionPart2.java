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
 * Rework by LordWinter 13.03.2023
 */
public class _138_TempleChampionPart2 extends Quest
{
	public _138_TempleChampionPart2(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30070);
		addTalkId(30070, 30118, 30474, 30666);

		addKillId(20176, 20550, 20551, 20552);

		questItemIds = new int[]
		{
		        10340, 10342, 10343, 10344
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return getNoQuestMsg(player);
		}

		switch (event)
		{
			case "30070-02.htm":
				if (st.isCreated() && npc.getId() == 30070)
				{
					st.startQuest();
					st.giveItems(10340, 1);
				}
				break;
			case "30070-05.htm":
				if (st.isCond(7) && npc.getId() == 30070)
				{
					if ((player.getLevel() < 42))
					{
						st.calcExpAndSp(getId());
					}
					st.calcReward(getId());
					st.exitQuest(false, true);
				}
				break;
			case "30070-03.htm":
				if (st.isCond(1) && npc.getId() == 30070)
				{
					st.setCond(2, true);
				}
				break;
			case "30118-06.htm":
				if (st.isCond(2) && npc.getId() == 30118)
				{
					st.setCond(3, true);
				}
				break;
			case "30118-09.htm":
				if (st.isCond(5) && npc.getId() == 30118)
				{
					st.setCond(6, true);
					st.giveItems(10344, 1);
				}
				break;
			case "30474-02.htm":
				if (st.isCond(3) && npc.getId() == 30474)
				{
					st.setCond(4, true);
				}
				break;
			case "30666-02.htm":
				if (st.hasQuestItems(10344) && npc.getId() == 30666)
				{
					st.set("talk", "1");
					st.takeItems(10344, -1);
				}
				break;
			case "30666-03.htm":
				if (st.hasQuestItems(10340) && npc.getId() == 30666)
				{
					st.set("talk", "2");
					st.takeItems(10340, -1);
				}
				break;
			case "30666-08.htm":
				if (st.isCond(6) && npc.getId() == 30666)
				{
					st.setCond(7, true);
					st.unset("talk");
				}
				break;
		}
		return event;
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
		switch (npc.getId())
		{
			case 30070 :
				switch (cond)
				{
					case 1:
						htmltext = "30070-02.htm";
						break;
					case 2:
					case 3:
					case 4:
					case 5:
					case 6:
						htmltext = "30070-03.htm";
						break;
					case 7:
						htmltext = "30070-04.htm";
						break;
					default:
						if (st.isCompleted())
						{
							return getAlreadyCompletedMsg(player);
						}
						final var qs = player.getQuestState("_137_TempleChampionPart1");
						final var isCompleted = qs != null && qs.isCompleted();
						htmltext = (player.getLevel() >= getMinLvl(getId())) && isCompleted ? "30070-01.htm" : "30070-00.htm";
						break;
				}
				break;
			case 30118 :
				switch (cond)
				{
					case 2:
						htmltext = "30118-01.htm";
						break;
					case 3:
					case 4:
						htmltext = "30118-07.htm";
						break;
					case 5:
						htmltext = "30118-08.htm";
						if (st.hasQuestItems(10343))
						{
							st.takeItems(10343, -1);
						}
						break;
					case 6:
						htmltext = "30118-10.htm";
						break;
				}
				break;
			case 30474 :
				switch (cond)
				{
					case 3:
						htmltext = "30474-01.htm";
						break;
					case 4:
						if (st.getQuestItemsCount(10342) >= 10)
						{
							st.takeItems(10342, -1);
							st.giveItems(10343, 1);
							st.setCond(5, true);
							htmltext = "30474-04.htm";
						}
						else
						{
							htmltext = "30474-03.htm";
						}
						break;
					case 5:
						htmltext = "30474-05.htm";
						break;
				}
				break;
			case 30666 :
				switch (cond)
				{
					case 6:
						switch (st.getInt("talk"))
						{
							case 1:
								htmltext = "30666-02.htm";
								break;
							case 2:
								htmltext = "30666-03.htm";
								break;
							default:
								htmltext = "30666-01.htm";
								break;
						}
						break;
					case 7:
						htmltext = "30666-09.htm";
						break;
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 4);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 10342, npc.getId(), 10);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _138_TempleChampionPart2(138, _138_TempleChampionPart2.class.getSimpleName(), "");
	}
}