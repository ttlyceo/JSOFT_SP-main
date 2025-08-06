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

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NetPing;

public class Ping implements IVoicedCommandHandler
{
	private static String[] _voicedCommands =
	{
	        "ping"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equalsIgnoreCase("ping"))
		{
			activeChar.sendMessage("Processing request...");
			activeChar.sendPacket(new NetPing(activeChar));
			ThreadPoolManager.getInstance().schedule(new AnswerTask(activeChar), 3000L);
		}
		return true;
	}
	
	protected final class AnswerTask implements Runnable
	{
		private final Player _player;
		
		public AnswerTask(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			final int ping = _player.getPing();
			if (ping != -1)
			{
				_player.sendMessage("Current ping: " + ping + " ms.");
			}
			else
			{
				_player.sendMessage("The data from the client was not received.");
			}
		}
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}