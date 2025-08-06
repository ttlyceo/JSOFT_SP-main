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
package l2e.gameserver.handler.admincommandhandlers.impl;

import java.util.StringTokenizer;

import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;

public class Kick implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_kick", "admin_kick_non_gm"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_kick"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final Player plyr = GameObjectsStorage.getPlayer(player);
				if (plyr != null)
				{
					if (plyr.isInOfflineMode())
					{
						plyr.unsetVar("offline");
						plyr.unsetVar("offlineTime");
						plyr.unsetVar("storemode");
					}
					
					if (plyr.isSellingBuffs())
					{
						plyr.unsetVar("offlineBuff");
					}
					plyr.kick();
					activeChar.sendMessage("You kicked " + plyr.getName(activeChar.getLang()) + " from the game.");
				}
			}
		}
		if (command.startsWith("admin_kick_non_gm"))
		{
			int counter = 0;
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (!player.isGM())
				{
					counter++;
					if (player.isInOfflineMode())
					{
						player.unsetVar("offline");
						player.unsetVar("offlineTime");
						player.unsetVar("storemode");
					}
					
					if (player.isSellingBuffs())
					{
						player.unsetVar("offlineBuff");
					}
					player.logout();
				}
			}
			activeChar.sendMessage("Kicked " + counter + " players");
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}