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
import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 13.06.2020
 */
public class _610_MagicalPowerOfWaterPart2 extends Quest
{
	private _610_MagicalPowerOfWaterPart2(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31372);
		addTalkId(31372, 31560);
		
		addKillId(25316);
		
		questItemIds = new int[]
		{
		        7238, 7239
		};
		
		final String info = loadGlobalQuestVar("_610_MagicalPowerOfWaterPart2_respawn");
		final long remain = (!info.isEmpty()) ? (Long.parseLong(info) - System.currentTimeMillis()) : 0;
		if (remain > 0)
		{
			startQuestTimer("spawn_altar", remain, null, null);
		}
		else
		{
			addSpawn(31560, 105452, -36775, -1050, 34000, false, 0, true);
		}
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if ((st != null) && Util.checkIfInRange(1500, npc, player, false))
		{
			if (npc.getId() == 25316)
			{
				switch (st.getCond())
				{
					case 1 :
						st.takeItems(7238, 1);
						break;
					case 2 :
						if (!st.hasQuestItems(7239))
						{
							st.giveItems(7239, 1);
						}
						st.setCond(3, true);
						break;
				}
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (player != null)
		{
			final QuestState st = player.getQuestState(getName());
			if (st == null)
			{
				return null;
			}
			
			switch (event)
			{
				case "31372-02.htm" :
					st.startQuest();
					htmltext = event;
					break;
				case "give_heart" :
					if (st.hasQuestItems(7239) && st.isCond(3))
					{
						st.calcExpAndSp(getId());
						st.calcReward(getId(), Rnd.get(1, 6));
						st.exitQuest(true, true);
						htmltext = "31372-06.htm";
					}
					else
					{
						htmltext = "31372-07.htm";
					}
					break;
				case "spawn_totem" :
					htmltext = (st.hasQuestItems(7238)) ? spawnAshutar(npc, st) : "31560-04.htm";
					break;
			}
		}
		else
		{
			if (event.equals("despawn_ashutar"))
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.THE_POWER_OF_CONSTRAINT_IS_GETTING_WEAKER_YOUR_RITUAL_HAS_FAILED));
				npc.deleteMe();
				addSpawn(31560, 105452, -36775, -1050, 34000, false, 0, true);
			}
			else if (event.equals("spawn_altar"))
			{
				addSpawn(31560, 105452, -36775, -1050, 34000, false, 0, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == 25316)
		{
			final long respawnDelay = 3 * 60 * 60 * 1000;
			cancelQuestTimer("despawn_ashutar", npc, null);
			saveGlobalQuestVar("_610_MagicalPowerOfWaterPart2_respawn", String.valueOf(System.currentTimeMillis() + respawnDelay));
			startQuestTimer("spawn_altar", respawnDelay, null, null);
			executeForEachPlayer(killer, npc, isSummon, true, false);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (npc.getId())
		{
			case 31372 :
				switch (st.getState())
				{
					case State.CREATED :
						htmltext = (player.getLevel() >= getMinLvl(getId())) ? (st.hasQuestItems(7238)) ? "31372-01.htm" : "31372-00.htm" : "31372-00a.htm";
						break;
					case State.STARTED :
						htmltext = (st.isCond(1)) ? "31372-03.htm" : (st.hasQuestItems(7239)) ? "31372-04.htm" : "31372-05.htm";
						break;
				}
				break;
			case 31560 :
				if (st.isStarted())
				{
					switch (st.getCond())
					{
						case 1 :
							htmltext = "31560-01.htm";
							break;
						case 2 :
							htmltext = spawnAshutar(npc, st);
							break;
						case 3 :
							htmltext = "31560-05.htm";
							break;
					}
				}
				break;
		}
		return htmltext;
	}
	
	private String spawnAshutar(Npc npc, QuestState st)
	{
		if (getQuestTimer("spawn_altar", null, null) != null)
		{
			return "31560-03.htm";
		}
		if (st.isCond(1))
		{
			st.takeItems(7238, 1);
			st.setCond(2, true);
		}
		npc.deleteMe();
		final Npc ashutar = addSpawn(25316, 104825, -36926, -1136, 0, false, 0);
		if (ashutar != null)
		{
			ashutar.broadcastPacketToOthers(2000, new NpcSay(ashutar, Say2.NPC_ALL, NpcStringId.THE_MAGICAL_POWER_OF_WATER_COMES_FROM_THE_POWER_OF_STORM_AND_HAIL_IF_YOU_DARE_TO_CONFRONT_IT_ONLY_DEATH_WILL_AWAIT_YOU));
			startQuestTimer("despawn_ashutar", 1200000, ashutar, null);
		}
		return "31560-02.htm";
	}
	
	public static void main(String[] args)
	{
		new _610_MagicalPowerOfWaterPart2(610, _610_MagicalPowerOfWaterPart2.class.getSimpleName(), "");
	}
}
