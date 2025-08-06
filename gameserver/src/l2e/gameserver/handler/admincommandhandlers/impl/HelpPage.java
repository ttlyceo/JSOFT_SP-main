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

import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class HelpPage implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_help"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		
		if (command.startsWith("admin_help"))
		{
			try
			{
				final String val = command.substring(11);
				showHelpPage(activeChar, val);
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}

		return true;
	}

	public static void showHelpPage(Player targetChar, String filename)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(targetChar, targetChar.getLang(), "data/html/admin/" + filename);
		targetChar.sendPacket(adminReply);
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}