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

import l2e.gameserver.Config;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.listener.player.impl.OfflineAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;

public class Offline implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
	        "offline"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!Config.ALLOW_OFFLINE_COMMAND)
		{
			return false;
		}
		
		if (!activeChar.canOfflineMode(activeChar, true))
		{
			activeChar.sendMessage((new ServerMessage("Community.ALL_DISABLE", activeChar.getLang())).toString());
			return false;
		}
		activeChar.sendConfirmDlg(new OfflineAnswerListener(activeChar), 15000, new ServerMessage("Offline.CHOCICE", activeChar.getLang()).toString());
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}