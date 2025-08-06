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

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowInfoUpdate;

/**
 * Created by LordWinter 20.04.2011
 * Based on L2J Eternity-World
 */
public class _510_AClansReputation extends Quest
{
	private static final String qn = "_510_AClansReputation";
	
	// NPC
	private static final int VALDIS = 31331;
	
	// Quest Item
	private static final int TYRANNOSAURUS_CLAW = 8767;
	
	private static final int[] MOBS =
	{
		22215,
		22216,
		22217
	};


	public _510_AClansReputation(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(VALDIS);
		addTalkId(VALDIS);
		addKillId(MOBS);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		switch (event)
		{
			case "31331-3.html":
				if (st.isCreated())
				{
					st.startQuest();
				}
				break;
			case "31331-6.html":
				st.exitQuest(true, true);
				break;
		}
		return event;
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
		
		final Clan clan = player.getClan();
		
		switch (st.getState())
		{
			case State.CREATED:
				htmltext = (clan == null || !player.isClanLeader() || clan.getLevel() < 5) ? "31331-0.htm" : "31331-1.htm";
				break;
			case State.STARTED:
				if ((clan == null) || !player.isClanLeader())
				{
					st.exitQuest(true);
					return "31331-8.html";
				}
				
				if (!st.hasQuestItems(TYRANNOSAURUS_CLAW))
				{
					htmltext = "31331-4.html";
				}
				else
				{
					final int count = (int) st.getQuestItemsCount(TYRANNOSAURUS_CLAW);
					final int reward = (count < 10) ? (30 * count) : (118 + 30 * count);
					st.playSound("ItemSound.quest_fanfare_1");
					st.takeItems(TYRANNOSAURUS_CLAW, -1);
					clan.addReputationScore(reward, true);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_QUEST_COMPLETED_AND_S1_POINTS_GAINED).addNumber(reward));
					clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
					htmltext = "31331-7.html";
				}
				break;
			default:
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (player.getClan() == null)
		{
			return null;
		}
		
		QuestState st = null;
		
		if (player.isClanLeader())
		{
			st = player.getQuestState(qn);
		}
		else
		{
			final Player pleader = player.getClan().getLeader().getPlayerInstance();
			if (pleader != null && player.isInsideRadius(pleader, 1500, true, false))
			{
				st = pleader.getQuestState(qn);
			}
		}
		
		if (st != null && st.isStarted())
		{
			st.rewardItems(TYRANNOSAURUS_CLAW, 1);
			st.playSound("ItemSound.quest_itemget");
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _510_AClansReputation(510, qn, "");
	}
}
