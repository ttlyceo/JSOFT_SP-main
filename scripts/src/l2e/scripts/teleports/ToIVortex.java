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

import l2e.commons.util.Util;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.service.BotFunctions;

public class ToIVortex extends Quest
{
	public ToIVortex(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30949, 30950, 30951, 30952, 30953, 30954);
		addTalkId(30949, 30950, 30951, 30952, 30953, 30954);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final int npcId = npc.getId();

		switch (event)
		{
			case "1":
			{
				if (hasQuestItems(player, 4401))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4401) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4401, 1);
									member.teleToLocation(114356, 13423, -5096, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4401, 1);
					player.teleToLocation(114356, 13423, -5096, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "2":
			{
				if (hasQuestItems(player, 4401))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4401) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4401, 1);
									member.teleToLocation(114666, 13380, -3608, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4401, 1);
					player.teleToLocation(114666, 13380, -3608, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "3":
			{
				if (hasQuestItems(player, 4401))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4401) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4401, 1);
									member.teleToLocation(111982, 16028, -2120, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4401, 1);
					player.teleToLocation(111982, 16028, -2120, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "4":
			{
				if (hasQuestItems(player, 4402))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4402) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4402, 1);
									member.teleToLocation(114636, 13413, -640, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4402, 1);
					player.teleToLocation(114636, 13413, -640, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "5":
			{
				if (hasQuestItems(player, 4402))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4402) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4402, 1);
									member.teleToLocation(114152, 19902, 928, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4402, 1);
					player.teleToLocation(114152, 19902, 928, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "6":
			{
				if (hasQuestItems(player, 4402))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4402) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4402, 1);
									member.teleToLocation(117131, 16044, 1944, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4402, 1);
					player.teleToLocation(117131, 16044, 1944, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "7":
			{
				if (hasQuestItems(player, 4403))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4403) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4403, 1);
									member.teleToLocation(113026, 17687, 2952, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4403, 1);
					player.teleToLocation(113026, 17687, 2952, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "8":
			{
				if (hasQuestItems(player, 4403))
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
										
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4403) == null)
									{
										continue;
									}
										
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4403, 1);
									member.teleToLocation(115571, 13723, 3960, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4403, 1);
					player.teleToLocation(115571, 13723, 3960, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "9":
			{
				if (hasQuestItems(player, 4403))
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
										
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4403) == null)
									{
										continue;
									}
										
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4403, 1);
									member.teleToLocation(114649, 14144, 4976, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4403, 1);
					player.teleToLocation(114649, 14144, 4976, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "10":
			{
				if (hasQuestItems(player, 4403))
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
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4403) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4403, 1);
									member.teleToLocation(118507, 16605, 5984, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4403, 1);
					player.teleToLocation(118507, 16605, 5984, true, player.getReflection());
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "GREEN":
			{
				if (player.getAdena() >= 10000)
				{
					takeItems(player, 57, 10000);
					giveItems(player, 4401, 1);
				}
				else
				{
					return npcId + "no-adena.htm";
				}
				break;
			}
			case "BLUE":
			{
				if (player.getAdena() >= 10000)
				{
					takeItems(player, 57, 10000);
					giveItems(player, 4402, 1);
				}
				else
				{
					return npcId + "no-adena.htm";
				}
				break;
			}
			case "RED":
			{
				if (player.getAdena() >= 10000)
				{
					takeItems(player, 57, 10000);
					giveItems(player, 4403, 1);
				}
				else
				{
					return npcId + "no-adena.htm";
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}

	public static void main(String[] args)
	{
		new ToIVortex(-1, ToIVortex.class.getSimpleName(), "teleports");
	}
}