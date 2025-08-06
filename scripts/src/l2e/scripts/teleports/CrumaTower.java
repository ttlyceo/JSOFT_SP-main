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
import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.BotFunctions;
import l2e.scripts.ai.AbstractNpcAI;

public class CrumaTower extends AbstractNpcAI
{
	public CrumaTower(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(30483);
		addTalkId(30483);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "";
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		final var template = TeleLocationParser.getInstance().getTemplate(300000);
		if (template != null)
		{
			if (player.getLevel() > template.getMaxLevel())
			{
				htmltext = "30483.htm";
			}
			else
			{
				if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
				{
					if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
					{
						for (final var member : player.getParty().getMembers())
						{
							if (member != null)
							{
								if (member.getObjectId() == player.getObjectId())
								{
									continue;
								}
								
								if (!Util.checkIfInRange(1000, player, member, true) || member.getLevel() > template.getMaxLevel())
								{
									continue;
								}
								
								if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
								{
									continue;
								}
								member.teleToLocation(template.getLocation(), true, member.getReflection());
							}
						}
					}
				}
				player.teleToLocation(template.getLocation(), true, player.getReflection());
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new CrumaTower(CrumaTower.class.getSimpleName(), "teleports");
	}
}