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
import l2e.gameserver.model.strings.server.ServerStorage;

/**
 * Rework by LordWinter 09.06.2021
 */
public class _663_SeductiveWhispers extends Quest
{
	public _663_SeductiveWhispers(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30846);
		addTalkId(30846);

		addKillId(20674, 20678, 20954, 20955, 20956, 20957, 20958, 20959, 20960, 20961, 20962, 20974, 20975, 20976, 20996, 20997, 20998, 20999, 21001, 21002, 21006, 21007, 21008, 21009, 21010);
		
		questItemIds = new int[]
		{
		        8766
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final long BEAD_COUNT = st.getQuestItemsCount(8766);

		if (event.equalsIgnoreCase("30846-04.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
				st.set("round", "0");
			}
		}
		else if (event.equalsIgnoreCase("30846-09.htm"))
		{
			st.exitQuest(true, true);
		}
		else if (event.equalsIgnoreCase("30846-08.htm"))
		{
			if (BEAD_COUNT < 1)
			{
				htmltext = "30846-11.htm";
			}
			else
			{
				st.takeItems(8766, 1);
				final int random = getRandom(100);
				if (random < 68)
				{
					htmltext = "30846-08.htm";
				}
				else
				{
					htmltext = "30846-08a.htm";
				}
			}
		}
		else if (event.equalsIgnoreCase("30846-10.htm"))
		{
			st.set("round", "0");
			if (BEAD_COUNT < 50)
			{
				htmltext = "30846-11.htm";
			}
		}
		else if (event.equalsIgnoreCase("30846-12.htm"))
		{
			final int round = st.getInt("round");
			if (round == 0)
			{
				if (BEAD_COUNT < 50)
				{
					return "30846-11.htm";
				}
				st.takeItems(8766, 50);
			}
			if (getRandom(100) > 68)
			{
				st.set("round", "0");
				htmltext = "30846-12.htm";
			}
			else
			{
				int next_round = round + 1;
				htmltext = "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "663quest.TEXT1") + " <font color=\"LEVEL\">NROUND</font> " + ServerStorage.getInstance().getString(player.getLang(), "663quest.TEXT5") + "<br><font color=\"LEVEL\">MYPRIZE</font><br><br><a action=\"bypass -h Quest _663_SeductiveWhispers 30846-12.htm\">"
				                + ServerStorage.getInstance().getString(player.getLang(), "663quest.TEXT2") + "</a><br><a action=\"bypass -h Quest _663_SeductiveWhispers 30846-13.htm\">" + ServerStorage.getInstance().getString(player.getLang(), "663quest.TEXT3") + "</a><br><a action=\"bypass -h Quest _663_SeductiveWhispers 30846-09.htm\">"
				                + ServerStorage.getInstance().getString(player.getLang(), "663quest.TEXT4") + "</a></body></html>";
				htmltext = htmltext.replace("NROUND", String.valueOf(next_round));
				if (next_round == 1)
				{
					htmltext = htmltext.replace("MYPRIZE", "" + ServerStorage.getInstance().getString(player.getLang(), "663quest.1_ROUND") + "");
				}
				if (next_round == 2)
				{
					htmltext = htmltext.replace("MYPRIZE", "" + ServerStorage.getInstance().getString(player.getLang(), "663quest.2_ROUND") + "");
				}
				if (next_round == 3)
				{
					htmltext = htmltext.replace("MYPRIZE", "" + ServerStorage.getInstance().getString(player.getLang(), "663quest.3_ROUND") + "");
				}
				if (next_round == 4)
				{
					htmltext = htmltext.replace("MYPRIZE", "" + ServerStorage.getInstance().getString(player.getLang(), "663quest.4_ROUND") + "");
				}
				if (next_round == 5)
				{
					htmltext = htmltext.replace("MYPRIZE", "" + ServerStorage.getInstance().getString(player.getLang(), "663quest.5_ROUND") + "");
				}
				if (next_round == 6)
				{
					htmltext = htmltext.replace("MYPRIZE", "" + ServerStorage.getInstance().getString(player.getLang(), "663quest.6_ROUND") + "");
				}
				if (next_round == 7)
				{
					htmltext = htmltext.replace("MYPRIZE", "" + ServerStorage.getInstance().getString(player.getLang(), "663quest.7_ROUND") + "");
				}
				if (next_round == 8)
				{
					next_round = 0;
					st.calcReward(getId(), 8);
					htmltext = "30846-12a.htm";
				}
				st.set("round", String.valueOf(next_round));
			}
		}
		else if (event.equalsIgnoreCase("30846-13.htm"))
		{
			final int round = st.getInt("round");

			if (round == 0)
			{
				return "30846-13a.htm";
			}

			st.set("round", "0");
			htmltext = "30846-13.htm";

			if (round == 1)
			{
				st.calcReward(getId(), 1);
			}
			else if (round == 2)
			{
				st.calcReward(getId(), 2);
			}
			else if (round == 3)
			{
				st.calcReward(getId(), 3);
			}
			else if (round == 4)
			{
				st.calcReward(getId(), 4);
			}
			else if (round == 5)
			{
				st.calcReward(getId(), 10);
				st.calcReward(getId(), 5, true);
			}
			else if (round == 6)
			{
				st.calcReward(getId(), 9);
				st.calcReward(getId(), 6, true);
			}
			else if (round == 7)
			{
				st.calcReward(getId(), 7);
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

		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() < getMinLvl(getId()))
				{
					st.exitQuest(true);
					htmltext = "30846-00.htm";
				}
				else
				{
					htmltext = "30846-01.htm";
				}
				break;
			case State.STARTED:
				htmltext = "30846-03.htm";
				break;
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player member = getRandomPartyMember(player, 1);
		if (member != null)
		{
			final QuestState st = member.getQuestState(getName());
			if (st != null)
			{
				st.calcDropItems(getId(), 8766, npc.getId(), Integer.MAX_VALUE);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _663_SeductiveWhispers(663, _663_SeductiveWhispers.class.getSimpleName(), "");
	}
}
