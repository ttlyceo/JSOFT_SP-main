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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;

public class _265_ChainsOfSlavery extends Quest
{
	private static final String qn = "_265_ChainsOfSlavery";

	private static final int KRISTIN = 30357;

	// MOBS
	private static final int IMP = 20004;
	private static final int IMP_ELDER = 20005;

	// ITEMS
	private static final int IMP_SHACKLES = 1368;

	// Newbie section
	private static final int NEWBIE_REWARD = 4;
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;

	public _265_ChainsOfSlavery(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(KRISTIN);
		addTalkId(KRISTIN);

		addKillId(IMP);
		addKillId(IMP_ELDER);

		questItemIds = new int[]
		{
		                IMP_SHACKLES
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

		if (event.equalsIgnoreCase("30357-03.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30357-06.htm"))
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

		final int cond = st.getInt("cond");
		final int id = st.getState();

		if (id == State.CREATED)
		{
			st.set("cond", "0");
		}

		if (cond == 0)
		{
			if (player.getRace().ordinal() != 2)
			{
				htmltext = "30357-00.htm";
				st.exitQuest(true);
			}
			else
			{
				if (player.getLevel() < 6)
				{
					htmltext = "30357-01.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "30357-02.htm";
				}
			}
		}
		else
		{
			final long count = st.getQuestItemsCount(IMP_SHACKLES);

			if (count > 0)
			{
				if (count >= 10)
				{
					st.giveItems(57, (12 * count) + 500);
				}
				else
				{
					st.giveItems(57, 12 * count);
				}

				st.takeItems(IMP_SHACKLES, -1);

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
				htmltext = "30357-05.htm";
			}
			else
			{
				htmltext = "30357-04.htm";
			}
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
			final int npcId = npc.getId();
			if (Rnd.getChance((5 + npcId) - 20004))
			{
				st.giveItems(IMP_SHACKLES, 1);
				st.playSound("ItemSound.quest_itemget");
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _265_ChainsOfSlavery(265, qn, "");
	}
}
