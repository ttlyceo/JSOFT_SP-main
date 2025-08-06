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
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.FriendRemove;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestFriendDel extends GameClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		SystemMessage sm;
		
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final int id = CharNameHolder.getInstance().getIdByName(_name);
		
		if (id == -1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_NOT_ON_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			return;
		}
		
		if (!activeChar.getFriendList().contains(id))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_NOT_ON_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_friends WHERE (charId=? AND friendId=?) OR (charId=? AND friendId=?)");
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, id);
			statement.setInt(4, activeChar.getObjectId());
			statement.execute();
			
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_REMOVED_FROM_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			activeChar.getFriendList().remove(Integer.valueOf(id));
			activeChar.sendPacket(new FriendRemove(_name));
			
			final Player player = GameObjectsStorage.getPlayer(_name);
			if (player != null)
			{
				player.getFriendList().remove(Integer.valueOf(activeChar.getObjectId()));
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST).addString(activeChar.getName(null)));
				player.sendPacket(new FriendRemove(activeChar.getName(null)));
			}
		}
		catch (final Exception e)
		{
			_log.warn("could not del friend objectid: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}