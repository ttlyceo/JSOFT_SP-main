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
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ControllableMobInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.taskmanager.DecayTaskManager;

public class Res implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_res", "admin_res_monster"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_res "))
		{
			handleRes(activeChar, command.split(" ")[1]);
		}
		else if (command.equals("admin_res"))
		{
			handleRes(activeChar);
		}
		else if (command.startsWith("admin_res_monster "))
		{
			handleNonPlayerRes(activeChar, command.split(" ")[1]);
		}
		else if (command.equals("admin_res_monster"))
		{
			handleNonPlayerRes(activeChar);
		}

		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleRes(Player activeChar)
	{
		handleRes(activeChar, null);
	}

	private void handleRes(Player activeChar, String resParam)
	{
		GameObject obj = activeChar.getTarget();

		if (resParam != null)
		{
			final Player plyr = GameObjectsStorage.getPlayer(resParam);

			if (plyr != null)
			{
				obj = plyr;
			}
			else
			{
				try
				{
					final int radius = Integer.parseInt(resParam);

					for (final Player knownPlayer : World.getInstance().getAroundPlayers(activeChar, radius, 200))
					{
						doResurrect(knownPlayer);
					}

					activeChar.sendMessage("Resurrected all players within a " + radius + " unit radius.");
					return;
				}
				catch (final NumberFormatException e)
				{
					activeChar.sendMessage("Enter a valid player name or radius.");
					return;
				}
			}
		}

		if (obj == null)
		{
			obj = activeChar;
		}

		if (obj instanceof ControllableMobInstance)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		doResurrect((Creature) obj);

		if (Config.DEBUG)
		{
			_log.info("GM: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") resurrected character " + obj.getObjectId());
		}
	}

	private void handleNonPlayerRes(Player activeChar)
	{
		handleNonPlayerRes(activeChar, "");
	}

	private void handleNonPlayerRes(Player activeChar, String radiusStr)
	{
		final GameObject obj = activeChar.getTarget();

		try
		{
			int radius = 0;

			if (!radiusStr.isEmpty())
			{
				radius = Integer.parseInt(radiusStr);

				for (final Creature knownChar : World.getInstance().getAroundCharacters(activeChar, radius, 200))
				{
					if (!(knownChar instanceof Player) && !(knownChar instanceof ControllableMobInstance))
					{
						doResurrect(knownChar);
					}
				}

				activeChar.sendMessage("Resurrected all non-players within a " + radius + " unit radius.");
			}
		}
		catch (final NumberFormatException e)
		{
			activeChar.sendMessage("Enter a valid radius.");
			return;
		}

		if (obj instanceof Player || obj instanceof ControllableMobInstance)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		doResurrect((Creature) obj);
	}

	private void doResurrect(Creature targetChar)
	{
		if (!targetChar.isDead())
		{
			return;
		}

		if (targetChar instanceof Player)
		{
			((Player) targetChar).restoreExp(100.0);
		}
		else
		{
			DecayTaskManager.getInstance().cancelDecayTask(targetChar);
		}
		targetChar.doRevive();
	}
}