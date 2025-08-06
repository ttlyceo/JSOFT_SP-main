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
 * Rework by LordWinter 15.12.2019
 */
public class _102_FungusFever extends Quest
{
	public _102_FungusFever(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30284);
		addTalkId(30156, 30217, 30219, 30221, 30284, 30285);

		addKillId(20013, 20019);

		questItemIds = new int[]
		{
		        964, 965, 966, 1130, 1131, 1132, 1133, 1134, 746
		};
	}

	private void check(QuestState st)
	{
		if ((st.getQuestItemsCount(1131) == 0) && (st.getQuestItemsCount(1132) == 0) && (st.getQuestItemsCount(1133) == 0) && (st.getQuestItemsCount(1134) == 0))
		{
			st.setCond(6, true);
		}
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

		if (event.equalsIgnoreCase("1"))
		{
			if (st.isCreated())
			{
				htmltext = "30284-02.htm";
				st.giveItems(964, 1);
				st.startQuest();
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

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (npcId == 30284)
		{
			if (cond == 0)
			{
				if (player.getRace().ordinal() != 1)
				{
					htmltext = "30284-00.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "30284-07.htm";
					return htmltext;
				}
				else
				{
					htmltext = "30284-08.htm";
					st.exitQuest(true);
				}

			}
			else if ((cond == 1) && (st.getQuestItemsCount(964) == 1))
			{
				htmltext = "30284-03.htm";
			}
			else if ((cond == 2) && (st.getQuestItemsCount(965) == 1))
			{
				htmltext = "30284-09.htm";
			}
			else if ((cond == 4) && (st.getQuestItemsCount(1130) == 1))
			{
				st.setCond(5, true);
				st.takeItems(1130, 1);
				st.giveItems(746, 1);
				htmltext = "30284-04.htm";
			}
			else if (cond == 5)
			{
				htmltext = "30284-05.htm";
			}
			else if ((cond == 6) && (st.getQuestItemsCount(746) == 1))
			{
				st.takeItems(746, 1);
				st.exitQuest(false, true);
				htmltext = "30284-06.htm";
				if (player.getClassId().isMage())
				{
					st.calcReward(getId(), 1);
				}
				else
				{
					st.calcReward(getId(), 2);
				}
			}
		}
		else if (npcId == 30156)
		{
			if ((cond == 1) && (st.getQuestItemsCount(964) == 1))
			{
				st.takeItems(964, 1);
				st.giveItems(965, 1);
				st.setCond(2, true);
				htmltext = "30156-03.htm";
			}
			else if ((cond == 2) && (st.getQuestItemsCount(965) > 0) && (st.getQuestItemsCount(966) < 10))
			{
				htmltext = "30156-04.htm";
			}
			else if ((cond > 3) && (st.getQuestItemsCount(746) > 0))
			{
				htmltext = "30156-07.htm";
			}
			else if ((cond == 3) && (st.getQuestItemsCount(965) > 0) && (st.getQuestItemsCount(966) >= 10))
			{
				st.takeItems(965, 1);
				st.takeItems(966, -1);
				st.giveItems(1130, 1);
				st.giveItems(1131, 1);
				st.giveItems(1132, 1);
				st.giveItems(1133, 1);
				st.giveItems(1134, 1);
				st.setCond(4, true);
				htmltext = "30156-05.htm";
			}
			else if (cond == 4)
			{
				htmltext = "30156-06.htm";
			}
		}
		else if ((npcId == 30217) && (cond == 5) && (st.getQuestItemsCount(746) == 1) && (st.getQuestItemsCount(1131) == 1))
		{
			st.takeItems(1131, 1);
			htmltext = "30217-01.htm";
			check(st);
		}
		else if ((npcId == 30219) && (cond == 5) && (st.getQuestItemsCount(746) == 1) && (st.getQuestItemsCount(1132) == 1))
		{
			st.takeItems(1132, 1);
			htmltext = "30219-01.htm";
			check(st);
		}
		else if ((npcId == 30221) && (cond == 5) && (st.getQuestItemsCount(746) == 1) && (st.getQuestItemsCount(1133) == 1))
		{
			st.takeItems(1133, 1);
			htmltext = "30221-01.htm";
			check(st);
		}
		else if ((npcId == 30285) && (cond == 5) && (st.getQuestItemsCount(746) == 1) && (st.getQuestItemsCount(1134) == 1))
		{
			st.takeItems(1134, 1);
			htmltext = "30285-01.htm";
			check(st);
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(killer, 2);
		if (partyMember == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if ((npc.getId() == 20013) || (npc.getId() == 20019))
		{
			if (st.calcDropItems(getId(), 966, npc.getId(), 10))
			{
				st.setCond(3);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}

	public static void main(String[] args)
	{
		new _102_FungusFever(102, _102_FungusFever.class.getSimpleName(), "");
	}
}
