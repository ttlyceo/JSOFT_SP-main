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

public class _151_CureForFeverDisease extends Quest
{
	private final static String qn = "_151_CureForFeverDisease";

	private static final int POISON_SAC = 703;
	private static final int FEVER_MEDICINE = 704;

	// NPCs
	private static final int ELIAS = 30050;
	private static final int YOHANES = 30032;

	public _151_CureForFeverDisease(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(ELIAS);
		addTalkId(ELIAS, YOHANES);

		addKillId(20103, 20106, 20108);

		questItemIds = new int[]
		{
		                FEVER_MEDICINE, POISON_SAC
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

		if (event.equalsIgnoreCase("30050-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		switch (st.getState())
		{
			case State.CREATED:
				if ((player.getLevel() >= 15) && (player.getLevel() <= 21))
				{
					htmltext = "30050-02.htm";
				}
				else
				{
					htmltext = "30050-01.htm";
					st.exitQuest(true);
				}
				break;

			case State.STARTED:
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ELIAS:
						if (cond == 1)
						{
							htmltext = "30050-04.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30050-05.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30050-06.htm";
							st.takeItems(FEVER_MEDICINE, 1);
							st.giveItems(102, 1);
							st.addExpAndSp(13106, 613);
							st.exitQuest(false, true);
							showOnScreenMsg(player, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
						}
						break;
					case YOHANES:
						if (cond == 2)
						{
							htmltext = "30032-01.htm";
							st.set("cond", "3");
							st.takeItems(POISON_SAC, 1);
							st.giveItems(FEVER_MEDICINE, 1);
							st.playSound("ItemSound.quest_middle");
						}
						else if (cond == 3)
						{
							htmltext = "30032-02.htm";
						}
						break;
				}
				break;
			case State.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg(player);
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null && getRandom(5) == 0)
		{
			st.set("cond", "2");
			st.giveItems(POISON_SAC, 1);
			st.playSound("ItemSound.quest_itemget");
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _151_CureForFeverDisease(151, qn, "");
	}
}
