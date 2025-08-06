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

import java.text.SimpleDateFormat;
import java.util.Date;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SpecialRatesParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class SpecialRates implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_special_rates", "admin_rate_enable", "admin_rate_disable"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_rate_enable"))
		{
			if (SpecialRatesParser.getInstance().enableRates())
			{
				activeChar.sendMessage("Special rates activated!");
			}
			else
			{
				activeChar.sendMessage("Special rates already active!");
			}
			ThreadPoolManager.getInstance().schedule(new RefreshMenu(activeChar), 100);
			return true;
		}
		else if (command.equals("admin_rate_disable"))
		{
			if (!SpecialRatesParser.getInstance().disableRates())
			{
				activeChar.sendMessage("Special rates not activate!");
			}
			else
			{
				activeChar.sendMessage("Special rates disabled!");
			}
			ThreadPoolManager.getInstance().schedule(new RefreshMenu(activeChar), 100);
			return true;
		}
		showMenu(activeChar);
		return true;
	}
	
	private void showMenu(Player activeChar)
	{
		final var isActive = SpecialRatesParser.getInstance().isActive();
		final var time = SpecialRatesParser.getInstance().getTime();
		
		final var nextDate = new Date(time);
		final var DATE_FORMAT = "dd-MMM-yyyy HH:mm";
		final var sdf = new SimpleDateFormat(DATE_FORMAT);
		
		final var adminhtm = new NpcHtmlMessage(5);
		adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/specialRates.htm");
		adminhtm.replace("%isActive%", isActive ? "<font color=00FF00>Active</font>" : "<font color=FF5155>Disable</font>");
		adminhtm.replace("%text%", isActive ? "Will disable in" : "Will enable in");
		adminhtm.replace("%time%", String.valueOf(sdf.format(nextDate)));
		activeChar.sendPacket(adminhtm);
	}
	
	protected class RefreshMenu implements Runnable
	{
		private final Player _player;
		
		private RefreshMenu(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player != null)
			{
				showMenu(_player);
			}
		}
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}