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

/**
 * Created by LordWinter 25.06.2011 Based on L2J Eternity-World
 */
public final class _316_DestroyPlaguebringers extends Quest
{
	private static final String qn = "_316_DestroyPlaguebringers";
	
	// Quest NPCs
	private static final int ELLIASIN = 30155;
	
	// Quest items
	private static final int WERERAT_FANG = 1042;
	private static final int NORMAL_FANG_REWARD = 60;
	private static final int VAROOL_FOULCLAWS_FANG = 1043;
	private static final int LEADER_FANG_REWARD = 10000;
	
	// Quest monsters
	private static final int SUKAR_WERERAT = 20040;
	private static final int SUKAR_WERERAT_LEADER = 20047;
	private static final int VAROOL_FOULCLAW = 27020;
	
	private _316_DestroyPlaguebringers(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(ELLIASIN);
		addTalkId(ELLIASIN);
		
		addKillId(SUKAR_WERERAT);
		addKillId(SUKAR_WERERAT_LEADER);
		addKillId(VAROOL_FOULCLAW);
		
		questItemIds = new int[]
		{
			WERERAT_FANG,
			VAROOL_FOULCLAWS_FANG
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
		
		if (event.equalsIgnoreCase("30155-04.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("30155-08.htm"))
		{
			st.exitQuest(true, true);
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
		
		if (cond == 0)
		{
			if (player.getRace().ordinal() != 1)
			{
				st.exitQuest(true);
				htmltext = "30155-00.htm";
			}
			else if (player.getLevel() < 18)
			{
				st.exitQuest(true);
				htmltext = "30155-02.htm";
			}
			else
			{
				htmltext = "30155-03.htm";
			}
		}
		else
		{
			final long normal = st.getQuestItemsCount(WERERAT_FANG);
			final long leader = st.getQuestItemsCount(VAROOL_FOULCLAWS_FANG);
			if ((normal != 0) || (leader != 0))
			{
				st.takeItems(WERERAT_FANG, normal);
				st.takeItems(VAROOL_FOULCLAWS_FANG, leader);
				st.rewardItems(57, ((normal * NORMAL_FANG_REWARD) + (leader * LEADER_FANG_REWARD)));
				return "30155-07.htm";
			}
			htmltext = "30155-05.htm";
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
			if (npc.getId() == VAROOL_FOULCLAW)
			{
				if (st.getQuestItemsCount(VAROOL_FOULCLAWS_FANG) == 0)
				{
					st.giveItems(VAROOL_FOULCLAWS_FANG, 1);
					st.playSound("ItemSound.quest_middle");
				}
			}
			else
			{
				st.giveItems(WERERAT_FANG, 1);
				st.playSound("ItemSound.quest_itemget");
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _316_DestroyPlaguebringers(316, qn, "");
	}
}