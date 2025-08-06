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
package l2e.gameserver.handler.communityhandlers.impl;

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.Dummy_7D;
import l2e.gameserver.network.serverpackets.Dummy_7D.ServerRequest;
import l2e.gameserver.network.serverpackets.Dummy_8D;

/**
 * Created by LordWinter 05.07.2013 Fixed by L2J Eternity-World
 */
public class CommunityLink extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityLink()
	{
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}
	
	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbslink", "_bbsurl", "_bbsopenurl"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player activeChar)
	{
		if (!checkCondition(activeChar, new StringTokenizer(command, "_").nextToken(), false, false))
		{
			return;
		}
		
		if (command.equalsIgnoreCase("_bbslink"))
		{
			sendHtm(activeChar, "data/html/community/homepage.htm");
		}
		else if (command.startsWith("_bbsurl"))
		{
			final StringTokenizer st = new StringTokenizer(command, ":");
			st.nextToken();
			String url = null;
			try
			{
				url = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (url != null)
			{
				activeChar.sendPacket(new Dummy_7D(url, ServerRequest.SC_SERVER_REQUEST_OPEN_URL));
			}
		}
		else if (command.startsWith("_bbsopenurl"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			String url = null;
			try
			{
				url = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (url != null)
			{
				activeChar.sendPacket(new Dummy_8D(url));
			}
		}
	}
	
	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}
	
	public static CommunityLink getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityLink _instance = new CommunityLink();
	}
}