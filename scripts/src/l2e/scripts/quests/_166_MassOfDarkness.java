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

public class _166_MassOfDarkness extends Quest
{
	private static final String qn = "_166_MassOfDarkness";
	
	private final static int UNDRIAS = 30130;
	private final static int IRIA = 30135;
	private final static int DORANKUS = 30139;
	private final static int TRUDY = 30143;
	
	// Items
	private final static int UNDRIAS_LETTER = 1088;
	private final static int CEREMONIAL_DAGGER = 1089;
	private final static int DREVIANT_WINE = 1090;
	private final static int GARMIELS_SCRIPTURE = 1091;
	private final static int ADENA = 57;
	
	public _166_MassOfDarkness(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(UNDRIAS);
		addTalkId(UNDRIAS, IRIA, DORANKUS, TRUDY);
		
		questItemIds = new int[]
		{
		        UNDRIAS_LETTER, CEREMONIAL_DAGGER, DREVIANT_WINE, GARMIELS_SCRIPTURE
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("30130-04.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.giveItems(UNDRIAS_LETTER, 1);
				st.playSound("ItemSound.quest_accept");
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg(player);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				if (player.getRace().ordinal() == 2)
				{
					if ((player.getLevel() >= 2) && (player.getLevel() <= 5))
					{
						htmltext = "30130-03.htm";
					}
					else
					{
						htmltext = "30130-00.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30130-02.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED :
				final int cond = st.getInt("cond");
				switch (npc.getId())
				{
					case UNDRIAS :
						if (cond == 1)
						{
							htmltext = "30130-05.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30130-06.htm";
							st.takeItems(CEREMONIAL_DAGGER, 1);
							st.takeItems(DREVIANT_WINE, 1);
							st.takeItems(GARMIELS_SCRIPTURE, 1);
							st.takeItems(UNDRIAS_LETTER, 1);
							st.rewardItems(ADENA, 500);
							st.addExpAndSp(5672, 446);
							st.exitQuest(false, true);
							showOnScreenMsg(player, NpcStringId.DELIVERY_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
						}
						break;
					case IRIA :
						if (st.getQuestItemsCount(CEREMONIAL_DAGGER) == 0)
						{
							st.giveItems(CEREMONIAL_DAGGER, 1);
							htmltext = "30135-01.htm";
						}
						else
						{
							htmltext = "30135-02.htm";
						}
						break;
					case DORANKUS :
						if (st.getQuestItemsCount(DREVIANT_WINE) == 0)
						{
							st.giveItems(DREVIANT_WINE, 1);
							htmltext = "30139-01.htm";
						}
						else
						{
							htmltext = "30139-02.htm";
						}
						break;
					case TRUDY :
						if (st.getQuestItemsCount(GARMIELS_SCRIPTURE) == 0)
						{
							st.giveItems(GARMIELS_SCRIPTURE, 1);
							htmltext = "30143-01.htm";
						}
						else
						{
							htmltext = "30143-02.htm";
						}
						break;
				}
				if ((cond == 1) && ((st.getQuestItemsCount(CEREMONIAL_DAGGER) + st.getQuestItemsCount(DREVIANT_WINE) + st.getQuestItemsCount(GARMIELS_SCRIPTURE)) >= 3))
				{
					st.set("cond", "2");
					st.playSound("ItemSound.quest_middle");
				}
				break;
			case State.COMPLETED :
				htmltext = Quest.getAlreadyCompletedMsg(player);
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _166_MassOfDarkness(166, qn, "");
	}
}
