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
import l2e.gameserver.handler.admincommandhandlers.impl.model.ViewerUtils;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class LogsViewer implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_logsviewer", "admin_startViewer", "admin_stopViewer", "admin_viewLog",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		
		if (command.startsWith("admin_logsviewer"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/logsViewer.htm");
			activeChar.sendPacket(adminhtm);
			return true;
		}
		
		final String file = command.split(" ")[1];
		if (command.startsWith("admin_viewLog"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/logsViewer.htm");
			activeChar.sendPacket(adminhtm);
			ViewerUtils.sendCbWindow(activeChar, file);
			return true;
		}
		if (command.startsWith("admin_startViewer"))
		{
			ViewerUtils.startLogViewer(activeChar, file);
			return true;
		}
		if (command.startsWith("admin_stopViewer"))
		{
			ViewerUtils.stopLogViewer(activeChar, file);
			return true;
		}
		return false;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}