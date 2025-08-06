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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public class ChangeAccessLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_changelvl"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		handleChangeLevel(command, activeChar);
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleChangeLevel(String command, Player activeChar)
	{
		final String[] parts = command.split(" ");
		if (parts.length == 2)
		{
			try
			{
				final var lvl = Integer.parseInt(parts[1]);
				if (activeChar.getTarget() instanceof Player)
				{
					onLineChange(activeChar, (Player) activeChar.getTarget(), lvl);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //changelvl <target_new_level> | <player_name> <new_level>");
			}
		}
		else if (parts.length == 3)
		{
			final var name = parts[1];
			final var lvl = Integer.parseInt(parts[2]);
			final var player = GameObjectsStorage.getPlayer(name);
			if (player != null)
			{
				onLineChange(activeChar, player, lvl);
			}
			else
			{
				Connection con = null;
				PreparedStatement statement = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement("UPDATE characters SET accesslevel=? WHERE char_name=?");
					statement.setInt(1, lvl);
					statement.setString(2, name);
					statement.execute();
					final var count = statement.getUpdateCount();
					if (count == 0)
					{
						activeChar.sendMessage("Character not found or access level unaltered.");
					}
					else
					{
						activeChar.sendMessage("Character's access level is now set to " + lvl);
					}
				}
				catch (final SQLException se)
				{
					activeChar.sendMessage("SQLException while changing character's access level");
					if (Config.DEBUG)
					{
						se.printStackTrace();
					}
				}
				finally
				{
					DbUtils.closeQuietly(con, statement);
				}
			}
		}
	}

	private void onLineChange(Player activeChar, Player player, int lvl)
	{
		if (lvl >= 0)
		{
			if (AdminParser.getInstance().hasAccessLevel(lvl))
			{
				player.setAccessLevel(lvl);
				player.sendMessage("Your access level has been changed to " + lvl);
				activeChar.sendMessage("Character's access level is now set to " + lvl + ". Effects won't be noticeable until next session.");
			}
			else
			{
				activeChar.sendMessage("You are trying to set unexisting access level: " + lvl + " please try again with a valid one!");
			}
		}
		else
		{
			player.setAccessLevel(lvl);
			player.sendMessage("Your character has been banned. Bye.");
			player.logout();
		}
	}
}