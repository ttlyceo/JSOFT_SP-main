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

public class _283_TheFewTheProudTheBrave extends Quest
{
	private static final String qn = "_283_TheFewTheProudTheBrave";

	private static int PERWAN = 32133;

	// Mobs
	private static int CRIMSON_SPIDER = 22244;

	// QuestItems
	private static int CRIMSON_SPIDER_CLAW = 9747;

	public _283_TheFewTheProudTheBrave(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(PERWAN);
		addTalkId(PERWAN);

		addKillId(CRIMSON_SPIDER);

		questItemIds = new int[]
		{
		                CRIMSON_SPIDER_CLAW
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
		
		if (event.equalsIgnoreCase("32133-03.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32133-06.htm"))
		{
			final long count = st.getQuestItemsCount(CRIMSON_SPIDER_CLAW);
			if (count > 0)
			{
				st.takeItems(CRIMSON_SPIDER_CLAW, -1);
				st.giveItems(57, 45 * count);
				st.playSound("ItemSound.quest_middle");
				final var qs = player.getQuestState("NewbieGuideSystem");
				if (qs != null && qs.getInt("finalStep") == 0)
				{
					qs.set("finalStep", 1);
					showOnScreenMsg(player, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
				}
			}
		}
		else if (event.equalsIgnoreCase("32133-08.htm"))
		{
			st.takeItems(CRIMSON_SPIDER_CLAW, -1);
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

		final int id = st.getState();
		final long claw = st.getQuestItemsCount(CRIMSON_SPIDER_CLAW);

		if ((id == State.CREATED) && (npc.getId() == PERWAN))
		{
			if (player.getLevel() < 15)
			{
				htmltext = "32133-02.htm";
				st.exitQuest(true);
			}
			else
			{
				htmltext = "32133-01.htm";
			}
		}
		else if ((id == State.STARTED) && (npc.getId() == PERWAN))
		{
			if (claw > 0)
			{
				htmltext = "32133-05.htm";
			}
			else
			{
				htmltext = "32133-04.htm";
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
			final int chance = getRandom(100);
			
			if ((npcId == CRIMSON_SPIDER) && (chance < 35))
			{
				st.giveItems(CRIMSON_SPIDER_CLAW, 1);
				st.playSound("ItemSound.quest_itemget");
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _283_TheFewTheProudTheBrave(283, qn, "");
	}
}
