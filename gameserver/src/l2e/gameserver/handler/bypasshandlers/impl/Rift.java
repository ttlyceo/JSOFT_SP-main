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
import l2e.gameserver.instancemanager.DimensionalRiftManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;

public class Rift implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "enterrift", "changeriftroom", "exitrift"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		if (command.toLowerCase().startsWith(COMMANDS[0]))
		{
			try
			{
				final Byte b1 = Byte.parseByte(command.substring(10));
				DimensionalRiftManager.getInstance().start(activeChar, b1, (Npc) target);
				return true;
			}
			catch (final Exception e)
			{
			}
		}
		else
		{
			final var rift = activeChar.isInParty() ? activeChar.getParty().getDimensionalRift() : null;
			if (command.toLowerCase().startsWith(COMMANDS[1])) // ChangeRiftRoom
			{
				if (rift != null)
				{
					if (activeChar.getObjectId() != activeChar.getParty().getLeaderObjectId())
					{
						DimensionalRiftManager.getInstance().showHtmlFile(activeChar, "data/html/seven_signs/rift/NotPartyLeader.htm", (Npc) target);
						return false;
					}
					
					if (rift.isJumped())
					{
						DimensionalRiftManager.getInstance().showHtmlFile(activeChar, "data/html/seven_signs/rift/AlreadyTeleported.htm", (Npc) target);
						return false;
					}
					activeChar.getParty().getDimensionalRift().manualTeleport();
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(activeChar, (Npc) target);
				}
				return true;
			}
			else if (command.toLowerCase().startsWith(COMMANDS[2])) // ExitRift
			{
				if (rift != null)
				{
					if (activeChar.getObjectId() != activeChar.getParty().getLeaderObjectId())
					{
						DimensionalRiftManager.getInstance().showHtmlFile(activeChar, "data/html/seven_signs/rift/NotPartyLeader.htm", (Npc) target);
						return false;
					}
					rift.manualExit();
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(activeChar, (Npc) target);
				}
				return true;
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