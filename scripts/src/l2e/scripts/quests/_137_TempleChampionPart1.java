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
public class _137_TempleChampionPart1 extends Quest
{
	public _137_TempleChampionPart1(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30070);
		addTalkId(30070);

		addKillId(20083, 20144, 20199, 20200, 20201, 20202);

		questItemIds = new int[]
		{
		        10340
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
				if (st.isCreated())
				{
					st.startQuest();
				}
				break;
			case "30070-05.htm":
				st.set("talk", "1");
				break;
			case "30070-06.htm":
				st.set("talk", "2");
				break;
			case "30070-08.htm":
				if (st.isCond(1))
				{
					st.unset("talk");
					st.setCond(2, true);
				}
				break;
			case "30070-16.htm":
				if (st.isCond(3) && (st.hasQuestItems(10334) && st.hasQuestItems(10339)))
				{
					st.takeItems(10334, -1);
					st.takeItems(10339, -1);
					if (player.getLevel() < 41)
					{
						st.calcExpAndSp(getId());
					}
					st.calcReward(getId());
					st.exitQuest(false, true);
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

		if (st.isCompleted())
		{
			return getAlreadyCompletedMsg(player);
		}

		switch (st.getCond())
		{
			case 1:
				switch (st.getInt("talk"))
				{
					case 1:
						htmltext = "30070-05.htm";
						break;
					case 2:
						htmltext = "30070-06.htm";
						break;
					default:
						htmltext = "30070-03.htm";
						break;
				}
				break;
			case 2:
				htmltext = "30070-08.htm";
				break;
			case 3:
				if (st.getInt("talk") == 1)
				{
					htmltext = "30070-10.htm";
				}
				else if (st.getQuestItemsCount(10340) >= 30)
				{
					st.set("talk", "1");
					htmltext = "30070-09.htm";
					st.takeItems(10340, -1);
				}
				break;
			default:
				htmltext = ((player.getLevel() >= getMinLvl(getId())) && st.hasQuestItems(10334) && st.hasQuestItems(10339)) ? "30070-01.htm" : "30070-00.htm";
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 2);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (st.calcDropItems(getId(), 10340, npc.getId(), 30))
			{
				st.setCond(3);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _137_TempleChampionPart1(137, _137_TempleChampionPart1.class.getSimpleName(), "");
	}
}
