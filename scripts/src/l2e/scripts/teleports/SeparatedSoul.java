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

import java.util.HashMap;
import java.util.Map;

import l2e.commons.util.Util;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.service.BotFunctions;

public class SeparatedSoul extends Quest
{
	private static final Map<String, Location> LOCATIONS = new HashMap<>();
	static
	{
		LOCATIONS.put("HuntersVillage", new Location(117031, 76769, -2696));
		LOCATIONS.put("AntharasLair", new Location(131116, 114333, -3704));
		LOCATIONS.put("AntharasLairDeep", new Location(148447, 110582, -3944));
		LOCATIONS.put("AntharasLairMagicForceFieldBridge", new Location(146129, 111232, -3568));
		LOCATIONS.put("DragonValley", new Location(73122, 118351, -3714));
		LOCATIONS.put("DragonValleyCenter", new Location(99218, 110283, -3696));
		LOCATIONS.put("DragonValleyNorth", new Location(116992, 113716, -3056));
		LOCATIONS.put("DragonValleySouth", new Location(113203, 121063, -3712));
	}

	public SeparatedSoul(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32864, 32865, 32866, 32867, 32868, 32869, 32870, 32891);
		addTalkId(32864, 32865, 32866, 32867, 32868, 32869, 32870, 32891);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (LOCATIONS.containsKey(event))
		{
			if (player.getLevel() >= 80)
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
								
								if (!Util.checkIfInRange(1000, player, member, true) || member.getLevel() < 80)
								{
									continue;
								}
								
								if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
								{
									continue;
								}
								member.teleToLocation(LOCATIONS.get(event), true, member.getReflection());
							}
						}
					}
				}
				player.teleToLocation(LOCATIONS.get(event), true, player.getReflection());
			}
			else
			{
				return "no-level.htm";
			}
		}
		else if ("Synthesis".equals(event))
		{
			if (hasQuestItems(player, 17266) && hasQuestItems(player, 17267))
			{
				takeItems(player, 17266, 1);
				takeItems(player, 17267, 1);
				giveItems(player, 17268, 1);
			}
			else
			{
				return "no-items.htm";
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new SeparatedSoul(-1, SeparatedSoul.class.getSimpleName(), "teleports");
	}
}