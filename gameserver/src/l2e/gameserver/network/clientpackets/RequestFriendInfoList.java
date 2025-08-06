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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestFriendInfoList extends GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();

		if (activeChar == null)
		{
			return;
		}
		
		SystemMessage sm;

		activeChar.sendPacket(SystemMessageId.FRIEND_LIST_HEADER);

		Player friend = null;
		for (final int id : activeChar.getFriendList())
		{
			final String friendName = CharNameHolder.getInstance().getNameById(id);

			if (friendName == null)
			{
				continue;
			}

			friend = GameObjectsStorage.getPlayer(friendName);
			if ((friend == null) || !friend.isOnline())
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_OFFLINE);
				sm.addString(friendName);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ONLINE);
				sm.addString(friendName);
			}

			activeChar.sendPacket(sm);
		}
		activeChar.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
	}
}