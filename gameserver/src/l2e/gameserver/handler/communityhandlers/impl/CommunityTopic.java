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

/**
 * Rework by LordWinter 10.07.2013 Fixed by L2J Eternity-World
 */
public class CommunityTopic extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityTopic()
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
		        "_bbsmemo", "_bbstopics"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player activeChar)
	{
		if (!checkCondition(activeChar, new StringTokenizer(command, "_").nextToken(), false, false))
		{
			return;
		}
		
		if (command.equals("_bbsmemo") || command.equals("_bbstopics"))
		{
			sendHtm(activeChar, "data/html/community/topic.htm");
		}
	}

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}

	private static class SingletonHolder
	{
		protected static final CommunityTopic _instance = new CommunityTopic();
	}
	
	public static CommunityTopic getInstance()
	{
		return SingletonHolder._instance;
	}
}