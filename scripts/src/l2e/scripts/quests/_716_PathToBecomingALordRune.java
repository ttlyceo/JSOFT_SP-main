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
import l2e.gameserver.model.Clan;
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
public final class _716_PathToBecomingALordRune extends Quest
{
	private static final String qn = "_716_PathToBecomingALordRune";
	
	// NPCs
	private static final int FREDERICK = 35509;
	private static final int AGRIPEL = 31348;
	private static final int INNOCENTIN = 31328;
	
	// Monsters
	private static final int[] PAGANS =
	{
		22136, // Doorman Zombie
		22137, // Penance Guard
		22138, // Chapel Guard
		22139, // Old Aristocrat's Soldier
		22140, // Zombie Worker
		22141, // Forgotten Victim
		22142, // Triol's Layperson
		22143, // Triol's Believer
		22144, // Resurrected Temple Knight
		22145, // Ritual Sacrifice
		22146, // Triol's Priest
		22147, // Ritual Offering
		22148, // Triol's Believer
		22149, // Ritual Offering
		22150, // Triol's Believer
		22151, // Triol's Priest
		22152, // Temple Guard
		22153, // Temple Guard Captain
		22154, // Ritual Sacrifice
		22155, // Triol's High Priest
		22156, // Triol's Priest
		22157, // Triol's Priest
		22158, // Triol's Believer
		22159, // Triol's High Priest
		22160, // Triol's Priest
		22161, // Ritual Sacrifice
		22163, // Triol's High Priest
		22164, // Triol's Believer
		22165, // Triol's Priest
		22166, // Triol's Believer
		22167, // Triol's High Priest
		22168, // Triol's Priest
		22169, // Ritual Sacrifice
		22170, // Triol's Believer
		22171, // Triol's High Priest
		22172, // Ritual Sacrifice
		22173, // Triol's Priest
		22174, // Triol's Priest
		22175, // Andreas' Captain of the Royal Guard
		22176, // Andreas' Royal Guards
		22194, // Penance Guard
	};
	
	private static final int CASTLE_ID = 8;
	
	public _716_PathToBecomingALordRune(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(FREDERICK);
		addTalkId(FREDERICK, AGRIPEL, INNOCENTIN);
		
		addKillId(PAGANS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, false);
		String htmltext = null;
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "35509-02.htm" :
			case "31348-02.htm" :
			case "31328-04.htm" :
			case "31348-06.htm" :
			case "31348-07.htm" :
			case "31348-08.htm" :
			{
				htmltext = event;
				break;
			}
			case "35509-04.htm" :
			{
				if (st.isCreated())
				{
					st.startQuest();
					htmltext = event;
				}
				break;
			}
			case "31348-03.htm" :
			{
				if (st.isCond(2))
				{
					st.setCond(3);
					htmltext = event;
				}
				break;
			}
			case "35509-16.htm" :
			{
				final QuestState qs0 = getQuestState(player.getClan().getLeader().getPlayerInstance(), false);
				if (qs0.isCond(4))
				{
					qs0.set("clanmember", player.getObjectId());
					qs0.setCond(5);
					htmltext = event;
				}
				break;
			}
			case "31328-05.htm" :
			{
				final QuestState qs0 = getQuestState(player.getClan().getLeader().getPlayerInstance(), false);
				if (qs0.isCond(5))
				{
					qs0.setMemoState(0);
					qs0.setCond(6);
					htmltext = event;
				}
				break;
			}
			case "31348-10.htm" :
			{
				if (st.isCond(7))
				{
					st.setCond(8);
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
		final Castle castle = CastleManager.getInstance().getCastleById(CASTLE_ID);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		switch (npc.getId())
		{
			case FREDERICK:
			{
				switch (st.getState())
				{
					case State.CREATED:
					{
						final Player leader = talker.getClan().getLeader().getPlayerInstance();
						if (talker != leader)
						{
							if (leader != null)
							{
								final QuestState qs0 = getQuestState(leader, false);
								if (qs0 != null && qs0.isCond(4))
								{
									if (Util.checkIfInRange(1500, talker, leader, true) && leader.isOnline())
									{
										htmltext = "35509-15.htm";
									}
									else
									{
										htmltext = "35509-14.htm";
									}
								}
								else if (qs0 != null && qs0.isCond(5))
								{
									htmltext = "35509-17.htm";
								}
								else
								{
									htmltext = "35509-13.htm";
								}
							}
							else
							{
								htmltext = "35509-17.htm";
							}
						}
						else
						{
							if (castle.getTerritory().getLordObjectId() != talker.getObjectId())
							{
								htmltext = "35509-01.htm";
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
								QuestState qs1 = getQuestState(talker, false);
								qs1 = talker.getQuestState(_021_HiddenTruth.class.getSimpleName());
								QuestState qs2 = getQuestState(talker, false);
								qs2 = talker.getQuestState("_025_HidingBehindTheTruth");
								
								if ((qs1 != null) && qs1.isCompleted() && (qs2 != null) && qs2.isCompleted())
								{
									st.setCond(2);
									htmltext = "35509-05.htm";
								}
								else
								{
									htmltext = "35509-06.htm";
								}
								break;
							}
							case 2:
							{
								htmltext = "35509-07.htm";
								break;
							}
							case 3:
							{
								st.setCond(4);
								htmltext = "35509-09.htm";
								break;
							}
							case 4:
							{
								htmltext = "35509-10.htm";
								break;
							}
							case 5:
							{
								htmltext = "35509-18.htm";
								break;
							}
							case 6:
							{
								st.setCond(7);
								htmltext = "35509-19.htm";
								break;
							}
							case 7:
							{
								htmltext = "35509-20.htm";
								break;
							}
							case 8:
							{
								if (castle.getSiege().getIsInProgress())
								{
									return "35509-21a.htm";
								}
								
								for (final Fort fort : FortManager.getInstance().getForts())
								{
									if (!fort.isBorderFortress() && fort.getSiege().getIsInProgress())
									{
										return "35509-21a.htm";
									}
									else if (!fort.isBorderFortress() && (fort.getContractedCastleId() != CASTLE_ID))
									{
										return "35509-21b.htm";
									}
								}
								
								final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_RUNE_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_RUNE);
								packet.addStringParameter(talker.getName(null));
								npc.broadcastPacketToOthers(2000, packet);
								castle.getTerritory().changeOwner(castle.getOwner());
								st.exitQuest(true, true);
								htmltext = "35509-21.htm";
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
			case AGRIPEL:
			{
				switch (st.getCond())
				{
					case 2:
					{
						htmltext = "31348-01.htm";
						break;
					}
					case 3:
					case 4:
					case 5:
					case 6:
					{
						htmltext = "31348-04.htm";
						break;
					}
					case 7:
					{
						if (st.getMemoState() >= 100)
						{
							htmltext = "31348-09.htm";
						}
						else
						{
							htmltext = "31348-05.htm";
						}
						break;
					}
					case 8:
					{
						htmltext = "31348-11.htm";
						break;
					}
				}
				break;
			}
			case INNOCENTIN:
			{
				final Player leader = talker.getClan().getLeader().getPlayerInstance();
				
				if (talker != leader)
				{
					final QuestState qs0 = getQuestState(leader, false);
					if (st.getMemoState() >= 100)
					{
						htmltext = "31328-06.htm";
					}
					else
					{
						if (leader.isOnline())
						{
							if (talker.getObjectId() == qs0.getInt("clanmember"))
							{
								htmltext = "31328-03.htm";
							}
							else
							{
								htmltext = "31328-03a.htm";
							}
						}
						else
						{
							htmltext = "31328-01.htm";
						}
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Clan killerClan = killer.getClan();
		if (killerClan == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(CASTLE_ID);
		final Clan castleOwner = castle.getOwner();
		if (castleOwner == null || killerClan != castleOwner)
		{
			return super.onKill(npc, killer, isSummon);
		}

		final Player leader = castleOwner.getLeader().getPlayerInstance();
		if (leader != null)
		{
			final QuestState qs = getQuestState(leader, false);
			if ((qs != null) && qs.isCond(7) && leader.isOnline() && (killer != leader) && (killer.getObjectId() == qs.getInt("clanmember")))
			{
				if (qs.getMemoState() < 100)
				{
					qs.setMemoState(qs.getMemoState() + 1);
				}
				else
				{
					playSound(leader, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _716_PathToBecomingALordRune(716, qn, "");
	}
}