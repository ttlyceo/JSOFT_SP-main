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
package l2e.gameserver.model;

import java.util.Iterator;

import l2e.commons.collections.EmptyIterator;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public interface PlayerGroup extends Iterable<Player>
{
	public static final PlayerGroup EMPTY = new PlayerGroup()
	{
		@Override
		public void broadCast(GameServerPacket... packet)
		{
		}
		
		@Override
		public void broadCastMessage(ServerMessage msg)
		{
		}
		
		@Override
		public int getMemberCount()
		{
			return 0;
		}

		@Override
		public Player getGroupLeader()
		{
			return null;
		}

		@Override
		public Iterator<Player> iterator()
		{
			return EmptyIterator.getInstance();
		}
	};

	void broadCast(GameServerPacket... packet);
	
	void broadCastMessage(ServerMessage msg);
	
	int getMemberCount();
	
	Player getGroupLeader();
}
