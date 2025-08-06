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

/**
 * Rework by LordWinter 09.06.2021
 */
public class _281_HeadForTheHills extends Quest
{
	private static final int NEWBIE_REWARD = 4;

	public _281_HeadForTheHills(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32173);
		addTalkId(32173);
		
		addKillId(22234, 22235, 22236, 22237, 22238, 22239);

		questItemIds = new int[]
		{
		        9796
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

		final long hills = st.getQuestItemsCount(9796);

		if (event.equalsIgnoreCase("32173-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32173-06.htm"))
		{
			final int newbie = player.getNewbie();
			if ((newbie | NEWBIE_REWARD) != newbie)
			{
				player.setNewbie(newbie | NEWBIE_REWARD);
				if (player.isMageClass())
				{
					st.calcReward(getId(), 1);
					st.playTutorialVoice("tutorial_voice_027");
				}
				else
				{
					st.calcReward(getId(), 2);
					st.playTutorialVoice("tutorial_voice_026");
				}
				showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_SOULSHOT_FOR_BEGINNERS_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
			}
			st.calcRewardPerItem(getId(), 3, (int) hills);
			st.takeItems(9796, -1);
		}
		else if (event.equalsIgnoreCase("32173-07.htm"))
		{
			if (hills < 50)
			{
				htmltext = "32173-07a.htm";
			}
			else
			{
				final int newbie = player.getNewbie();
				if ((newbie | NEWBIE_REWARD) != newbie)
				{
					player.setNewbie(newbie | NEWBIE_REWARD);
					if (player.isMageClass())
					{
						st.calcReward(getId(), 1);
						st.playTutorialVoice("tutorial_voice_027");
					}
					else
					{
						st.calcReward(getId(), 2);
						st.playTutorialVoice("tutorial_voice_026");
					}
					showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_SOULSHOT_FOR_BEGINNERS_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
				}
				st.takeItems(9796, 50);
				st.calcReward(getId(), 4, true);
			}
		}
		else if (event.equalsIgnoreCase("32173-09.htm"))
		{
			st.takeItems(9796, -1);
			st.exitQuest(true, true);
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

		final int npcId = npc.getId();
		final byte state = st.getState();

		if (npcId == 32173)
		{
			if (state == State.CREATED)
			{
				if (player.getLevel() < getMinLvl(getId()))
				{
					htmltext = "32173-02.htm";
					st.exitQuest(true);
				}
				else
				{
					htmltext = "32173-01.htm";
				}
			}
			else if (state == State.STARTED)
			{
				if (st.getQuestItemsCount(9796) > 0)
				{
					htmltext = "32173-05.htm";
				}
				else
				{
					htmltext = "32173-04.htm";
				}
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player member = getRandomPartyMemberState(player, State.STARTED);
		if (member != null)
		{
			final QuestState st = member.getQuestState(getName());
			if (st != null && npc.getId() >= 22234 && npc.getId() <= 22239)
			{
				st.calcDropItems(getId(), 9796, npc.getId(), Integer.MAX_VALUE);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _281_HeadForTheHills(281, _281_HeadForTheHills.class.getSimpleName(), "");
	}
}