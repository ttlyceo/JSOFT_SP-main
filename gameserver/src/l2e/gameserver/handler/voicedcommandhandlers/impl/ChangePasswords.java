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

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.gameserverpackets.ChangePassword;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class ChangePasswords implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "changepassword"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!Config.ALLOW_CHANGE_PASSWORD)
		{
			return false;
		}
		
		if (target != null && !(target.isEmpty()))
		{
			final StringTokenizer st = new StringTokenizer(target);
			try
			{
				String curpass = null, newpass = null, repeatnewpass = null;
				if (st.hasMoreTokens())
				{
					curpass = st.nextToken();
				}
				if (st.hasMoreTokens())
				{
					newpass = st.nextToken();
				}
				if (st.hasMoreTokens())
				{
					repeatnewpass = st.nextToken();
				}

				if (!((curpass == null) || (newpass == null) || (repeatnewpass == null)))
				{
					if (!activeChar.checkFloodProtection("CHANGEPASSWORD", "password_delay"))
					{
						return false;
					}
					
					if (!newpass.equals(repeatnewpass))
					{
						activeChar.sendMessage("The new password doesn't match with the repeated one!");
						return false;
					}
					if (newpass.length() < 3)
					{
						activeChar.sendMessage("The new password is shorter than 3 chars! Please try with a longer one.");
						return false;
					}
					if (newpass.length() > 30)
					{
						activeChar.sendMessage("The new password is longer than 30 chars! Please try with a shorter one.");
						return false;
					}
					AuthServerCommunication.getInstance().sendPacket(new ChangePassword(activeChar.getAccountName(), curpass, newpass, "0"));
				}
				else
				{
					activeChar.sendMessage("Invalid password data! You have to fill all boxes.");
					return false;
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("A problem occured while changing password!");
				_log.warn("", e);
			}
		}
		else
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
			html.setFile(activeChar, activeChar.getLang(), "data/html/mods/ChangePassword.htm");
			activeChar.sendPacket(html);
			return true;
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}