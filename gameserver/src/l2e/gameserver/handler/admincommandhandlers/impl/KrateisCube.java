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

import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.games.krateiscube.KrateisCubeManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class KrateisCube implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_krateiscube_menu", "admin_krateiscube_change"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_krateiscube_menu"))
		{
			showMenu(activeChar);
		}
		else if (command.startsWith("admin_krateiscube_change"))
		{
			if (KrateisCubeManager.getInstance().isPreparing())
			{
				KrateisCubeManager.getInstance().prepareEvent();
			}
			else if (KrateisCubeManager.getInstance().isRegisterTime() && !KrateisCubeManager.getInstance().isActive())
			{
				KrateisCubeManager.getInstance().closeRegistration();
			}
			else if (KrateisCubeManager.getInstance().isActive())
			{
				KrateisCubeManager.getInstance().abortEvent();
			}
			showMenu(activeChar);
		}
		return true;
	}
	
	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/krateiscube.htm");
		if (KrateisCubeManager.getInstance().isRegisterTime() && !KrateisCubeManager.getInstance().isActive() && !KrateisCubeManager.getInstance().isPreparing())
		{
			html.replace("%msg%", "<font color=\"b02e31\">Registration Open</font>");
			html.replace("%date%", "<font color=\"LEVEL\">" + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(KrateisCubeManager.getInstance().getNextMatchTime())) + "</font>");
		}
		else
		{
			if (KrateisCubeManager.getInstance().isPreparing())
			{
				html.replace("%msg%", "<font color=\"LEVEL\">Prepating</font>");
				html.replace("%date%", "<font color=\"b02e31\">Registration Close!</font>");
			}
			else if (KrateisCubeManager.getInstance().isActive() && !KrateisCubeManager.getInstance().isRegisterTime())
			{
				html.replace("%msg%", "<font color=\"LEVEL\">Active Now</font>");
				html.replace("%date%", "<font color=\"b02e31\">Registration Close!</font>");
			}
			else if (KrateisCubeManager.getInstance().isActive() && KrateisCubeManager.getInstance().isRegisterTime())
			{
				html.replace("%msg%", "<font color=\"b02e31\">Registration Open</font>");
				html.replace("%date%", "<font color=\"LEVEL\">" + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(KrateisCubeManager.getInstance().getNextMatchTime())) + "</font>");
			}
		}
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}