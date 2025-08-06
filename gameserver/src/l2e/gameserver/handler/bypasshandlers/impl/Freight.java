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
package l2e.gameserver.handler.bypasshandlers.impl;

import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.itemcontainer.PcFreight;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.PackageToList;
import l2e.gameserver.network.serverpackets.WareHouseWithdrawList;

public class Freight implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "package_withdraw", "package_deposit"
	};

	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		if (command.equalsIgnoreCase(COMMANDS[0]))
		{
			final PcFreight freight = activeChar.getFreight();
			if (freight != null)
			{
				if (freight.getSize() > 0)
				{
					activeChar.setActiveWarehouse(freight);
					activeChar.sendPacket(new WareHouseWithdrawList(1, activeChar, WareHouseWithdrawList.FREIGHT));
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
				}
			}
		}
		else if (command.equalsIgnoreCase(COMMANDS[1]))
		{
			if (activeChar.getAccountChars().size() < 1)
			{
				activeChar.sendPacket(SystemMessageId.CHARACTER_DOES_NOT_EXIST);
			}
			else
			{
				activeChar.sendPacket(new PackageToList(activeChar.getAccountChars()));
			}
		}
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}