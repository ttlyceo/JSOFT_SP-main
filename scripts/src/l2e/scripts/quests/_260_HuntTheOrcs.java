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

public class _260_HuntTheOrcs extends Quest
{
	private static final String qn = "_260_HuntTheOrcs";

	private static final int RAYEN = 30221;

	// Items
	private static final int ORC_AMULET = 1114;
	private static final int ORCS_NECKLACE = 1115;

	// Monsters
	private static final int KABOO_ORC = 20468;
	private static final int KABOO_ORC_ARCHER = 20469;
	private static final int KABOO_ORC_GRUNT = 20470;
	private static final int KABOO_ORC_FIGHTER = 20471;
	private static final int KABOO_ORC_FIGHTER_LEADER = 20472;
	private static final int KABOO_ORC_FIGHTER_LIEUTENANT = 20473;

	// Newbie section
	private static final int NEWBIE_REWARD = 4;
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;

	public _260_HuntTheOrcs(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(RAYEN);
		addTalkId(RAYEN);

		addKillId(KABOO_ORC, KABOO_ORC_ARCHER, KABOO_ORC_GRUNT, KABOO_ORC_FIGHTER, KABOO_ORC_FIGHTER_LEADER, KABOO_ORC_FIGHTER_LIEUTENANT);

		questItemIds = new int[]
		{
		                ORC_AMULET, ORCS_NECKLACE
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30221-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30221-06.htm"))
		{
			st.exitQuest(true, true);
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
			case State.CREATED:
				if (player.getRace().ordinal() == 1)
				{
					if ((player.getLevel() >= 6) && (player.getLevel() <= 16))
					{
						htmltext = "30221-02.htm";
					}
					else
					{
						htmltext = "30221-01.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30221-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				final long amulet = st.getQuestItemsCount(ORC_AMULET);
				final long necklace = st.getQuestItemsCount(ORCS_NECKLACE);

				if ((amulet == 0) && (necklace == 0))
				{
					htmltext = "30221-04.htm";
				}
				else
				{
					final int newbie = player.getNewbie();

					if ((newbie | NEWBIE_REWARD) != newbie)
					{
						player.setNewbie(newbie | NEWBIE_REWARD);
						st.showQuestionMark(26, 1);

						if (player.getClassId().isMage())
						{
							st.playTutorialVoice("tutorial_voice_027");
							st.giveItems(SPIRITSHOT_FOR_BEGINNERS, 3000);
						}
						else
						{
							st.playTutorialVoice("tutorial_voice_026");
							st.giveItems(SOULSHOT_FOR_BEGINNERS, 6000);
						}
						showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_SOULSHOT_FOR_BEGINNERS_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
					}
					htmltext = "30221-05.htm";
					st.takeItems(ORC_AMULET, -1);
					st.takeItems(ORCS_NECKLACE, -1);
					st.giveItems(57, (amulet * 5) + (necklace * 15));
				}
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
		if (st != null)
		{
			switch (npc.getId())
			{
				case KABOO_ORC:
				case KABOO_ORC_GRUNT:
				case KABOO_ORC_ARCHER:
					if (getRandom(10) < 4)
					{
						st.giveItems(ORC_AMULET, 1);
					}
					break;

				case KABOO_ORC_FIGHTER:
				case KABOO_ORC_FIGHTER_LEADER:
				case KABOO_ORC_FIGHTER_LIEUTENANT:
					if (getRandom(10) < 4)
					{
						st.giveItems(ORCS_NECKLACE, 1);
					}
					break;
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _260_HuntTheOrcs(260, qn, "");
	}
}
