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
import l2e.gameserver.network.serverpackets.ExAgitAuctionCmd;
import l2e.gameserver.network.serverpackets.ExBrBuffEventState;
import l2e.gameserver.network.serverpackets.ExGoodsInventoryChangedNotify;
import l2e.gameserver.network.serverpackets.ExGoodsInventoryInfo;
import l2e.gameserver.network.serverpackets.ExGoodsInventoryResult;
import l2e.gameserver.network.serverpackets.ExSay2Fail;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Packets implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_test", "admin_1_packet", "admin_2_packet", "admin_3_packet", "admin_4_packet", "admin_5_packet", "admin_6_packet"
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
		
		if (command.startsWith("admin_test"))
		{
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_1_packet"))
		{
			activeChar.broadcastPacket(new ExAgitAuctionCmd());
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_2_packet"))
		{
			activeChar.broadcastPacket(new ExBrBuffEventState(10, 20573, 1, 60));
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_3_packet"))
		{
			activeChar.broadcastPacket(new ExSay2Fail());
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_4_packet"))
		{
			activeChar.broadcastPacket(new ExGoodsInventoryChangedNotify());
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_5_packet"))
		{
			activeChar.broadcastPacket(new ExGoodsInventoryInfo());
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_6_packet"))
		{
			activeChar.broadcastPacket(new ExGoodsInventoryResult(2));
			showMenu(activeChar);
			return true;
		}
		return false;
	}

	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/packets-test.htm");
		activeChar.sendPacket(html);
	}
}