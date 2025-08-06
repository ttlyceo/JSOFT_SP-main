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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;

public class ShiftClick implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "shiftclick"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equalsIgnoreCase("shiftclick"))
		{
			if (activeChar.getBlockShiftClick())
			{
				activeChar.setVar("shiftclick@", "0");
				activeChar.sendMessage((new ServerMessage("ShiftClick.SHIFT_DISABLED", activeChar.getLang())).toString());
			}
			else
			{
				activeChar.setVar("shiftclick@", "1");
				activeChar.sendMessage((new ServerMessage("ShiftClick.SHIFT_ENABLED", activeChar.getLang())).toString());
			}
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}