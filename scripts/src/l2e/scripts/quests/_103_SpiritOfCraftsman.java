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
 * Rework by LordWinter 18.11.2021
 */
public class _103_SpiritOfCraftsman extends Quest
{
	public _103_SpiritOfCraftsman(int id, String name, String desc)
	{
		super(id, name, desc);

		addStartNpc(30307);
		addTalkId(30307, 30132, 30144);

		addKillId(20015, 20020, 20455, 20517, 20518);

		questItemIds = new int[]
		{
		        968, 969, 970, 1107, 971, 972, 973, 974
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

		if (event.equalsIgnoreCase("30307-05.htm") && npc.getId() == 30307)
		{
			if (st.isCreated())
			{
				st.startQuest();
				st.giveItems(968, 1);
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

		if (st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		
		if ((npcId == 30307) && (st.getState() == State.CREATED))
		{
			if (player.getRace().ordinal() != 2)
			{
				htmltext = "30307-00.htm";
			}
			else if (player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "30307-03.htm";
			}
			else
			{
				st.exitQuest(true);
				htmltext = "30307-02.htm";
			}

		}
		else if (st.getState() == State.STARTED)
		{
			if ((npcId == 30307) && (st.getCond() >= 1) && ((st.getQuestItemsCount(968) >= 1) || (st.getQuestItemsCount(969) >= 1) || (st.getQuestItemsCount(970) >= 1)))
			{
				htmltext = "30307-06.htm";
			}
			else if ((npcId == 30132) && st.isCond(1) && (st.getQuestItemsCount(968) == 1))
			{
				htmltext = "30132-01.htm";
				st.takeItems(968, 1);
				st.giveItems(969, 1);
				st.setCond(2, true);
			}
			else if (npcId == 30132 && ((st.getCond() >= 2) && (st.getQuestItemsCount(969) >= 1) || (st.getQuestItemsCount(970) >= 1)))
			{
				htmltext = "30132-02.htm";
			}
			else if ((npcId == 30144) && st.isCond(2) && (st.getQuestItemsCount(969) >= 1))
			{
				htmltext = "30144-01.htm";
				st.takeItems(969, 1);
				st.giveItems(970, 1);
				st.setCond(3, true);
			}
			else if ((npcId == 30144) && st.isCond(3) && (st.getQuestItemsCount(970) >= 1) && (st.getQuestItemsCount(1107) < 10))
			{
				htmltext = "30144-02.htm";
			}
			else if ((npcId == 30144) && st.isCond(4) && (st.getQuestItemsCount(970) == 1) && (st.getQuestItemsCount(1107) >= 10))
			{
				htmltext = "30144-03.htm";
				st.takeItems(970, 1);
				st.takeItems(1107, 10);
				st.giveItems(971, 1);
				st.setCond(5, true);
			}
			else if ((npcId == 30144) && st.isCond(5) && (st.getQuestItemsCount(971) == 1))
			{
				htmltext = "30144-04.htm";
			}
			else if ((npcId == 30132) && st.isCond(5) && (st.getQuestItemsCount(971) == 1))
			{
				htmltext = "30132-03.htm";
				st.takeItems(971, 1);
				st.giveItems(972, 1);
				st.setCond(6, true);
			}
			else if ((npcId == 30132) && st.isCond(6) && (st.getQuestItemsCount(972) == 1) && (st.getQuestItemsCount(973) == 0) && (st.getQuestItemsCount(974) == 0))
			{
				htmltext = "30132-04.htm";
			}
			else if ((npcId == 30132) && st.isCond(7) && (st.getQuestItemsCount(973) == 1))
			{
				htmltext = "30132-05.htm";
				st.takeItems(973, 1);
				st.giveItems(974, 1);
				st.setCond(8, true);
			}
			else if ((npcId == 30132) && st.isCond(8) && (st.getQuestItemsCount(974) == 1))
			{
				htmltext = "30132-06.htm";
			}
			else if ((npcId == 30307) && st.isCond(8) && (st.getQuestItemsCount(974) == 1))
			{
				htmltext = "30307-07.htm";
				st.takeItems(974, 1);
				st.calcExpAndSp(getId());
				if (player.getClassId().isMage())
				{
					st.calcReward(getId(), 1);
					st.playTutorialVoice("tutorial_voice_027");
				}
				else
				{
					st.calcReward(getId(), 2);
					st.playTutorialVoice("tutorial_voice_026");
				}
				showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_RACE_SPECIFIC_WEAPON_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if ((st.getQuestItemsCount(970) == 1) && ((npc.getId() == 20517) || (npc.getId() == 20518) || (npc.getId() == 20455)))
			{
				if (st.calcDropItems(getId(), 1107, npc.getId(), 10))
				{
					st.setCond(4, true);
				}
			}
			else if (st.getQuestItemsCount(972) == 1 && ((npc.getId() == 20015) || (npc.getId() == 20020)))
			{
				if (st.calcDropItems(getId(), 973, npc.getId(), 1))
				{
					st.takeItems(972, 1);
					st.setCond(7, true);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _103_SpiritOfCraftsman(103, _103_SpiritOfCraftsman.class.getSimpleName(), "");
	}
}
