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

import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.handler.admincommandhandlers.AdminCommandHandler;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Menu implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_char_manage", "admin_teleport_character_to_menu", "admin_recall_char_menu", "admin_recall_party_menu", "admin_recall_clan_menu", "admin_goto_char_menu", "admin_kick_menu", "admin_kill_menu", "admin_ban_menu", "admin_unban_menu"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_char_manage"))
		{
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_teleport_character_to_menu"))
		{
			final String[] data = command.split(" ");
			if (data.length == 5)
			{
				final String playerName = data[1];
				final Player player = GameObjectsStorage.getPlayer(playerName);
				if (player != null)
				{
					teleportCharacter(player, Integer.parseInt(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4]), activeChar, "Admin is teleporting you.");
				}
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_recall_char_menu"))
		{
			try
			{
				final String targetName = command.substring(23);
				final Player player = GameObjectsStorage.getPlayer(targetName);
				teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar, "Admin is teleporting you.");
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.startsWith("admin_recall_party_menu"))
		{
			final int x = activeChar.getX(), y = activeChar.getY(),
			        z = activeChar.getZ();
			try
			{
				final String targetName = command.substring(24);
				final Player player = GameObjectsStorage.getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return true;
				}
				if (!player.isInParty())
				{
					activeChar.sendMessage("Player is not in party.");
					teleportCharacter(player, x, y, z, activeChar, "Admin is teleporting you.");
					return true;
				}
				for (final Player pm : player.getParty().getMembers())
				{
					teleportCharacter(pm, x, y, z, activeChar, "Your party is being teleported by an Admin.");
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //recall_party_menu <char_name>");
			}
		}
		else if (command.startsWith("admin_recall_clan_menu"))
		{
			final int x = activeChar.getX(), y = activeChar.getY(),
			        z = activeChar.getZ();
			try
			{
				final String targetName = command.substring(23);
				final Player player = GameObjectsStorage.getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return true;
				}
				final Clan clan = player.getClan();
				if (clan == null)
				{
					activeChar.sendMessage("Player is not in a clan.");
					teleportCharacter(player, x, y, z, activeChar, "Admin is teleporting you.");
					return true;
				}
				
				for (final Player member : clan.getOnlineMembers(0))
				{
					teleportCharacter(member, x, y, z, activeChar, "Your clan is being teleported by an Admin.");
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //recall_clan_menu <char_name>");
			}
		}
		else if (command.startsWith("admin_goto_char_menu"))
		{
			try
			{
				final String targetName = command.substring(21);
				final Player player = GameObjectsStorage.getPlayer(targetName);
				if (player != null)
				{
					teleportToCharacter(activeChar, player);
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.equals("admin_kill_menu"))
		{
			handleKill(activeChar);
		}
		else if (command.startsWith("admin_kick_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final Player plyr = GameObjectsStorage.getPlayer(player);
				String text;
				if (plyr != null)
				{
					if (plyr.isInOfflineMode())
					{
						plyr.unsetVar("offline");
						plyr.unsetVar("offlineTime");
						plyr.unsetVar("storemode");
					}
					
					if (plyr.isSellingBuffs())
					{
						plyr.unsetVar("offlineBuff");
					}
					plyr.logout();
					text = "You kicked " + plyr.getName(null) + " from the game.";
				}
				else
				{
					text = "Player " + player + " was not found in the game.";
				}
				activeChar.sendMessage(text);
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_ban_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				final String subCommand = "admin_ban_char";
				if (!AdminParser.getInstance().hasAccess(subCommand, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access right to use this command!");
					_log.warn("Character " + activeChar.getName(null) + " tryed to use admin command " + subCommand + ", but have no access to it!");
					return false;
				}
				final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(subCommand);
				ach.useAdminCommand(subCommand + command.substring(14), activeChar);
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_unban_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				final String subCommand = "admin_unban_char";
				if (!AdminParser.getInstance().hasAccess(subCommand, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access right to use this command!");
					_log.warn("Character " + activeChar.getName(null) + " tryed to use admin command " + subCommand + ", but have no access to it!");
					return false;
				}
				final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(subCommand);
				ach.useAdminCommand(subCommand + command.substring(16), activeChar);
			}
			showMainPage(activeChar);
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleKill(Player activeChar)
	{
		handleKill(activeChar, null);
	}
	
	private void handleKill(Player activeChar, String player)
	{
		final GameObject obj = activeChar.getTarget();
		Creature target = (Creature) obj;
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/main_menu.htm");
		activeChar.sendPacket(adminhtm);
		
		if (player != null)
		{
			final Player plyr = GameObjectsStorage.getPlayer(player);
			if (plyr != null)
			{
				target = plyr;
				activeChar.sendMessage("You killed " + plyr.getName(null));
			}
		}
		if (target != null)
		{
			if (target instanceof Player)
			{
				target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/charmanage.htm");
				activeChar.sendPacket(adminhtm);
			}
			else
			{
				target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
	}
	
	private void teleportCharacter(Player player, int x, int y, int z, Player activeChar, String message)
	{
		if (player != null)
		{
			player.sendMessage(message);
			player.teleToLocation(x, y, z, true, activeChar.getReflection());
		}
		showMainPage(activeChar);
	}
	
	private void teleportToCharacter(Player activeChar, GameObject target)
	{
		Player player = null;
		if (target instanceof Player)
		{
			player = (Player) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		}
		else
		{
			activeChar.teleToLocation(player.getX(), player.getY(), player.getZ(), true, player.getReflection());
			activeChar.sendMessage("You're teleporting yourself to character " + player.getName(null));
		}
		showMainPage(activeChar);
	}
	
	private void showMainPage(Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/charmanage.htm");
		activeChar.sendPacket(adminhtm);
	}
}