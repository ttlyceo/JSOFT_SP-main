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
package l2e.gameserver;

import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter 11.02.2011
 */
public class OnlinePlayers
{
	private class AnnounceOnline implements Runnable
	{
		@Override
		public void run()
		{
			if (Config.ONLINE_PLAYERS_AT_STARTUP)
			{
				final ServerMessage msg = new ServerMessage("OnlinePlayers.ONLINE_ANNOUNCE", true);
				msg.add((GameObjectsStorage.getAllPlayersCount() * Config.FAKE_ONLINE) + Config.FAKE_ONLINE_MULTIPLIER);
				Announcements.getInstance().announceToAll(msg);
				ThreadPoolManager.getInstance().schedule(new AnnounceOnline(), Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL);
			}
		}
	}

	private OnlinePlayers()
	{
		ThreadPoolManager.getInstance().schedule(new AnnounceOnline(), Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL);
	}
	
	public static final OnlinePlayers getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final OnlinePlayers _instance = new OnlinePlayers();
	}
}