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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerStorage;

public class Lang implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "lang"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!Config.MULTILANG_VOICED_ALLOW)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("lang") && target != null)
		{
			if (!Config.MULTILANG_ALLOWED.contains(target))
			{
				String answer = "" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.WRONG_LANG") + "";
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					answer += " " + lang;
				}
				activeChar.sendMessage(answer);
				return false;
			}
			activeChar.setLang(target);
			if (target.equalsIgnoreCase("en"))
			{
				activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.EN_LANG") + "");
			}
			else if (target.equalsIgnoreCase("ru"))
			{
				activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.RU_LANG") + "");
			}
			activeChar.updateNpcNames();
		}
		else if (command.startsWith("lang"))
		{
			final String[] params = command.split(" ");
			if (params.length == 2)
			{
				final String lng = params[1];
				if (!Config.MULTILANG_ALLOWED.contains(lng))
				{
					String answer = "" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.WRONG_LANG") + "";
					for (final String lang : Config.MULTILANG_ALLOWED)
					{
						answer += " " + lang;
					}
					activeChar.sendMessage(answer);
					return false;
				}
				
				activeChar.setLang(lng);
				if (lng.equalsIgnoreCase("en"))
				{
					activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.EN_LANG") + "");
				}
				else if (lng.equalsIgnoreCase("ru"))
				{
					activeChar.sendMessage("" + ServerStorage.getInstance().getString(activeChar.getLang(), "Lang.RU_LANG") + "");
				}
				activeChar.updateNpcNames();
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