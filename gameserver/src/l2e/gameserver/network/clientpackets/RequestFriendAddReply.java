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

import java.sql.Connection;
import java.sql.PreparedStatement;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.FriendAddRequestResult;
import l2e.gameserver.network.serverpackets.L2Friend;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestFriendAddReply extends GameClientPacket
{
	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player != null)
		{
			final Player requestor = player.getActiveRequester();
			if (requestor == null)
			{
				return;
			}

			if (_response == 1)
			{
				Connection con = null;
				PreparedStatement statement = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement("INSERT INTO character_friends (charId, friendId) VALUES (?, ?), (?, ?)");
					statement.setInt(1, requestor.getObjectId());
					statement.setInt(2, player.getObjectId());
					statement.setInt(3, player.getObjectId());
					statement.setInt(4, requestor.getObjectId());
					statement.execute();
					SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND);
					requestor.sendPacket(msg);

					msg = SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED_TO_FRIENDS);
					msg.addString(player.getName(null));
					requestor.sendPacket(msg);
					requestor.getFriendList().add(player.getObjectId());

					msg = SystemMessage.getSystemMessage(SystemMessageId.S1_JOINED_AS_FRIEND);
					msg.addString(requestor.getName(null));
					player.sendPacket(msg);
					player.getFriendList().add(requestor.getObjectId());

					player.sendPacket(new L2Friend(true, requestor.getObjectId()));
					requestor.sendPacket(new L2Friend(true, player.getObjectId()));
				}
				catch (final Exception e)
				{
					_log.warn("Could not add friend objectid: " + e.getMessage(), e);
				}
				finally
				{
					DbUtils.closeQuietly(con, statement);
				}
			}
			else
			{
				requestor.broadcastPacket(FriendAddRequestResult.STATIC_PACKET);
			}
			player.setActiveRequester(null);
			requestor.onTransactionResponse();
		}
	}
}