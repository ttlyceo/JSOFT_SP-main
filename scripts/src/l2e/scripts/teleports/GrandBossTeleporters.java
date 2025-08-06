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
package l2e.scripts.teleports;

import java.util.List;

import l2e.commons.util.Util;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.zone.type.NoRestartZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.scripts.ai.grandboss.AntharasManager;
import l2e.scripts.ai.grandboss.ValakasManager;

public class GrandBossTeleporters extends Quest
{
	private Quest valakasAI()
	{
		return QuestManager.getInstance().getQuest(ValakasManager.class.getSimpleName());
	}
	
	private Quest antharasAI()
	{
		return QuestManager.getInstance().getQuest(AntharasManager.class.getSimpleName());
	}
	
	public GrandBossTeleporters(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(13001, 31859, 31384, 31385, 31540, 31686, 31687, 31759);
		addTalkId(13001, 31859, 31384, 31385, 31540, 31686, 31687, 31759);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState st = player.getQuestState(getName());
		
		if (event.equalsIgnoreCase("31540"))
		{
			if (st.hasQuestItems(7267))
			{
				if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
				{
					if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
					{
						for (final Player member : player.getParty().getMembers())
						{
							if (member != null)
							{
								if (member.getObjectId() == player.getObjectId())
								{
									continue;
								}
								
								if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(7267) == null)
								{
									continue;
								}
								
								if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
								{
									continue;
								}
								member.teleToLocation(183813, -115157, -3303, true, member.getReflection());
							}
						}
					}
				}
				player.teleToLocation(183813, -115157, -3303, true, player.getReflection());
			}
			else
			{
				htmltext = "31540-06.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		switch (npc.getId())
		{
			case 13001 :
				if (antharasAI() != null)
				{
					final NoRestartZone zone = ZoneManager.getInstance().getZoneById(70050, NoRestartZone.class);
					final int status = EpicBossManager.getInstance().getBossStatus(29068);
					
					if (status == 2)
					{
						htmltext = "13001-02.htm";
					}
					else if (status == 3)
					{
						htmltext = "13001-01.htm";
					}
					else if (player.isInParty())
					{
						final Party party = player.getParty();
						final boolean isInCC = party.isInCommandChannel();
						final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
						final boolean isPartyLeader = (isInCC) ? party.getCommandChannel().isLeader(player) : party.isLeader(player);
						if (!isPartyLeader)
						{
							htmltext = "13001-05.htm";
						}
						else if (!hasQuestItems(player, 3865))
						{
							htmltext = "13001-03.htm";
						}
						else if ((zone.getPlayersInside().size() + members.size()) > 200)
						{
							htmltext = "13001-04.htm";
						}
						else
						{
							boolean canEnter = true;
							for (final Player member : members)
							{
								if (member != null)
								{
									if (!hasQuestItems(member, 3865))
									{
										final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ITEM_REQUIREMENT_NOT_SUFFICIENT);
										sm.addPcName(member);
										player.broadCast(sm);
										canEnter = false;
										return null;
									}
									
									if (!member.isInsideRadius(player, 1000, true, true))
									{
										final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_IN_LOCATION_THAT_CANNOT_BE_ENTERED);
										sm.addPcName(member);
										player.broadCast(sm);
										canEnter = false;
										return null;
									}
								}
							}
							
							if (canEnter)
							{
								for (final Player member : members)
								{
									if (member.isInsideRadius(npc, 1000, true, false))
									{
										member.teleToLocation(179700 + getRandom(700), 113800 + getRandom(2100), -7709, true, member.getReflection());
									}
								}
								
								if (status == 0)
								{
									final GrandBossInstance antharas = EpicBossManager.getInstance().getBoss(29068);
									antharasAI().notifyEvent("waiting", antharas, player);
								}
							}
						}
					}
					else
					{
						if (!hasQuestItems(player, 3865))
						{
							htmltext = "13001-03.htm";
						}
						else if ((zone.getPlayersInside().size() + 1) > 200)
						{
							htmltext = "13001-04.htm";
						}
						else
						{
							player.teleToLocation(179700 + getRandom(700), 113800 + getRandom(2100), -7709, true, player.getReflection());
							if (status == 0)
							{
								final GrandBossInstance antharas = EpicBossManager.getInstance().getBoss(29068);
								antharasAI().notifyEvent("waiting", antharas, player);
							}
						}
					}
				}
				break;
			case 31859 :
				if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
				{
					if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
					{
						for (final Player member : player.getParty().getMembers())
						{
							if (member != null)
							{
								if (member.getObjectId() == player.getObjectId())
								{
									continue;
								}
								
								if (!Util.checkIfInRange(1000, player, member, true))
								{
									continue;
								}
								
								if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
								{
									continue;
								}
								member.teleToLocation(79800 + getRandom(600), 151200 + getRandom(1100), -3534, true, member.getReflection());
							}
						}
					}
				}
				player.teleToLocation(79800 + getRandom(600), 151200 + getRandom(1100), -3534, true, player.getReflection());
				break;
			case 31385 :
				if (valakasAI() != null)
				{
					final int status = EpicBossManager.getInstance().getBossStatus(29028);
					final NoRestartZone zone = ZoneManager.getInstance().getZoneById(70052, NoRestartZone.class);
					if ((status == 0) || (status == 1))
					{
						if (zone.getPlayersInside().size() >= 200)
						{
							htmltext = "31385-03.htm";
						}
						else if (hasQuestItems(player, 7267))
						{
							if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
							{
								if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
								{
									for (final Player member : player.getParty().getMembers())
									{
										if (member != null)
										{
											if (member.getObjectId() == player.getObjectId())
											{
												continue;
											}
											
											if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(7267) == null)
											{
												continue;
											}
											
											if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
											{
												continue;
											}
											member.teleToLocation(204328 + getRandom(600), -111874 + getRandom(600), 70, true, member.getReflection());
										}
									}
								}
							}
							player.teleToLocation(204328 + getRandom(600), -111874 + getRandom(600), 70, true, player.getReflection());
							if (status == 0)
							{
								final var valakas = EpicBossManager.getInstance().getBoss(29028);
								valakasAI().notifyEvent("waiting", valakas, player);
							}
						}
						else
						{
							htmltext = "31385-04.htm";
						}
					}
					else if (status == 2)
					{
						htmltext = "31385-02.htm";
					}
					else
					{
						htmltext = "31385-01.htm";
					}
				}
				else
				{
					htmltext = "31385-01.htm";
				}
				break;
			case 31384 :
				DoorParser.getInstance().getDoor(24210004).openMe();
				break;
			case 31686 :
				DoorParser.getInstance().getDoor(24210005).openMe();
				break;
			case 31687 :
				DoorParser.getInstance().getDoor(24210006).openMe();
				break;
			case 31540 :
				final NoRestartZone zone = ZoneManager.getInstance().getZoneById(70052, NoRestartZone.class);
				if (zone.getPlayersInside().size() < 50)
				{
					htmltext = "31540-01.htm";
				}
				else if (zone.getPlayersInside().size() < 100)
				{
					htmltext = "31540-02.htm";
				}
				else if (zone.getPlayersInside().size() < 150)
				{
					htmltext = "31540-03.htm";
				}
				else if (zone.getPlayersInside().size() < 200)
				{
					htmltext = "31540-04.htm";
				}
				else
				{
					htmltext = "31540-05.htm";
				}
				break;
			case 31759 :
				if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
				{
					if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
					{
						for (final Player member : player.getParty().getMembers())
						{
							if (member != null)
							{
								if (member.getObjectId() == player.getObjectId())
								{
									continue;
								}
								
								if (!Util.checkIfInRange(1000, player, member, true))
								{
									continue;
								}
								
								if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
								{
									continue;
								}
								member.teleToLocation(150037 + getRandom(500), -57720 + getRandom(500), -2976, true, member.getReflection());
							}
						}
					}
				}
				player.teleToLocation(150037 + getRandom(500), -57720 + getRandom(500), -2976, true, player.getReflection());
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new GrandBossTeleporters(-1, GrandBossTeleporters.class.getSimpleName(), "teleports");
	}
}