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
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class AerialCleft implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_aerial_cleft", "admin_cleft_start", "admin_cleft_stop", "admin_cleft_open_reg", "admin_cleft_clean_time"
	};

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.startsWith(ADMIN_COMMANDS[0]))
		{
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_cleft_start"))
		{
			if (AerialCleftEvent.getInstance().forcedEventStart())
			{
				activeChar.sendMessage("Aerial Cleft started!");
			}
			else
			{
				activeChar.sendMessage("Problem with starting Aerial Cleft.");
			}
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_cleft_stop"))
		{
			if (AerialCleftEvent.getInstance().forcedEventStop())
			{
				activeChar.sendMessage("Aerial Cleft stoped!");
			}
			else
			{
				activeChar.sendMessage("Problem with stoping Aerial Cleft.");
			}
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_cleft_open_reg"))
		{
			if (AerialCleftEvent.getInstance().openRegistration())
			{
				activeChar.sendMessage("Open registration for Aerial Cleft.");
			}
			else
			{
				activeChar.sendMessage("Warning! Aerial Cleft in progress.");
			}
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_cleft_clean_time"))
		{
			if (AerialCleftEvent.getInstance().cleanUpTime())
			{
				activeChar.sendMessage("Clean up reload time for Aerial Cleft.");
			}
			else
			{
				activeChar.sendMessage("Warning! Aerial Cleft in progress.");
			}
			showMenu(activeChar);
			return true;
		}
		return false;
	}
	
	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/gracia.htm");
		activeChar.sendPacket(html);
	}
}