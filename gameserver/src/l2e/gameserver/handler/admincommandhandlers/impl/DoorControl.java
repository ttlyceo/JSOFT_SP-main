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

import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.entity.Castle;

public class DoorControl implements IAdminCommandHandler
{
	private static DoorParser _DoorParser = DoorParser.getInstance();
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_open", "admin_close", "admin_openall", "admin_closeall"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		try
		{
			if (command.startsWith("admin_open "))
			{
				final int doorId = Integer.parseInt(command.substring(11));
				if (_DoorParser.getDoor(doorId) != null)
				{
					_DoorParser.getDoor(doorId).openMe();
				}
				else
				{
					for (final Castle castle : CastleManager.getInstance().getCastles())
					{
						if (castle.getDoor(doorId) != null)
						{
							castle.getDoor(doorId).openMe();
						}
					}
				}
			}
			else if (command.startsWith("admin_close "))
			{
				final int doorId = Integer.parseInt(command.substring(12));
				if (_DoorParser.getDoor(doorId) != null)
				{
					_DoorParser.getDoor(doorId).closeMe();
				}
				else
				{
					for (final Castle castle : CastleManager.getInstance().getCastles())
					{
						if (castle.getDoor(doorId) != null)
						{
							castle.getDoor(doorId).closeMe();
						}
					}
				}
			}
			if (command.equals("admin_closeall"))
			{
				for (final DoorInstance door : _DoorParser.getDoors())
				{
					door.closeMe();
				}
				for (final Castle castle : CastleManager.getInstance().getCastles())
				{
					for (final DoorInstance door : castle.getDoors())
					{
						door.closeMe();
					}
				}
			}
			if (command.equals("admin_openall"))
			{
				for (final DoorInstance door : _DoorParser.getDoors())
				{
					door.openMe();
				}
				for (final Castle castle : CastleManager.getInstance().getCastles())
				{
					for (final DoorInstance door : castle.getDoors())
					{
						door.openMe();
					}
				}
			}
			if (command.equals("admin_open"))
			{
				final GameObject target = activeChar.getTarget();
				if (target instanceof DoorInstance)
				{
					((DoorInstance) target).openMe();
				}
				else
				{
					activeChar.sendMessage("Incorrect target.");
				}
			}

			if (command.equals("admin_close"))
			{
				final GameObject target = activeChar.getTarget();
				if (target instanceof DoorInstance)
				{
					((DoorInstance) target).closeMe();
				}
				else
				{
					activeChar.sendMessage("Incorrect target.");
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}