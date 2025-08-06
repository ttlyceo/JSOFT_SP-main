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

import l2e.gameserver.Config;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Invul implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_invul", "admin_setinvul"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		
		if (command.equals("admin_invul"))
		{
			handleInvul(activeChar);
			final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		if (command.equals("admin_setinvul"))
		{
			final var target = activeChar.getTarget();
			if (target != null && target.isCreature())
			{
				handleInvul((Creature) target);
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleInvul(Creature activeChar)
	{
		String text;
		if (activeChar.isInvul())
		{
			activeChar.setIsInvul(false);
			text = activeChar.getName(null) + " is now mortal";
			if (Config.DEBUG)
			{
				_log.info("GM: Gm removed invul mode from character " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ")");
			}
		}
		else
		{
			activeChar.setIsInvul(true);
			text = activeChar.getName(null) + " is now invulnerable";
			if (Config.DEBUG)
			{
				_log.info("GM: Gm activated invul mode for character " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ")");
			}
		}
		if (activeChar.isPlayer())
		{
			activeChar.sendMessage(text);
			((Player) activeChar).broadcastUserInfo(true);
		}
		else
		{
			activeChar.broadcastInfo();
		}
		
	}
}