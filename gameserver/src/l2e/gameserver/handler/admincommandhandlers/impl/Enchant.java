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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Enchant implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_seteh", "admin_setec", "admin_seteg", "admin_setel", "admin_seteb", "admin_setew", "admin_setes", "admin_setle", "admin_setre", "admin_setlf", "admin_setrf", "admin_seten", "admin_setun", "admin_setba", "admin_setbe", "admin_enchant"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_enchant"))
		{
			showMainPage(activeChar);
		}
		else
		{
			int armorType = -1;

			if (command.startsWith("admin_seteh"))
			{
				armorType = Inventory.PAPERDOLL_HEAD;
			}
			else if (command.startsWith("admin_setec"))
			{
				armorType = Inventory.PAPERDOLL_CHEST;
			}
			else if (command.startsWith("admin_seteg"))
			{
				armorType = Inventory.PAPERDOLL_GLOVES;
			}
			else if (command.startsWith("admin_seteb"))
			{
				armorType = Inventory.PAPERDOLL_FEET;
			}
			else if (command.startsWith("admin_setel"))
			{
				armorType = Inventory.PAPERDOLL_LEGS;
			}
			else if (command.startsWith("admin_setew"))
			{
				armorType = Inventory.PAPERDOLL_RHAND;
			}
			else if (command.startsWith("admin_setes"))
			{
				armorType = Inventory.PAPERDOLL_LHAND;
			}
			else if (command.startsWith("admin_setle"))
			{
				armorType = Inventory.PAPERDOLL_LEAR;
			}
			else if (command.startsWith("admin_setre"))
			{
				armorType = Inventory.PAPERDOLL_REAR;
			}
			else if (command.startsWith("admin_setlf"))
			{
				armorType = Inventory.PAPERDOLL_LFINGER;
			}
			else if (command.startsWith("admin_setrf"))
			{
				armorType = Inventory.PAPERDOLL_RFINGER;
			}
			else if (command.startsWith("admin_seten"))
			{
				armorType = Inventory.PAPERDOLL_NECK;
			}
			else if (command.startsWith("admin_setun"))
			{
				armorType = Inventory.PAPERDOLL_UNDER;
			}
			else if (command.startsWith("admin_setba"))
			{
				armorType = Inventory.PAPERDOLL_CLOAK;
			}
			else if (command.startsWith("admin_setbe"))
			{
				armorType = Inventory.PAPERDOLL_BELT;
			}

			if (armorType != -1)
			{
				try
				{
					final int ench = Integer.parseInt(command.substring(12));
					
					if (ench < 0 || ench > 65535)
					{
						activeChar.sendMessage("You must set the enchant level to be between 0-65535.");
					}
					else
					{
						setEnchant(activeChar, ench, armorType);
					}
				}
				catch (final StringIndexOutOfBoundsException e)
				{
					if (Config.DEVELOPER)
					{
						_log.warn("Set enchant error: " + e);
					}
					activeChar.sendMessage("Please specify a new enchant value.");
				}
				catch (final NumberFormatException e)
				{
					if (Config.DEVELOPER)
					{
						_log.warn("Set enchant error: " + e);
					}
					activeChar.sendMessage("Please specify a valid new enchant value.");
				}
			}
			showMainPage(activeChar);
		}
		return true;
	}

	private void setEnchant(Player activeChar, int ench, int armorType)
	{
		GameObject target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}
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

		int curEnchant = 0;
		ItemInstance itemInstance = null;

		final ItemInstance parmorInstance = player.getInventory().getPaperdollItem(armorType);
		if (parmorInstance != null && parmorInstance.getLocationSlot() == armorType)
		{
			itemInstance = parmorInstance;
		}

		if (itemInstance != null)
		{
			curEnchant = itemInstance.getEnchantLevel();
			
			player.getInventory().unEquipItemInSlot(armorType);
			itemInstance.setEnchantLevel(ench);
			player.getInventory().equipItem(itemInstance);
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(itemInstance);
			player.sendPacket(iu);
			player.broadcastCharInfo();
			
			activeChar.sendMessage("Changed enchantment of " + player.getName(null) + "'s " + itemInstance.getItem().getName(activeChar.getLang()) + " from " + curEnchant + " to " + ench + ".");
			player.sendMessage("Admin has changed the enchantment of your " + itemInstance.getItem().getName(player.getLang()) + " from " + curEnchant + " to " + ench + ".");
		}
	}

	private void showMainPage(Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/enchant.htm");
		activeChar.sendPacket(adminhtm);
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}