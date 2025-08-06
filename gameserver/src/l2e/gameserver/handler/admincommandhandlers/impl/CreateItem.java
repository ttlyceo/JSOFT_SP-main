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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class CreateItem implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_itemcreate", "admin_create_item", "admin_create_coin", "admin_give_item_target", "admin_give_item_to_all", "admin_give_item_all_with_check"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);

		if (command.equals("admin_itemcreate"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/itemcreation.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_create_item"))
		{
			try
			{
				final String val = command.substring(17);
				final StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2)
				{
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					final String num = st.nextToken();
					final long numval = Long.parseLong(num);
					createItem(activeChar, activeChar, idval, numval);
				}
				else if (st.countTokens() == 1)
				{
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					createItem(activeChar, activeChar, idval, 1);
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //create_item <itemId> [amount]");
			}
			catch (final NumberFormatException nfe)
			{
				activeChar.sendMessage("Specify a valid number.");
			}
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/itemcreation.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_create_coin"))
		{
			try
			{
				final String val = command.substring(17);
				final StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2)
				{
					final String name = st.nextToken();
					final int idval = getCoinId(name);
					if (idval > 0)
					{
						final String num = st.nextToken();
						final long numval = Long.parseLong(num);
						createItem(activeChar, activeChar, idval, numval);
					}
				}
				else if (st.countTokens() == 1)
				{
					final String name = st.nextToken();
					final int idval = getCoinId(name);
					createItem(activeChar, activeChar, idval, 1);
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //create_coin <name> [amount]");
			}
			catch (final NumberFormatException nfe)
			{
				activeChar.sendMessage("Specify a valid number.");
			}
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/itemcreation.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_give_item_target"))
		{
			try
			{
				Player target;
				if (activeChar.getTarget() instanceof Player)
				{
					target = (Player) activeChar.getTarget();
				}
				else
				{
					activeChar.sendMessage("Invalid target.");
					return false;
				}

				final String val = command.substring(22);
				final StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2)
				{
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					final String num = st.nextToken();
					final long numval = Long.parseLong(num);
					createItem(activeChar, target, idval, numval);
				}
				else if (st.countTokens() == 1)
				{
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					createItem(activeChar, target, idval, 1);
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //give_item_target <itemId> [amount]");
			}
			catch (final NumberFormatException nfe)
			{
				activeChar.sendMessage("Specify a valid number.");
			}
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/itemcreation.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_give_item_to_all"))
		{
			final String val = command.substring(22);
			final StringTokenizer st = new StringTokenizer(val);
			int idval = 0;
			long numval = 0;
			if (st.countTokens() == 2)
			{
				final String id = st.nextToken();
				idval = Integer.parseInt(id);
				final String num = st.nextToken();
				numval = Long.parseLong(num);
			}
			else if (st.countTokens() == 1)
			{
				final String id = st.nextToken();
				idval = Integer.parseInt(id);
				numval = 1;
			}
			int counter = 0;
			final Item template = ItemsParser.getInstance().getTemplate(idval);
			if (template == null)
			{
				activeChar.sendMessage("This item doesn't exist.");
				return false;
			}
			if ((numval > 10) && !template.isStackable())
			{
				activeChar.sendMessage("This item does not stack - Creation aborted.");
				return false;
			}
			for (final Player onlinePlayer : GameObjectsStorage.getPlayers())
			{
				if ((activeChar != onlinePlayer) && onlinePlayer.isOnline() && ((onlinePlayer.getClient() != null) && !onlinePlayer.getClient().isDetached()))
				{
					onlinePlayer.getInventory().addItem("Admin", idval, numval, onlinePlayer, activeChar);
					onlinePlayer.sendMessage("Admin spawned " + numval + " " + template.getName(onlinePlayer.getLang()) + " in your inventory.");
					counter++;
				}
			}
			activeChar.sendMessage(counter + " players rewarded with " + template.getName(activeChar.getLang()));
		}
		else if (command.startsWith("admin_give_item_all_with_check"))
		{
			final String val = command.substring(30);
			final StringTokenizer st = new StringTokenizer(val);
			int idval = 0;
			long numval = 0;
			if (st.countTokens() == 2)
			{
				final String id = st.nextToken();
				idval = Integer.parseInt(id);
				final String num = st.nextToken();
				numval = Long.parseLong(num);
			}
			else if (st.countTokens() == 1)
			{
				final String id = st.nextToken();
				idval = Integer.parseInt(id);
				numval = 1;
			}
			int counter = 0;
			final Item template = ItemsParser.getInstance().getTemplate(idval);
			if (template == null)
			{
				activeChar.sendMessage("This item doesn't exist.");
				return false;
			}
			if ((numval > 10) && !template.isStackable())
			{
				activeChar.sendMessage("This item does not stack - Creation aborted.");
				return false;
			}
			
			final List<String> hwids = new ArrayList<>();
			final boolean isIpCheck = Config.PROTECTION.equalsIgnoreCase("NONE");
			for (final Player onlinePlayer : GameObjectsStorage.getPlayers())
			{
				if ((activeChar != onlinePlayer) && onlinePlayer.isOnline() && ((onlinePlayer.getClient() != null) && !onlinePlayer.getClient().isDetached()))
				{
					final String plHwid = isIpCheck ? onlinePlayer.getIPAddress() : onlinePlayer.getHWID();
					if (hwids.contains(plHwid))
					{
						continue;
					}
					onlinePlayer.addItem("Admin", idval, numval, onlinePlayer, true);
					hwids.add(plHwid);
					counter++;
				}
			}
			activeChar.sendMessage(counter + " players rewarded with " + template.getName(activeChar.getLang()));
			hwids.clear();
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void createItem(Player activeChar, Player target, int id, long num)
	{
		final Item template = ItemsParser.getInstance().getTemplate(id);
		if (template == null)
		{
			activeChar.sendMessage("This item doesn't exist.");
			return;
		}
		if ((num > 10) && !template.isStackable())
		{
			activeChar.sendMessage("This item does not stack - Creation aborted.");
			return;
		}

		target.addItem("Admin", id, num, activeChar, true);

		if (activeChar != target)
		{
			target.sendMessage("Admin spawned " + num + " " + template.getName(target.getLang()) + " in your inventory.");
		}
		activeChar.sendMessage("You have spawned " + num + " " + template.getName(activeChar.getLang()) + "(" + id + ") in " + target.getName(null) + " inventory.");
	}

	private int getCoinId(String name)
	{
		int id;
		if (name.equalsIgnoreCase("adena"))
		{
			id = 57;
		}
		else if (name.equalsIgnoreCase("ancientadena"))
		{
			id = 5575;
		}
		else if (name.equalsIgnoreCase("festivaladena"))
		{
			id = 6673;
		}
		else if (name.equalsIgnoreCase("blueeva"))
		{
			id = 4355;
		}
		else if (name.equalsIgnoreCase("goldeinhasad"))
		{
			id = 4356;
		}
		else if (name.equalsIgnoreCase("silvershilen"))
		{
			id = 4357;
		}
		else if (name.equalsIgnoreCase("bloodypaagrio"))
		{
			id = 4358;
		}
		else if (name.equalsIgnoreCase("fantasyislecoin"))
		{
			id = 13067;
		}
		else
		{
			id = 0;
		}
		return id;
	}
}