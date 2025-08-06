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

import l2e.gameserver.data.parser.BuyListParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.buylist.ProductList;
import l2e.gameserver.network.serverpackets.BuyList;
import l2e.gameserver.network.serverpackets.ExBuySellList;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Shop implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_buy", "admin_gmshop"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_buy"))
		{
			try
			{
				handleBuyRequest(activeChar, command.substring(10));
			}
			catch (final IndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Please specify buylist.");
			}
		}
		else if (command.equals("admin_gmshop"))
		{
			final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gmshops.htm");
			activeChar.sendPacket(adminhtm);
		}
		return true;
	}

	private void handleBuyRequest(Player activeChar, String command)
	{
		int val = -1;
		try
		{
			val = Integer.parseInt(command);
		}
		catch (final Exception e)
		{
			_log.warn("admin buylist failed:" + command);
		}

		final ProductList buyList = BuyListParser.getInstance().getBuyList(val);

		if (buyList != null)
		{
			activeChar.sendPacket(new BuyList(buyList, activeChar.getAdena(), 0));
			activeChar.sendPacket(new ExBuySellList(activeChar, false));
		}
		else
		{
			_log.warn("no buylist with id:" + val);
		}
		activeChar.sendActionFailed();
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}