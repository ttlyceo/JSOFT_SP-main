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


import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.serverpackets.RadarControl;

/**
 * Updated by LordWinter
 */
public class _307_ControlDeviceOfTheGiants extends Quest
{
	private static final String qn = "_307_ControlDeviceOfTheGiants";
	
	private static int DROPH = 32711;
	private static int HEKATON_PRIME = 25687;
	
	private static final int DROPHS_ITEMS = 14850;
	private static final int CAVETEXT1SHEET = 14851;
	private static final int CAVETEXT2SHEET = 14852;
	private static final int CAVETEXT3SHEET = 14853;
	
	private static final long HEKATON_PRIME_RESPAWN = 12 * 3600 * 1000L;
	
	private static final Location GORGOLOS_LOC = new Location(186096, 61501, -4075, 0);
	private static final Location LAST_TITAN_UTENUS_LOC = new Location(186730, 56456, -4555, 0);
	private static final Location GIANT_MARPANAK_LOC = new Location(194057, 53722, -4259, 0);
	private static final Location HEKATON_PRIME_LOC = new Location(192328, 56120, -7651, 0);
	
	
	public _307_ControlDeviceOfTheGiants(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(DROPH);
		addTalkId(DROPH);
		
		addKillId(HEKATON_PRIME);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(qn);
		String htmltext = event;
		if (event.equalsIgnoreCase("32711-02.htm"))
		{
			if (st.isCreated())
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
		}
		else if (event.equalsIgnoreCase("loc1"))
		{
			htmltext = "32711-02a_1.htm";
			final RadarControl rc = new RadarControl(0, 1, GORGOLOS_LOC.getX(), GORGOLOS_LOC.getY(), GORGOLOS_LOC.getZ());
			player.sendPacket(rc);
		}
		else if (event.equalsIgnoreCase("loc2"))
		{
			htmltext = "32711-02a_2.htm";
			final RadarControl rc = new RadarControl(0, 1, LAST_TITAN_UTENUS_LOC.getX(), LAST_TITAN_UTENUS_LOC.getY(), LAST_TITAN_UTENUS_LOC.getZ());
			player.sendPacket(rc);
		}
		else if (event.equalsIgnoreCase("loc3"))
		{
			htmltext = "32711-02a_3.htm";
			final RadarControl rc = new RadarControl(0, 1, GIANT_MARPANAK_LOC.getX(), GIANT_MARPANAK_LOC.getY(), GIANT_MARPANAK_LOC.getZ());
			player.sendPacket(rc);
		}
		else if (event.equalsIgnoreCase("summon_rb"))
		{
			if (ServerVariables.getLong("HekatonPrimeRespawn", 0) < System.currentTimeMillis() && st.getQuestItemsCount(CAVETEXT1SHEET) >= 1 && st.getQuestItemsCount(CAVETEXT2SHEET) >= 1 && st.getQuestItemsCount(CAVETEXT3SHEET) >= 1)
			{
				st.takeItems(CAVETEXT1SHEET, 1);
				st.takeItems(CAVETEXT2SHEET, 1);
				st.takeItems(CAVETEXT3SHEET, 1);
				ServerVariables.set("HekatonPrimeRespawn", System.currentTimeMillis() + HEKATON_PRIME_RESPAWN);
				st.addSpawn(HEKATON_PRIME, HEKATON_PRIME_LOC.getX(), HEKATON_PRIME_LOC.getY(), HEKATON_PRIME_LOC.getZ(), 0);
				htmltext = "32711-03a.htm";
			}
			else
			{
				htmltext = "32711-02b.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		final int id = st.getState();
		final int cond = st.getInt("cond");
		final int npcId = npc.getId();
		if (npcId == DROPH)
		{
			if ((id == State.CREATED) && (cond == 0))
			{
				if (player.getLevel() >= 79)
				{
					htmltext = "32711-01.htm";
				}
				else
				{
					st.exitQuest(true);
					htmltext = "32711-00.htm";
				}
			}
			else if (id == State.STARTED)
			{
				if (npcId == DROPH)
				{
					if (cond == 1)
					{
						if (st.getQuestItemsCount(CAVETEXT1SHEET) >= 1 && st.getQuestItemsCount(CAVETEXT2SHEET) >= 1 && st.getQuestItemsCount(CAVETEXT3SHEET) >= 1)
						{
							if(ServerVariables.getLong("HekatonPrimeRespawn", 0) < System.currentTimeMillis())
							{
								htmltext = "32711-03.htm";
							}
							else
							{
								htmltext = "32711-04.htm";
							}
						}
						else
						{
							htmltext = "32711-02a.htm";
						}
					}
					else if (cond == 2)
					{
						htmltext = "32711-05.htm";
						st.giveItems(DROPHS_ITEMS, 1);
						st.playSound("ItemSound.quest_finish");
						st.exitQuest(true);
					}
				}
			}
			
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (npc.getId() == HEKATON_PRIME)
			{
				st.set("cond", "2");
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _307_ControlDeviceOfTheGiants(307, qn, "");
	}
}