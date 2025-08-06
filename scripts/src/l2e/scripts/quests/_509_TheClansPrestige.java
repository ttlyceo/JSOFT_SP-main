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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.RadarControl;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowInfoUpdate;

public class _509_TheClansPrestige extends Quest
{
	private static final String qn = "_509_TheClansPrestige";

	// NPC
	private static final int VALDIS = 31331;
	
	private static final Map<Integer, List<Integer>> REWARD_POINTS = new HashMap<>();
	
	static
	{
		REWARD_POINTS.put(1, Arrays.asList(25290, 8489, 1378));
		REWARD_POINTS.put(2, Arrays.asList(25293, 8490, 1378));
		REWARD_POINTS.put(3, Arrays.asList(25523, 8491, 1070));
		REWARD_POINTS.put(4, Arrays.asList(25322, 8492, 782));
	}
	
	private static final int[] RAID_BOSS =
	{
		25290,
		25293,
		25523,
		25322
	};

	public _509_TheClansPrestige(int id, String name, String descr)
	{
		super(id, name, descr);
		
		addStartNpc(VALDIS);
		addTalkId(VALDIS);
		addKillId(RAID_BOSS);
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
			case "31331-0.htm":
				if (st.isCreated())
				{
					st.startQuest();
				}
				break;
			case "31331-1.htm":
				st.set("raid", "1");
				player.sendPacket(new RadarControl(0, 2, 186304, -43744, -3193));
				break;
			case "31331-2.htm":
				st.set("raid", "2");
				player.sendPacket(new RadarControl(0, 2, 134672, -115600, -1216));
				break;
			case "31331-3.htm":
				st.set("raid", "3");
				player.sendPacket(new RadarControl(0, 2, 170000, -60000, -3500));
				break;
			case "31331-4.htm":
				st.set("raid", "4");
				player.sendPacket(new RadarControl(0, 2, 93296, -75104, -1824));
				break;
			case "31331-5.htm":
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
				htmltext = (clan == null || !player.isClanLeader() || clan.getLevel() < 6) ? "31331-0a.htm" : "31331-0b.htm";
				break;
			case State.STARTED:
				if (clan == null || !player.isClanLeader())
				{
					st.exitQuest(true);
					return "31331-6.htm";
				}
				
				final int raid = st.getInt("raid");
				
				if (REWARD_POINTS.containsKey(raid))
				{
					if (st.hasQuestItems(REWARD_POINTS.get(raid).get(1)))
					{
						htmltext = "31331-" + raid + "b.htm";
						st.playSound("ItemSound.quest_fanfare_1");
						st.takeItems(REWARD_POINTS.get(raid).get(1), -1);
						final int rep = REWARD_POINTS.get(raid).get(2);
						clan.addReputationScore(rep, true);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_QUEST_COMPLETED_AND_S1_POINTS_GAINED).addNumber(rep));
						clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
					}
					else
					{
						htmltext = "31331-" + raid + "a.htm";
					}
				}
				else
				{
					htmltext = "31331-0.htm";
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
			final int raid = st.getInt("raid");
			if (REWARD_POINTS.containsKey(raid))
			{
				if ((npc.getId() == REWARD_POINTS.get(raid).get(0)) && !st.hasQuestItems(REWARD_POINTS.get(raid).get(1)))
				{
					st.rewardItems(REWARD_POINTS.get(raid).get(1), 1);
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new _509_TheClansPrestige(509, qn, "");
	}
}