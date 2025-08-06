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

import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 09.06.2021
 */
public final class _643_RiseandFalloftheElrokiTribe extends Quest
{
	private static final Map<String, int[]> _requests = new HashMap<>();
	static
	{
		_requests.put("1", new int[]
		{
			9492,
			400
		});
		_requests.put("2", new int[]
		{
			9493,
			250
		});
		_requests.put("3", new int[]
		{
			9494,
			200
		});
		_requests.put("4", new int[]
		{
			9495,
			134
		});
		_requests.put("5", new int[]
		{
			9496,
			134
		});
		_requests.put("6", new int[]
		{
			10115,
			287
		});
	}
	
	public _643_RiseandFalloftheElrokiTribe(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32106);
		addTalkId(32106, 32117);
		
		addKillId(22201, 22202, 22204, 22205, 22209, 22210, 22212, 22213, 22219, 22220, 22221, 22222, 22224, 22225, 22226, 22227, 22742, 22743, 22744, 22745);
		
		questItemIds = new int[]
		{
		        8776
		};
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		final long count = st.getQuestItemsCount(8776);
		
		if (event.equalsIgnoreCase("32106-03.htm") && npc.getId() == 32106)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32117-03.htm") && npc.getId() == 32117)
		{
			if (count >= 300)
			{
				st.takeItems(8776, 300);
				st.calcReward(getId(), 8, true);
			}
			else
			{
				htmltext = "32117-04.htm";
			}
		}
		else if (_requests.containsKey(event))
		{
			if (count >= _requests.get(event)[1])
			{
				st.takeItems(8776, _requests.get(event)[1]);
				st.calcReward(getId(), Integer.parseInt(event));
				htmltext = "32117-06.htm";
			}
			else
			{
				htmltext = "32117-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("32106-07.htm"))
		{
			st.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		final int id = st.getState();
		final int cond = st.getCond();
		final int npcId = npc.getId();
		final long count = st.getQuestItemsCount(8776);
		
		switch (st.getState())
		{
			case State.CREATED:
				if (npcId == 32106)
				{
					if ((id == State.CREATED) && (cond == 0))
					{
						if (player.getLevel() >= getMinLvl(getId()))
						{
							htmltext = "32106-01.htm";
						}
						else
						{
							htmltext = "32106-00.htm";
							st.exitQuest(true);
						}
					}
				}
				break;
			case State.STARTED:
				if (npcId == 32106)
				{
					if (cond == 1)
					{
						if (count == 0)
						{
							htmltext = "32106-05.htm";
						}
						else
						{
							htmltext = "32106-05a.htm";
							st.takeItems(8776, -1);
							st.calcRewardPerItem(getId(), 7, (int) count);
						}
					}
				}
				else if (npcId == 32117)
				{
					if (cond == 1)
					{
						htmltext = "32117-01.htm";
					}
					else
					{
						st.exitQuest(true);
					}
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player member = getRandomPartyMemberState(player, State.STARTED);
		if (member != null)
		{
			final QuestState st = member.getQuestState(getName());
			if (st != null && st.isCond(1))
			{
				st.calcDropItems(getId(), 8776, npc.getId(), Integer.MAX_VALUE);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _643_RiseandFalloftheElrokiTribe(643, _643_RiseandFalloftheElrokiTribe.class.getSimpleName(), "");
	}
}