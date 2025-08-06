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

public class CombineTalismans implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
	        "combinetalismans", "combine"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String args)
	{
		if (activeChar != null)
		{
			activeChar.checkCombineTalisman(null, true);
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}