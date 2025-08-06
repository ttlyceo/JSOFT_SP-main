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
 * Rework by LordWinter 23.05.2021
 */
public class _611_AllianceWithVarkaSilenos extends Quest
{
	private static class DropInfo
	{
		private final int _minCond;
		private final int _itemId;
		
		public DropInfo(int minCond)
		{
			_minCond = minCond;
			switch (_minCond)
			{
				case 1:
					_itemId = 7226;
					break;
				case 2:
					_itemId = 7227;
					break;
				default:
					_itemId = 7228;
					break;
			}
		}
		
		public int getMinCond()
		{
			return _minCond;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
	}
	
	private static final Map<Integer, DropInfo> MOBS = new HashMap<>();
	static
	{
		MOBS.put(21324, new DropInfo(1));
		MOBS.put(21325, new DropInfo(1));
		MOBS.put(21327, new DropInfo(1));
		MOBS.put(21328, new DropInfo(1));
		MOBS.put(21329, new DropInfo(1));
		MOBS.put(21331, new DropInfo(2));
		MOBS.put(21332, new DropInfo(2));
		MOBS.put(21334, new DropInfo(2));
		MOBS.put(21335, new DropInfo(2));
		MOBS.put(21336, new DropInfo(2));
		MOBS.put(21338, new DropInfo(2));
		MOBS.put(21339, new DropInfo(3));
		MOBS.put(21340, new DropInfo(3));
		MOBS.put(21342, new DropInfo(3));
		MOBS.put(21343, new DropInfo(2));
		MOBS.put(21344, new DropInfo(2));
		MOBS.put(21345, new DropInfo(3));
		MOBS.put(21346, new DropInfo(3));
		MOBS.put(21347, new DropInfo(3));
		MOBS.put(21348, new DropInfo(3));
		MOBS.put(21349, new DropInfo(3));
	}
	
	private static final int[] KETRA_MARKS =
	{
	        7211, 7212, 7213, 7214, 7215
	};
	
	private static final int[] VARKA_MARKS =
	{
	        7221, 7222, 7223, 7224, 7225
	};
	
	private static final int[] SOLDIER_BADGE_COUNT =
	{
	        100, 200, 300, 300, 400
	};
	
	private static final int[] OFFICER_BADGE_COUNT =
	{
	        0, 100, 200, 300, 400
	};
	
	private static final int[] CAPTAIN_BADGE_COUNT =
	{
	        0, 0, 100, 200, 200
	};
	
	private _611_AllianceWithVarkaSilenos(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31378);
		addTalkId(31378);
		addKillId(21324, 21325, 21327, 21328, 21329, 21331, 21332, 21334, 21335, 21336, 21338, 21339, 21340, 21342, 21343, 21344, 21345, 21346, 21347, 21348, 21349);
		
		questItemIds = new int[]
		{
		        7228, 7227, 7226
		};
	}
	
	private int getMaxItems(QuestState st, int itemId)
	{
		int count = 0;
		switch (itemId)
		{
			case 7226 :
				count = SOLDIER_BADGE_COUNT[st.getCond() - 1];
				break;
			case 7227 :
				count = OFFICER_BADGE_COUNT[st.getCond() - 1];
				break;
			case 7228 :
				count = CAPTAIN_BADGE_COUNT[st.getCond() - 1];
				break;
		}
		return count;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "31378-12a.html":
			case "31378-12b.html":
			case "31378-25.html":
				break;
			case "31378-04.htm":
				if (hasAtLeastOneQuestItem(player, KETRA_MARKS))
				{
					return "31378-03.htm";
				}
				st.setState(State.STARTED);
				st.playSound(QuestSound.ITEMSOUND_QUEST_ACCEPT);
				for (int i = 0; i < VARKA_MARKS.length; i++)
				{
					if (st.hasQuestItems(VARKA_MARKS[i]))
					{
						st.setCond(i + 2);
						return "31378-0" + (i + 5) + ".htm";
					}
				}
				st.setCond(1);
				break;
			case "31378-12.html":
				if (st.getQuestItemsCount(7226) < SOLDIER_BADGE_COUNT[0])
				{
					return getNoQuestMsg(player);
				}
				st.takeItems(7226, -1);
				st.giveItems(VARKA_MARKS[0], 1);
				st.setCond(2, true);
				break;
			case "31378-15.html":
				if ((st.getQuestItemsCount(7226) < SOLDIER_BADGE_COUNT[1]) || (st.getQuestItemsCount(7227) < OFFICER_BADGE_COUNT[1]))
				{
					return getNoQuestMsg(player);
				}
				takeItems(player, -1, 7226, 7227, VARKA_MARKS[0]);
				st.giveItems(VARKA_MARKS[1], 1);
				st.setCond(3, true);
				break;
			case "31378-18.html":
				if ((st.getQuestItemsCount(7226) < SOLDIER_BADGE_COUNT[2]) || (st.getQuestItemsCount(7227) < OFFICER_BADGE_COUNT[2]) || (st.getQuestItemsCount(7228) < CAPTAIN_BADGE_COUNT[2]))
				{
					return getNoQuestMsg(player);
				}
				takeItems(player, -1, 7226, 7227, 7228, VARKA_MARKS[1]);
				st.giveItems(VARKA_MARKS[2], 1);
				st.setCond(4, true);
				break;
			case "31378-21.html":
				if (!st.hasQuestItems(7229) || (st.getQuestItemsCount(7226) < SOLDIER_BADGE_COUNT[3]) || (st.getQuestItemsCount(7227) < OFFICER_BADGE_COUNT[3]) || (st.getQuestItemsCount(7228) < CAPTAIN_BADGE_COUNT[3]))
				{
					return getNoQuestMsg(player);
				}
				takeItems(player, -1, 7226, 7227, 7228, 7229, VARKA_MARKS[2]);
				st.giveItems(VARKA_MARKS[3], 1);
				st.setCond(5, true);
				break;
			case "31378-26.html":
				takeItems(player, -1, VARKA_MARKS);
				takeItems(player, -1, 7229, 7230);
				st.exitQuest(true, true);
				break;
			default:
				htmltext = null;
				break;
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
		
		switch (st.getState())
		{
			case State.CREATED:
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "31378-01.htm" : "31378-02.htm";
				break;
			case State.STARTED:
				switch (st.getCond())
				{
					case 1:
						htmltext = (st.getQuestItemsCount(7226) >= SOLDIER_BADGE_COUNT[0]) ? "31378-11.html" : "31378-10.html";
						break;
					case 2:
						htmltext = (st.hasQuestItems(VARKA_MARKS[0]) && (st.getQuestItemsCount(7226) >= SOLDIER_BADGE_COUNT[1]) && (st.getQuestItemsCount(7227) >= OFFICER_BADGE_COUNT[1])) ? "31378-14.html" : "31378-13.html";
						break;
					case 3:
						htmltext = (st.hasQuestItems(VARKA_MARKS[1]) && (st.getQuestItemsCount(7226) >= SOLDIER_BADGE_COUNT[2]) && (st.getQuestItemsCount(7227) >= OFFICER_BADGE_COUNT[2]) && (st.getQuestItemsCount(7228) >= CAPTAIN_BADGE_COUNT[2])) ? "31378-17.html" : "31378-16.html";
						break;
					case 4:
						htmltext = (hasQuestItems(player, VARKA_MARKS[2], 7229) && (st.getQuestItemsCount(7226) >= SOLDIER_BADGE_COUNT[3]) && (st.getQuestItemsCount(7227) >= OFFICER_BADGE_COUNT[3]) && (st.getQuestItemsCount(7228) >= CAPTAIN_BADGE_COUNT[3])) ? "31378-20.html" : "31378-19.html";
						break;
					case 5:
						if (!st.hasQuestItems(VARKA_MARKS[3]) || !st.hasQuestItems(7230) || (st.getQuestItemsCount(7226) < SOLDIER_BADGE_COUNT[4]) || (st.getQuestItemsCount(7227) < OFFICER_BADGE_COUNT[4]) || (st.getQuestItemsCount(7228) < CAPTAIN_BADGE_COUNT[4]))
						{
							return "31378-22.html";
						}
						st.setCond(6, true);
						takeItems(player, -1, 7226, 7227, 7228, 7230, VARKA_MARKS[3]);
						st.giveItems(VARKA_MARKS[4], 1);
						htmltext = "31378-23.html";
						break;
					case 6:
						if (st.hasQuestItems(VARKA_MARKS[4]))
						{
							htmltext = "31378-24.html";
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(killer, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			final DropInfo info = MOBS.get(npc.getId());
			if (st.getCond() >= info.getMinCond() && st.getCond() < 6)
			{
				final int max = getMaxItems(st, info.getItemId());
				st.calcDropItems(getId(), info.getItemId(), npc.getId(), max);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _611_AllianceWithVarkaSilenos(611, _611_AllianceWithVarkaSilenos.class.getSimpleName(), "");
	}
}