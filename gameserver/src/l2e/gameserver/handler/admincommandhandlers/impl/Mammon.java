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

import l2e.gameserver.SevenSigns;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.AutoSpawnHandler;
import l2e.gameserver.model.AutoSpawnHandler.AutoSpawnInstance;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public class Mammon implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_mammon_find", "admin_mammon_respawn",
	};

	private final boolean _isSealValidation = SevenSigns.getInstance().isSealValidationPeriod();
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		int teleportIndex = -1;
		final AutoSpawnInstance blackSpawnInst = AutoSpawnHandler.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_BLACKSMITH_ID, false);
		final AutoSpawnInstance merchSpawnInst = AutoSpawnHandler.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_MERCHANT_ID, false);
		
		if (command.startsWith("admin_mammon_find"))
		{
			try
			{
				if (command.length() > 17)
				{
					teleportIndex = Integer.parseInt(command.substring(18));
				}
			}
			catch (final Exception NumberFormatException)
			{
				activeChar.sendMessage("Usage: //mammon_find [teleportIndex] (where 1 = Blacksmith, 2 = Merchant)");
				return false;
			}
			
			if (!_isSealValidation)
			{
				activeChar.sendPacket(SystemMessageId.SSQ_COMPETITION_UNDERWAY);
				return false;
			}
			
			if (blackSpawnInst != null)
			{
				final Npc blackInst = blackSpawnInst.getNPCInstanceList().peek();
				if (blackInst != null)
				{
					activeChar.sendMessage("Blacksmith of Mammon: " + blackInst.getX() + " " + blackInst.getY() + " " + blackInst.getZ());
					if (teleportIndex == 1)
					{
						activeChar.teleToLocation(blackInst.getLocation(), true, activeChar.getReflection());
					}
				}
			}
			else
			{
				activeChar.sendMessage("Blacksmith of Mammon isn't registered for spawn.");
			}
			
			if (merchSpawnInst != null)
			{
				final Npc merchInst = merchSpawnInst.getNPCInstanceList().peek();
				if (merchInst != null)
				{
					activeChar.sendMessage("Merchant of Mammon: " + merchInst.getX() + " " + merchInst.getY() + " " + merchInst.getZ());
					if (teleportIndex == 2)
					{
						activeChar.teleToLocation(merchInst.getLocation(), true, activeChar.getReflection());
					}
				}
			}
			else
			{
				activeChar.sendMessage("Merchant of Mammon isn't registered for spawn.");
			}
		}
		else if (command.startsWith("admin_mammon_respawn"))
		{
			if (!_isSealValidation)
			{
				activeChar.sendPacket(SystemMessageId.SSQ_COMPETITION_UNDERWAY);
				return false;
			}
			
			if (merchSpawnInst != null)
			{
				final long merchRespawn = AutoSpawnHandler.getInstance().getTimeToNextSpawn(merchSpawnInst);
				activeChar.sendMessage("The Merchant of Mammon will respawn in " + (merchRespawn / 60000) + " minute(s).");
			}
			else
			{
				activeChar.sendMessage("Merchant of Mammon isn't registered for spawn.");
			}
			
			if (blackSpawnInst != null)
			{
				final long blackRespawn = AutoSpawnHandler.getInstance().getTimeToNextSpawn(blackSpawnInst);
				activeChar.sendMessage("The Blacksmith of Mammon will respawn in " + (blackRespawn / 60000) + " minute(s).");
			}
			else
			{
				activeChar.sendMessage("Blacksmith of Mammon isn't registered for spawn.");
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}