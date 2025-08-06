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
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.scripts.ai.AbstractNpcAI;

/**
 * Based on L2J Eternity-World
 */
public class FreyaTeleport extends AbstractNpcAI
{
	public FreyaTeleport(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(32734);
		addTalkId(32734);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String htmltext = "";
		
		switch (npc.getId())
		{
			case 32734 :
			{
				final var template = TeleLocationParser.getInstance().getTemplate(300005);
				if (template != null)
				{
					if (player.getLevel() < template.getMinLevel())
					{
						return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "SeedOfAnnihilation.CANT_TELE") + "</body></html>";
					}
					else
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
										member.teleToLocation(template.getLocation(), true, member.getReflection());
									}
								}
							}
						}
						player.teleToLocation(template.getLocation(), true, player.getReflection());
					}
				}
				break;
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new FreyaTeleport(FreyaTeleport.class.getSimpleName(), "teleports");
	}
}
