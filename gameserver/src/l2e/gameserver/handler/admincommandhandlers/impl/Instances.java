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

import java.util.StringTokenizer;

import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.entity.Reflection;

public class Instances implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_setinstance", "admin_ghoston", "admin_ghostoff", "admin_createinstance", "admin_destroyinstance", "admin_listinstances"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (command.startsWith("admin_createinstance"))
		{
			final String[] parts = command.split(" ");
			if (parts.length != 3)
			{
				activeChar.sendMessage("Example: //createinstance <id> <templatefile> - ids => 300000 are reserved for dynamic instances");
			}
			else
			{
				try
				{
					final int id = Integer.parseInt(parts[1]);
					if ((id < 300000) && ReflectionManager.getInstance().createReflectionFromTemplate(id, parts[2]))
					{
						activeChar.sendMessage("Instance created.");
					}
					else
					{
						activeChar.sendMessage("Failed to create instance.");
					}
					return true;
				}
				catch (final Exception e)
				{
					activeChar.sendMessage("Failed loading: " + parts[1] + " " + parts[2]);
					return false;
				}
			}
		}
		else if (command.startsWith("admin_listinstances"))
		{
			for (final Reflection temp : ReflectionManager.getInstance().getReflections().values())
			{
				activeChar.sendMessage("Id: " + temp.getId() + " Name: " + temp.getName());
			}
		}
		else if (command.startsWith("admin_setinstance"))
		{
			try
			{
				final int val = Integer.parseInt(st.nextToken());
				final var ref = ReflectionManager.getInstance().getReflection(val);
				if (ref == null)
				{
					activeChar.sendMessage("Instance " + val + " doesnt exist.");
					return false;
				}

				final GameObject target = activeChar.getTarget();
				if (target == null || target instanceof Summon)
				{
					activeChar.sendMessage("Incorrect target.");
					return false;
				}
				if (target instanceof Player)
				{
					final Player player = (Player) target;
					player.sendMessage("Admin set your instance to:" + val);
					player.teleToLocation(player.getX(), player.getY(), player.getZ(), true, ref);
				}
				else
				{
					target.setReflection(ref);
				}
				activeChar.sendMessage("Moved " + target.getName(activeChar.getLang()) + " to instance " + target.getReflectionId() + ".");
				return true;
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Use //setinstance id");
			}
		}
		else if (command.startsWith("admin_destroyinstance"))
		{
			try
			{
				final int val = Integer.parseInt(st.nextToken());
				final var ref = ReflectionManager.getInstance().getReflection(val);
				if (ref != null)
				{
					ref.collapse();
					activeChar.sendMessage("Instance destroyed");
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Use //destroyinstance id");
			}
		}
		else if (command.startsWith("admin_ghoston"))
		{
			activeChar.getAppearance().setGhostMode(true);
			activeChar.sendMessage("Ghost mode enabled");
			activeChar.broadcastUserInfo(true);
			activeChar.decayMe();
			activeChar.spawnMe();
		}
		else if (command.startsWith("admin_ghostoff"))
		{
			activeChar.getAppearance().setGhostMode(false);
			activeChar.sendMessage("Ghost mode disabled");
			activeChar.broadcastUserInfo(true);
			activeChar.decayMe();
			activeChar.spawnMe();
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}