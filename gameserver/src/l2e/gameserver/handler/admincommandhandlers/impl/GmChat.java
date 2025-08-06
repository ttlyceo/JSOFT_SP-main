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

import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class GmChat implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_gmchat", "admin_snoop", "admin_gmchat_menu"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_gmchat"))
		{
			handleGmChat(command, activeChar);
		}
		else if (command.startsWith("admin_snoop"))
		{
			snoop(command, activeChar);
		}
		if (command.startsWith("admin_gmchat_menu"))
		{
			final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		return true;
	}
	
	private void snoop(String command, Player activeChar)
	{
		GameObject target = null;
		if (command.length() > 12)
		{
			target = GameObjectsStorage.getPlayer(command.substring(12));
		}
		if (target == null)
		{
			target = activeChar.getTarget();
		}
		
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.SELECT_TARGET);
			return;
		}
		if (!(target instanceof Player))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		final Player player = (Player) target;
		player.addSnooper(activeChar);
		activeChar.addSnooped(player);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleGmChat(String command, Player activeChar)
	{
		try
		{
			int offset = 0;
			String text;
			if (command.startsWith("admin_gmchat_menu"))
			{
				offset = 18;
			}
			else
			{
				offset = 13;
			}
			text = command.substring(offset);
			final CreatureSay cs = new CreatureSay(0, Say2.ALLIANCE, activeChar.getName(null), text);
			AdminParser.getInstance().broadcastToGMs(cs);
		}
		catch (final StringIndexOutOfBoundsException e)
		{}
	}
}