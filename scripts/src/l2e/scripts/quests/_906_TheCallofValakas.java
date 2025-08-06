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
import l2e.gameserver.model.quest.QuestState.QuestType;
import l2e.gameserver.model.quest.State;

/**
 * Created by LordWinter 11.05.2012 Based on L2J Eternity-World
 */
public class _906_TheCallofValakas extends Quest
{
	private static final String qn = "_906_TheCallofValakas";

	private static final int Klein = 31540;
	private static final int LavasaurusAlphaFragment = 21993;
	private static final int ValakasMinion = 29029;

	public _906_TheCallofValakas(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(Klein);
		addTalkId(Klein);

		addKillId(ValakasMinion);

		questItemIds = new int[]
		{
		                LavasaurusAlphaFragment
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null || (st.isCompleted() && !st.isNowAvailable()))
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("31540-04.htm"))
		{
			if (st.isCreated())
			{
				st.setState(State.STARTED);
				st.set("cond", "1");
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("31540-07.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(LavasaurusAlphaFragment, -1);
				st.giveItems(21895, 1);
				st.setState(State.COMPLETED);
				st.playSound("ItemSound.quest_finish");
				st.exitQuest(QuestType.DAILY);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		final int cond = st.getInt("cond");

		if (npc.getId() == Klein)
		{
			switch (st.getState())
			{
				case State.CREATED:
					if (player.getLevel() >= 83)
					{
						if (st.getQuestItemsCount(7267) > 0)
						{
							htmltext = "31540-01.htm";
						}
						else
						{
							htmltext = "31540-00b.htm";
						}
					}
					else
					{
						htmltext = "31540-00.htm";
						st.exitQuest(true);
					}
					break;
				case State.STARTED:
					if (cond == 1)
					{
						htmltext = "31540-05.htm";
					}
					else if (cond == 2)
					{
						htmltext = "31540-06.htm";
					}
					break;
				case State.COMPLETED:
					if (st.isNowAvailable())
					{
						if (player.getLevel() >= 83)
						{
							if (st.getQuestItemsCount(7267) > 0)
							{
								htmltext = "31540-01.htm";
							}
							else
							{
								htmltext = "31540-00b.htm";
							}
						}
						else
						{
							htmltext = "31540-00.htm";
							st.exitQuest(true);
						}
					}
					else
					{
						htmltext = "31540-00a.htm";
					}
					break;
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);

		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final QuestState st = partyMember.getQuestState(qn);
		if (st == null)
		{
			return null;
		}

		final int cond = st.getInt("cond");
		final int npcId = npc.getId();

		if (cond == 1)
		{
			if ((npcId == ValakasMinion) && Rnd.calcChance(40))
			{
				st.giveItems(LavasaurusAlphaFragment, 1);
				st.set("cond", "2");
				st.playSound("ItemSound.quest_middle");
			}
		}

		if (player.getParty() != null)
		{
			QuestState st2;
			for (final Player pmember : player.getParty().getMembers())
			{
				st2 = pmember.getQuestState(qn);

				if ((st2 != null) && (cond == 1) && (pmember.getObjectId() != partyMember.getObjectId()))
				{
					if ((npcId == ValakasMinion) && Rnd.calcChance(40))
					{
						st.giveItems(LavasaurusAlphaFragment, 1);
						st.set("cond", "2");
						st.playSound("ItemSound.quest_middle");
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _906_TheCallofValakas(906, qn, "");
	}
}
