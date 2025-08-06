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
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public class Debug implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_debug"
	};

	@Override
	public final boolean useAdminCommand(String command, Player activeChar)
	{
		final String[] commandSplit = command.split(" ");
		if (ADMIN_COMMANDS[0].equalsIgnoreCase(commandSplit[0]))
		{
			GameObject target;
			if (commandSplit.length > 1)
			{
				target = GameObjectsStorage.getPlayer(commandSplit[1].trim());
				if (target == null)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
					return true;
				}
			}
			else
			{
				target = activeChar.getTarget();
			}
			
			if (target instanceof Creature)
			{
				setDebug(activeChar, (Creature) target);
			}
			else
			{
				setDebug(activeChar, activeChar);
			}
		}
		return true;
	}
	
	@Override
	public final String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private final void setDebug(Player activeChar, Creature target)
	{
		if (target.isDebug())
		{
			target.setDebug(null);
			activeChar.sendMessage("Stop debugging " + target.getName(activeChar.getLang()));
		}
		else
		{
			target.setDebug(activeChar);
			activeChar.sendMessage("Start debugging " + target.getName(activeChar.getLang()));
		}
	}
}