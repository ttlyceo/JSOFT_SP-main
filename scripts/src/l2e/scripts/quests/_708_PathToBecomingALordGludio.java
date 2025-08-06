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

import l2e.commons.util.Util;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter
 */
public class _708_PathToBecomingALordGludio extends Quest
{
	private static final String qn = "_708_PathToBecomingALordGludio";
	
	// NPCs
	private static final int SAYRES = 35100;
	private static final int PINTER = 30298;
	private static final int BATHIS = 30332;
	
	// Items
	private static final int HEADLESS_ARMOR = 13848;
	private static final int VARNISH = 1865;
	private static final int ANIMAL_SKIN = 1867;
	private static final int IRON_ORE = 1869;
	private static final int COKES = 1879;
	
	// Monsters
	private static final int HEADLESS_KNIGHT = 27393;
	
	private static final int[] MOBS =
	{
	        20045, 20051, 20099, HEADLESS_KNIGHT
	};
	
	private static final int GLUDIO_CASTLE = 1;
	
	public _708_PathToBecomingALordGludio(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(SAYRES);
		addTalkId(SAYRES, PINTER, BATHIS);
		addKillId(MOBS);
		
		questItemIds = new int[]
		{
			HEADLESS_ARMOR
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(GLUDIO_CASTLE);
		
		switch (event)
		{
			case "35100-02.htm":
			case "30298-04.htm" :
			{
				htmltext = event;
				break;
			}
			case "35100-04.htm" :
			{
				if (st.isCreated())
				{
					st.startQuest();
					htmltext = event;
				}
				break;
			}
			case "35100-08.htm" :
			{
				if (st.isCond(1))
				{
					st.setCond(2);
					htmltext = event;
				}
				break;
			}
			case "35100-12.htm" :
			{
				final QuestState qs0 = getQuestState(player.getClan().getLeader().getPlayerInstance(), false);
				if (qs0.isCond(2))
				{
					qs0.set("clanmember", player.getObjectId());
					qs0.setCond(3);
					htmltext = event.replace("%name%", player.getName(null));
				}
				break;
			}
			case "30298-05.htm" :
			{
				final QuestState qs0 = getQuestState(player.getClan().getLeader().getPlayerInstance(), false);
				if (qs0.isCond(3))
				{
					qs0.setCond(4);
					htmltext = event;
				}
				break;
			}
			case "30298-09.htm" :
			{
				final QuestState qs0 = getQuestState(player.getClan().getLeader().getPlayerInstance(), false);
				if (qs0.isCond(4) && (getQuestItemsCount(player, VARNISH) >= 100) && (getQuestItemsCount(player, ANIMAL_SKIN) >= 100) && (getQuestItemsCount(player, IRON_ORE) >= 100) && (getQuestItemsCount(player, COKES) >= 50))
				{
					qs0.setCond(5);
					htmltext = event;
				}
				break;
			}
			case "30332-02.htm" :
			{
				if (st.isCond(5))
				{
					st.setCond(6);
					htmltext = event;
				}
				break;
			}
			case "30332-05.htm" :
			{
				if (st.isCond(7) && (getQuestItemsCount(player, HEADLESS_ARMOR) >= 1))
				{
					takeItems(player, HEADLESS_ARMOR, -1);
					npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.LISTEN_YOU_VILLAGERS_OUR_LIEGE_WHO_WILL_SOON_BECOME_A_LORD_HAS_DEFEATED_THE_HEADLESS_KNIGHT_YOU_CAN_NOW_REST_EASY));
					st.setCond(9);
					htmltext = event;
				}
				break;
			}
			case "35100-23.htm" :
			{
				if (st.isCond(9))
				{
					if (castle.getSiege().getIsInProgress())
					{
						return "35100-22a.htm";
					}
					
					for (final Fort fort : FortManager.getInstance().getForts())
					{
						if (!fort.isBorderFortress() && fort.getSiege().getIsInProgress())
						{
							return "35100-22a.htm";
						}
						else if (!fort.isBorderFortress() && (fort.getContractedCastleId() != GLUDIO_CASTLE))
						{
							return "35100-22b.htm";
						}
					}
					final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_LORD_OF_THE_TOWN_OF_GLUDIO_LONG_MAY_HE_REIGN);
					packet.addStringParameter(player.getName(null));
					npc.broadcastPacketToOthers(2000, packet);
					castle.getTerritory().changeOwner(castle.getOwner());
					st.exitQuest(true, true);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState st = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(GLUDIO_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		switch (npc.getId())
		{
			case SAYRES:
			{
				switch (st.getState())
				{
					case State.CREATED:
					{
						final Player leader = talker.getClan().getLeader().getPlayerInstance();
						if (leader == null)
						{
							htmltext = "35100-09.htm";
						}
						else if (leader != null && talker != leader)
						{
							final QuestState qs0 = getQuestState(leader, false);
							if (qs0 != null)
							{
								if (qs0.isCond(2))
								{
									if (Util.checkIfInRange(1500, talker, leader, true) && leader.isOnline())
									{
										htmltext = "35100-11.htm";
									}
									else
									{
										htmltext = "35100-10.htm";
									}
								}
								else if (qs0.isCond(3))
								{
									htmltext = "35100-13a.htm";
								}
								else
								{
									htmltext = "35100-09.htm";
								}
							}
							else
							{
								htmltext = "35100-09.htm";
							}
						}
						else
						{
							if (castle.getTerritory().getLordObjectId() != talker.getObjectId())
							{
								htmltext = "35100-01.htm";
							}
						}
						break;
					}
					case State.STARTED:
					{
						switch (st.getCond())
						{
							case 1:
							{
								htmltext = "35100-06.htm";
								break;
							}
							case 2:
							{
								htmltext = "35100-14.htm";
								break;
							}
							case 3:
							{
								htmltext = "35100-15.htm";
								break;
							}
							case 4:
							{
								htmltext = "35100-16.htm";
								break;
							}
							case 5:
							{
								htmltext = "35100-18.htm";
								break;
							}
							case 6:
							{
								htmltext = "35100-19.htm";
								break;
							}
							case 7:
							{
								htmltext = "35100-20.htm";
								break;
							}
							case 8:
							{
								htmltext = "35100-21.htm";
								break;
							}
							case 9:
							{
								htmltext = "35100-22.htm";
								break;
							}
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(talker);
						break;
					}
				}
				break;
			}
			case PINTER:
			{
				final Player leader = talker.getClan().getLeader().getPlayerInstance();
				
				if (talker != leader)
				{
					final QuestState qs0 = getQuestState(leader, false);
					if (leader.isOnline())
					{
						if (talker.getObjectId() == qs0.getInt("clanmember"))
						{
							switch (qs0.getCond())
							{
								case 3:
								{
									htmltext = "30298-03.htm";
									break;
								}
								case 4:
								{
									if ((getQuestItemsCount(talker, VARNISH) >= 100) && (getQuestItemsCount(talker, ANIMAL_SKIN) >= 100) && (getQuestItemsCount(talker, IRON_ORE) >= 100) && (getQuestItemsCount(talker, COKES) >= 50))
									{
										htmltext = "30298-08.htm";
									}
									else
									{
										htmltext = "30298-07.htm";
									}
									break;
								}
								case 5:
								{
									htmltext = "30298-12.htm";
									break;
								}
							}
							
						}
						else
						{
							htmltext = "30298-03a.htm";
						}
					}
					else
					{
						htmltext = "30298-01.htm";
					}
				}
				break;
			}
			case BATHIS:
			{
				switch (st.getCond())
				{
					case 5:
					{
						htmltext = "30332-01.htm";
						break;
					}
					case 6:
					{
						htmltext = "30332-03.htm";
						break;
					}
					case 7:
					{
						if (getQuestItemsCount(talker, HEADLESS_ARMOR) >= 1)
						{
							htmltext = "30332-04.htm";
						}
						break;
					}
					case 9:
					{
						htmltext = "30332-06.htm";
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState st = killer.getQuestState(getName());
		
		if ((st != null) && st.isCond(6))
		{
			if (npc.getId() == HEADLESS_KNIGHT)
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.DOES_MY_MISSION_TO_BLOCK_THE_SUPPLIES_END_HERE));
				giveItems(killer, HEADLESS_ARMOR, 1);
				st.setCond(7);
			}
			else if (getRandom(100) < 10)
			{
				addSpawn(HEADLESS_KNIGHT, npc.getLocation(), false, 100000);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _708_PathToBecomingALordGludio(708, qn, "");
	}
}
