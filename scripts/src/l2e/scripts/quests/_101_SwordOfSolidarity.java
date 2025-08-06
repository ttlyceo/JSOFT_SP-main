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
 * Rework by LordWinter 13.12.2019
 */
public class _101_SwordOfSolidarity extends Quest
{
	public _101_SwordOfSolidarity(int id, String name, String desc)
	{
		super(id, name, desc);

		addStartNpc(30008);
		addTalkId(30008, 30283);

		addKillId(20361, 20362);

		questItemIds = new int[]
		{
		        796, 937, 739, 740, 741, 742
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("30008-04.htm") && npc.getId() == 30008)
		{
			if (st.isCreated())
			{
				st.startQuest();
				st.giveItems(796, 1);
			}
		}
		else if (event.equalsIgnoreCase("30283-02.htm") && npc.getId() == 30283)
		{
			if (st.isCond(1))
			{
				st.takeItems(796, st.getQuestItemsCount(796));
				st.giveItems(937, 1);
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("30283-07.htm") && npc.getId() == 30283)
		{
			if (st.isCond(5))
			{
				st.takeItems(739, -1);
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
				if (!player.getClassId().isMage())
				{
					st.playTutorialVoice("tutorial_voice_027");
					st.giveItems(5790, 3500);
				}
				else
				{
					st.playTutorialVoice("tutorial_voice_026");
					st.giveItems(5789, 7000);
				}
				showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_RACE_SPECIFIC_WEAPON_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
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
		final int id = st.getState();

		switch (id)
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 30008)
				{
					if (player.getRace().ordinal() != 0)
					{
						htmltext = "30008-00.htm";
					}
					else if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "30008-02.htm";
					}
					else
					{
						htmltext = "30008-08.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED:
				if (npcId == 30008)
				{
					if (st.isCond(1) && (st.getQuestItemsCount(796) == 1))
					{
						htmltext = "30008-05.htm";
					}
					else if ((st.getCond() >= 2) && (st.getQuestItemsCount(796) == 0) && (st.getQuestItemsCount(742) == 0))
					{
						if ((st.getQuestItemsCount(741) > 0) && (st.getQuestItemsCount(740) > 0))
						{
							htmltext = "30008-12.htm";
						}
						else if ((st.getQuestItemsCount(741) + st.getQuestItemsCount(740)) <= 1)
						{
							htmltext = "30008-11.htm";
						}
						else if (st.getQuestItemsCount(739) > 0)
						{
							htmltext = "30008-07.htm";
						}
						else if (st.getQuestItemsCount(937) == 1)
						{
							htmltext = "30008-10.htm";
						}
					}
					else if (st.isCond(4) && (st.getQuestItemsCount(796) == 0) && (st.getQuestItemsCount(742) > 0))
					{
						htmltext = "30008-06.htm";
						st.takeItems(742, st.getQuestItemsCount(742));
						st.giveItems(739, 1);
						st.setCond(5, true);
					}
				}
				else if (npcId == 30283)
				{
					if (st.isCond(1) && (st.getQuestItemsCount(796) > 0))
					{
						htmltext = "30283-01.htm";
					}
					else if ((st.getCond() >= 2) && (st.getQuestItemsCount(796) == 0) && (st.getQuestItemsCount(937) > 0))
					{
						if ((st.getQuestItemsCount(741) + st.getQuestItemsCount(740)) == 1)
						{
							htmltext = "30283-08.htm";
						}
						else if ((st.getQuestItemsCount(741) + st.getQuestItemsCount(740)) == 0)
						{
							htmltext = "30283-03.htm";
						}
						else if ((st.getQuestItemsCount(741) > 0) && (st.getQuestItemsCount(740) > 0))
						{
							htmltext = "30283-04.htm";
							st.takeItems(937, st.getQuestItemsCount(937));
							st.takeItems(741, st.getQuestItemsCount(741));
							st.takeItems(740, st.getQuestItemsCount(740));
							st.giveItems(742, 1);
							st.setCond(4, true);
						}
					}
					else if ((st.getInt("cond") == 4) && (st.getQuestItemsCount(742) > 0))
					{
						htmltext = "30283-05.htm";
					}
					else if ((st.getInt("cond") == 5) && (st.getQuestItemsCount(739) > 0))
					{
						htmltext = "30283-06.htm";
					}
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}

		if ((npc.getId() == 20361) || (npc.getId() == 20362))
		{
			if (st.getQuestItemsCount(937) > 0)
			{
				st.calcDoDropItems(getId(), 741, npc.getId(), 1);
				st.calcDoDropItems(getId(), 740, npc.getId(), 1);
			}
				
			if (st.isCond(2) && (st.getQuestItemsCount(741) > 0) && (st.getQuestItemsCount(740) > 0))
			{
				st.setCond(3, true);
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _101_SwordOfSolidarity(101, _101_SwordOfSolidarity.class.getSimpleName(), "");
	}
}