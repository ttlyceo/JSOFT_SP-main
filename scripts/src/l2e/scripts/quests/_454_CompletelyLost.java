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
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState.QuestType;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 06.02.2024
 */
public final class _454_CompletelyLost extends Quest
{
	private Npc _erman;
	private final int _interval;
	
	public _454_CompletelyLost(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32738);
		addTalkId(32738, 32736);
		addEventReceivedId(32738);
		
		for (final var spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn != null)
			{
				if (spawn.getId() == 32736)
				{
					_erman = spawn.getLastSpawn();
				}
			}
		}
		_interval = (getQuestParams(questId).getInteger("timeLimit") / 4) * 60;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "QUEST_TIMER":
			{
				final double dist = npc.getDistance(_erman);
				if (dist <= 100 && npc.getVariables().getInteger("state", 0) == 2)
				{
					npc.getVariables().set("state", 3);
					npc.getAI().setIntention(CtrlIntention.IDLE);
					npc.setHeading(Util.calculateHeadingFrom(npc, _erman));
					startQuestTimer("SAY_TIMER2", 2000, npc, null);
				}
				else
				{
					startQuestTimer("QUEST_TIMER", 2000, npc, null);
				}
				break;
			}
			case "SAY_TIMER1":
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.GASP));
				break;
			}
			case "SAY_TIMER2":
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.SOB_TO_SEE_ERMIAN_AGAIN_CAN_I_GO_TO_MY_FAMILY_NOW));
				npc.getAI().setIntention(CtrlIntention.MOVING, new Location(-180219, 186341, -10600));
				npc.sendScriptEvent(this, "SCE_A_SEED_ESCORT_QUEST_SUCCESS", npc, null);
				startQuestTimer("EXPIRED_TIMER", 2000, npc, null);
				break;
			}
			case "CHECK_TIMER":
			{
				final var leader = npc.getVariables().getObject("leader", Player.class);
				if (leader != null)
				{
					final double dist = npc.getDistance(leader);
					if (dist > 1000)
					{
						if (((dist > 5000) && (dist < 6900)) || ((dist > 31000) && (dist < 32000)))
						{
							npc.teleToLocation(leader, true, leader.getReflection());
							npc.getAI().setIntention(CtrlIntention.FOLLOW, leader);
						}
						else if (npc.getVariables().getInteger("whisper", 0) == 0)
						{
							if (player != null)
							{
								player.sendPacket(new NpcSay(npc.getObjectId(), Say2.TELL, npc.getId(), NpcStringId.WHERE_ARE_YOU_I_CANT_SEE_ANYTHING));
							}
							npc.getVariables().set("whisper", 1);
						}
						else if (npc.getVariables().getInteger("whisper", 0) == 1)
						{
							if (player != null)
							{
								player.sendPacket(new NpcSay(npc.getObjectId(), Say2.TELL, npc.getId(), NpcStringId.WHERE_ARE_YOU_REALLY_I_CANT_FOLLOW_YOU_LIKE_THIS));
							}
							npc.getVariables().set("whisper", 2);
						}
						else if (npc.getVariables().getInteger("whisper", 0) == 2)
						{
							if (player != null)
							{
								player.sendPacket(new NpcSay(npc.getObjectId(), Say2.TELL, npc.getId(), NpcStringId.IM_SORRY_THIS_IS_IT_FOR_ME));
							}
							npc.sendScriptEvent(this, "SCE_A_SEED_ESCORT_QUEST_FAILURE", npc, null);
						}
					}
				}
				startQuestTimer("CHECK_TIMER", 2000, npc, null);
				break;
			}
			case "TIME_LIMIT1":
			{
				final var leader = npc.getVariables().getObject("leader", Player.class);
				if (leader != null && leader.isOnline())
				{
					leader.sendPacket(new NpcSay(npc.getObjectId(), Say2.TELL, npc.getId(), NpcStringId.IS_IT_STILL_LONG_OFF));
				}
				startQuestTimer("TIME_LIMIT2", (_interval * 1000), npc, null);
				break;
			}
			case "TIME_LIMIT2":
			{
				final var leader = npc.getVariables().getObject("leader", Player.class);
				if (leader != null && leader.isOnline())
				{
					leader.sendPacket(new NpcSay(npc.getObjectId(), Say2.TELL, npc.getId(), NpcStringId.IS_ERMIAN_WELL_EVEN_I_CANT_BELIEVE_THAT_I_SURVIVED_IN_A_PLACE_LIKE_THIS));
				}
				startQuestTimer("TIME_LIMIT3", (_interval * 1000), npc, null);
				break;
			}
			case "TIME_LIMIT3":
			{
				final var leader = npc.getVariables().getObject("leader", Player.class);
				if (leader != null && leader.isOnline())
				{
					leader.sendPacket(new NpcSay(npc.getObjectId(), Say2.TELL, npc.getId(), NpcStringId.I_DONT_KNOW_HOW_LONG_ITS_BEEN_SINCE_I_PARTED_COMPANY_WITH_YOU_TIME_DOESNT_SEEM_TO_MOVE_IT_JUST_FEELS_TOO_LONG));
				}
				startQuestTimer("TIME_LIMIT4", (_interval * 1000), npc, null);
				break;
			}
			case "TIME_LIMIT4":
			{
				final var leader = npc.getVariables().getObject("leader", Player.class);
				if (leader != null && leader.isOnline())
				{
					leader.sendPacket(new NpcSay(npc.getObjectId(), Say2.TELL, npc.getId(), NpcStringId.SORRY_TO_SAY_THIS_BUT_THE_PLACE_YOU_STRUCK_ME_BEFORE_NOW_HURTS_GREATLY));
				}
				startQuestTimer("TIME_LIMIT5", (_interval * 1000), npc, null);
				break;
			}
			case "TIME_LIMIT5":
			{
				final var leader = npc.getVariables().getObject("leader", Player.class);
				if (leader != null && leader.isOnline())
				{
					leader.sendPacket(new NpcSay(npc.getObjectId(), Say2.TELL, npc.getId(), NpcStringId.UGH_IM_SORRY_IT_LOOKS_LIKE_THIS_IS_IT_FOR_ME_I_WANTED_TO_LIVE_AND_SEE_MY_FAMILY));
				}
				npc.sendScriptEvent(this, "SCE_A_SEED_ESCORT_QUEST_FAILURE", npc, null);
				startQuestTimer("EXPIRED_TIMER", 2000, npc, null);
				break;
			}
			case "EXPIRED_TIMER":
			{
				npc.deleteMe();
				break;
			}
		}
		
		if (player == null)
		{
			return null;
		}
		
		final var qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "32738-04.htm":
			{
				if (qs.isCreated() && qs.isNowAvailable() && (player.getLevel() >= getMinLvl(getId())))
				{
					if (npc.getVariables().getInteger("quest_escort", 0) == 0)
					{
						npc.getVariables().set("leader", player);
						npc.getVariables().set("quest_escort", 1);
						if (player.isInParty())
						{
							npc.getVariables().set("partyId", player.getParty().getLeaderObjectId());
						}
						qs.setMemoState(1);
						qs.startQuest();
						htmltext = event;
					}
					else
					{
						final var leader = npc.getVariables().getObject("leader", Player.class);
						if (leader.isInParty() && leader.getParty().containsPlayer(player))
						{
							qs.startQuest();
							qs.setMemoState(1);
							final var html = new NpcHtmlMessage(npc.getObjectId());
							html.setFile(player, player.getLang(), "data/html/scripts/quests/" + getName() + "/32738-04a.htm");
							html.replace("%leader%", leader.getName(null));
							player.sendPacket(html);
							return null;
						}
						else
						{
							final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
							html.setFile(player, player.getLang(), "data/html/scripts/quests/" + getName() + "/32738-01b.htm");
							html.replace("%leader%", leader.getName(null));
							player.sendPacket(html);
							return null;
						}
					}
				}
				break;
			}
			case "agree1":
			{
				if (qs.isMemoState(1))
				{
					final var leader = npc.getVariables().getObject("leader", Player.class);
					if (leader != null)
					{
						if (leader.isInParty())
						{
							qs.setMemoState(2);
							npc.sendScriptEvent(this, "SCE_A_SEED_ESCORT_QUEST_START", npc, null);
							htmltext = "32738-06.htm";
						}
						else
						{
							htmltext = "32738-05a.htm";
						}
					}
				}
				break;
			}
			case "agree2":
			{
				if (qs.isMemoState(1))
				{
					qs.setMemoState(2);
					htmltext = "32738-06.htm";
					npc.sendScriptEvent(this, "SCE_A_SEED_ESCORT_QUEST_START", npc, null);
					final var leader = npc.getVariables().getObject("leader", Player.class);
					if (leader != null)
					{
						if (leader.isInParty())
						{
							for (final var member : leader.getParty().getMembers())
							{
								if (member != null)
								{
									final var qsMember = getQuestState(member, false);
									if ((qsMember != null) && qsMember.isMemoState(1) && (npc.getVariables().getInteger("partyId", 0) == leader.getParty().getLeaderObjectId()))
									{
										qsMember.setMemoState(2);
									}
								}
							}
						}
					}
				}
				break;
			}
			case "32738-07.htm" :
			{
				if (qs.isMemoState(1))
				{
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, GameObject reference)
	{
		switch (eventName)
		{
			case "SCE_A_SEED_ESCORT_QUEST_START":
			{
				final var leader = receiver.getVariables().getObject("leader", Player.class);
				if (leader != null)
				{
					receiver.setRunning();
					receiver.getAI().setIntention(CtrlIntention.FOLLOW, leader);
					startQuestTimer("QUEST_TIMER", 2000, receiver, null);
				}
				startQuestTimer("CHECK_TIMER", 1000, receiver, null);
				startQuestTimer("TIME_LIMIT1", 60000, receiver, null);
				receiver.getVariables().set("state", 2);
				receiver.getVariables().set("quest_escort", 99);
				break;
			}
			case "SCE_A_SEED_ESCORT_QUEST_SUCCESS":
			{
				final var leader = receiver.getVariables().getObject("leader", Player.class);
				if (leader != null)
				{
					if (leader.isInParty())
					{
						for (final var member : leader.getParty().getMembers())
						{
							if (member != null)
							{
								final var qs = getQuestState(member, false);
								if ((qs != null) && qs.isMemoState(2))
								{
									qs.setMemoState(4);
								}
							}
						}
					}
					else
					{
						final var qs = getQuestState(leader, false);
						if ((qs != null) && qs.isMemoState(2))
						{
							qs.setMemoState(4);
						}
					}
				}
				cancelQuestTimer("QUEST_TIMER", receiver, null);
				cancelQuestTimer("CHECK_TIMER", receiver, null);
				cancelQuestTimer("TIME_LIMIT1", receiver, null);
				cancelQuestTimer("TIME_LIMIT2", receiver, null);
				cancelQuestTimer("TIME_LIMIT3", receiver, null);
				cancelQuestTimer("TIME_LIMIT4", receiver, null);
				cancelQuestTimer("TIME_LIMIT5", receiver, null);
				break;
			}
			case "SCE_A_SEED_ESCORT_QUEST_FAILURE":
			{
				final Player leader = receiver.getVariables().getObject("leader", Player.class);
				if (leader != null)
				{
					if (leader.isInParty())
					{
						for (final var member : leader.getParty().getMembers())
						{
							if (member != null)
							{
								final var qs = getQuestState(member, false);
								if ((qs != null) && qs.isMemoState(2))
								{
									qs.setMemoState(3);
								}
							}
						}
					}
					else
					{
						final var qs = getQuestState(leader, false);
						if ((qs != null) && qs.isMemoState(2))
						{
							qs.setMemoState(3);
						}
					}
				}
				receiver.deleteMe();
				cancelQuestTimer("QUEST_TIMER", receiver, null);
				cancelQuestTimer("CHECK_TIMER", receiver, null);
				cancelQuestTimer("TIME_LIMIT1", receiver, null);
				cancelQuestTimer("TIME_LIMIT2", receiver, null);
				cancelQuestTimer("TIME_LIMIT3", receiver, null);
				cancelQuestTimer("TIME_LIMIT4", receiver, null);
				cancelQuestTimer("TIME_LIMIT5", receiver, null);
				break;
			}
		}
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final var qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.COMPLETED:
			{
				switch (npc.getId())
				{
					case 32738 :
					{
						if (!qs.isNowAvailable())
						{
							htmltext = "32738-02.htm";
							break;
						}
						qs.setState(State.CREATED);
					}
				}
			}
			case State.CREATED:
			{
				switch (npc.getId())
				{
					case 32738 :
					{
						if (player.getLevel() >= getMinLvl(getId()))
						{
							final int quest_escort = npc.getVariables().getInteger("quest_escort", 0);
							if (quest_escort == 0)
							{
								htmltext = "32738-01.htm";
							}
							else if (quest_escort == 99)
							{
								htmltext = "32738-01c.htm";
							}
							else
							{
								final var leader = npc.getVariables().getObject("leader", Player.class);
								if (leader.isInParty() && leader.getParty().containsPlayer(player))
								{
									final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
									html.setFile(player, player.getLang(), "data/html/scripts/quests/" + getName() + "/32738-01a.htm");
									html.replace("%leader%", leader.getName(null));
									html.replace("%name%", player.getName(null));
									player.sendPacket(html);
									return null;
								}
								else
								{
									final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
									html.setFile(player, player.getLang(), "data/html/scripts/quests/" + getName() + "/32738-01b.htm");
									html.replace("%leader%", leader.getName(null));
									player.sendPacket(html);
									return null;
								}
							}
						}
						else
						{
							htmltext = "32738-03.htm";
						}
						break;
					}
					case 32736 :
					{
						htmltext = "32736-01.htm";
						break;
					}
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case 32738 :
					{
						if (qs.isMemoState(1))
						{
							htmltext = "32738-05.htm";
						}
						else if (qs.isMemoState(2))
						{
							htmltext = "32738-08.htm";
						}
						break;
					}
					case 32736 :
					{
						switch (qs.getMemoState())
						{
							case 1:
							case 2:
							{
								htmltext = "32736-01.htm";
								break;
							}
							case 3:
							{
								qs.exitQuest(QuestType.DAILY, true);
								htmltext = "32736-02.htm";
								break;
							}
							case 4:
							{
								qs.calcReward(getId(), Rnd.get(1, 6), true);
								qs.exitQuest(QuestType.DAILY, true);
								htmltext = "32736-03.htm";
								break;
							}
						}
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _454_CompletelyLost(454, _454_CompletelyLost.class.getSimpleName(), "");
	}
}
