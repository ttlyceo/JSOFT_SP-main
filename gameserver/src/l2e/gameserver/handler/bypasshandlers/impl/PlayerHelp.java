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

import java.util.StringTokenizer;

import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class PlayerHelp implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "player_help"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		try
		{
			if (command.length() < 13)
			{
				return false;
			}

			final String path = command.substring(12);
			if (path.indexOf("..") != -1)
			{
				return false;
			}

			final StringTokenizer st = new StringTokenizer(path);
			final String[] cmd = st.nextToken().split("#");

			NpcHtmlMessage html;
			if (cmd.length > 1)
			{
				final int itemId = Integer.parseInt(cmd[1]);
				html = new NpcHtmlMessage(1, itemId);
			}
			else
			{
				html = new NpcHtmlMessage(1);
			}

			html.setFile(activeChar, activeChar.getLang(), "data/html/help/" + cmd[0]);
			activeChar.sendPacket(html);
		}
		catch (final Exception e)
		{
			_log.warn("Exception in " + getClass().getSimpleName(), e);
		}
		return true;
	}

	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}