/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2e.gameserver.handler.admincommandhandlers.impl;

import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;

public class Element implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_setlh", "admin_setlc", "admin_setll", "admin_setlg", "admin_setlb", "admin_setlw", "admin_setls"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		int armorType = -1;
		
		if (command.startsWith("admin_setlh"))
		{
			armorType = Inventory.PAPERDOLL_HEAD;
		}
		else if (command.startsWith("admin_setlc"))
		{
			armorType = Inventory.PAPERDOLL_CHEST;
		}
		else if (command.startsWith("admin_setlg"))
		{
			armorType = Inventory.PAPERDOLL_GLOVES;
		}
		else if (command.startsWith("admin_setlb"))
		{
			armorType = Inventory.PAPERDOLL_FEET;
		}
		else if (command.startsWith("admin_setll"))
		{
			armorType = Inventory.PAPERDOLL_LEGS;
		}
		else if (command.startsWith("admin_setlw"))
		{
			armorType = Inventory.PAPERDOLL_RHAND;
		}
		else if (command.startsWith("admin_setls"))
		{
			armorType = Inventory.PAPERDOLL_LHAND;
		}
		
		if (armorType != -1)
		{
			try
			{
				final String[] args = command.split(" ");
				
				final byte element = Elementals.getElementId(args[1]);
				final int value = Integer.parseInt(args[2]);
				if (element < -1 || element > 5 || value < 0 || value > 450)
				{
					activeChar.sendMessage("Usage: //setlh/setlc/setlg/setlb/setll/setlw/setls <element> <value>[0-450]");
					return false;
				}
				
				setElement(activeChar, element, value, armorType);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //setlh/setlc/setlg/setlb/setll/setlw/setls <element>[0-5] <value>[0-450]");
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void setElement(Player activeChar, byte type, int value, int armorType)
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
		
		ItemInstance itemInstance = null;

		final ItemInstance parmorInstance = player.getInventory().getPaperdollItem(armorType);
		if (parmorInstance != null && parmorInstance.getLocationSlot() == armorType)
		{
			itemInstance = parmorInstance;
		}
		
		if (itemInstance != null)
		{
			String old, current;
			final Elementals element = itemInstance.getElemental(type);
			if (element == null)
			{
				old = "None";
			}
			else
			{
				old = element.toString();
			}

			player.getInventory().unEquipItemInSlot(armorType);
			if (type == -1)
			{
				itemInstance.clearElementAttr(type);
			}
			else
			{
				itemInstance.setElementAttr(type, value);
			}
			player.getInventory().equipItem(itemInstance);
			
			if (itemInstance.getElementals() == null)
			{
				current = "None";
			}
			else
			{
				current = itemInstance.getElemental(type).toString();
			}

			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(itemInstance);
			player.sendPacket(iu);

			activeChar.sendMessage("Changed elemental power of " + player.getName(null) + "'s " + itemInstance.getItem().getName(activeChar.getLang()) + " from " + old + " to " + current + ".");
			if (player != activeChar)
			{
				player.sendMessage(activeChar.getName(null) + " has changed the elemental power of your " + itemInstance.getItem().getName(player.getLang()) + " from " + old + " to " + current + ".");
			}
		}
	}
}