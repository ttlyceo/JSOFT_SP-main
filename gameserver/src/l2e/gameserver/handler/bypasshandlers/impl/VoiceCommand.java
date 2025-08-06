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
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;

public class VoiceCommand implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "voice"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if ((command.length() > 7) && (command.charAt(6) == '.'))
		{
			final String vc, vparams;
			final int endOfCommand = command.indexOf(" ", 7);
			if (endOfCommand > 0)
			{
				vc = command.substring(7, endOfCommand).trim();
				vparams = command.substring(endOfCommand).trim();
			}
			else
			{
				vc = command.substring(7).trim();
				vparams = null;
			}
			
			if (vc.length() > 0)
			{
				final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler(vc);
				if (vch != null)
				{
					return vch.useVoicedCommand(vc, activeChar, vparams);
				}
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