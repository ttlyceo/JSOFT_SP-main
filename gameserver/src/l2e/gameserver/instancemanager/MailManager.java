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
package l2e.gameserver.instancemanager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.tasks.MessageDeletionTask;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Message;
import l2e.gameserver.network.serverpackets.ExNoticePostArrived;

public final class MailManager extends LoggerObject
{
	private final Map<Integer, Message> _messages = new ConcurrentHashMap<>();
	
	protected MailManager()
	{
		load();
	}

	private void load()
	{
		int count = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM messages ORDER BY expiration");
			
			rset = statement.executeQuery();
			while (rset.next())
			{
				
				final var msg = new Message(rset);
				final int msgId = msg.getId();
				_messages.put(msgId, msg);
				
				count++;
				
				final long expiration = msg.getExpiration();
				
				if (expiration < System.currentTimeMillis())
				{
					ThreadPoolManager.getInstance().schedule(new MessageDeletionTask(msgId), 10000);
				}
				else
				{
					ThreadPoolManager.getInstance().schedule(new MessageDeletionTask(msgId), expiration - System.currentTimeMillis());
				}
			}
		}
		catch (final SQLException e)
		{
			warn("Error loading from database:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		info("Successfully loaded " + count + " messages.");
	}
	
	public final Message getMessage(int msgId)
	{
		return _messages.get(msgId);
	}
	
	public final Collection<Message> getMessages()
	{
		return _messages.values();
	}
	
	public final boolean hasUnreadPost(Player player)
	{
		final int objectId = player.getObjectId();
		for (final var msg : getMessages())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && msg.isUnread())
			{
				return true;
			}
		}
		return false;
	}
	
	public final int getInboxSize(int objectId)
	{
		int size = 0;
		for (final var msg : getMessages())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && !msg.isDeletedByReceiver())
			{
				size++;
			}
		}
		return size;
	}
	
	public final int getOutboxSize(int objectId)
	{
		int size = 0;
		for (final var msg : getMessages())
		{
			if ((msg != null) && (msg.getSenderId() == objectId) && !msg.isDeletedBySender())
			{
				size++;
			}
		}
		return size;
	}
	
	public final List<Message> getInbox(int objectId)
	{
		final List<Message> inbox = new ArrayList<>();
		for (final var msg : getMessages())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && !msg.isDeletedByReceiver())
			{
				inbox.add(msg);
			}
		}
		
		if (!inbox.isEmpty() && inbox.size() > 1)
		{
			final var statsComparator = new SortMail(false);
			Collections.sort(inbox, statsComparator);
		}
		return inbox;
	}
	
	public final List<Message> getOutbox(int objectId)
	{
		final List<Message> outbox = new ArrayList<>();
		for (final var msg : getMessages())
		{
			if ((msg != null) && (msg.getSenderId() == objectId) && !msg.isDeletedBySender())
			{
				outbox.add(msg);
			}
		}
		
		if (!outbox.isEmpty() && outbox.size() > 1)
		{
			final var statsComparator = new SortMail(true);
			Collections.sort(outbox, statsComparator);
		}
		return outbox;
	}
	
	private static class SortMail implements Comparator<Message>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		private final boolean _isSent;
		
		private SortMail(boolean isSent)
		{
			_isSent = isSent;
		}
		
		@Override
		public int compare(Message o1, Message o2)
		{
			return _isSent ? Integer.compare(o1.getExpirationSeconds(), o2.getExpirationSeconds()) : Integer.compare(o2.getExpirationSeconds(), o1.getExpirationSeconds());
		}
	}
	
	public void sendMessage(Message msg)
	{
		_messages.put(msg.getId(), msg);
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = Message.getStatement(msg, con);
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Error saving message:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		final var receiver = GameObjectsStorage.getPlayer(msg.getReceiverId());
		if (receiver != null)
		{
			receiver.sendPacket(ExNoticePostArrived.valueOf(true));
		}
		ThreadPoolManager.getInstance().schedule(new MessageDeletionTask(msg.getId()), msg.getExpiration() - System.currentTimeMillis());
	}
	
	public final void markAsReadInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE messages SET isUnread = '0' WHERE messageId = ?");
			statement.setInt(1, msgId);
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Error marking as read message:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final void markAsDeletedBySenderInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE messages SET isDeletedBySender = '1' WHERE messageId = ?");
			statement.setInt(1, msgId);
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Error marking as deleted by sender message:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final void markAsDeletedByReceiverInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE messages SET isDeletedByReceiver = '1' WHERE messageId = ?");
			statement.setInt(1, msgId);
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Error marking as deleted by receiver message:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final void removeAttachmentsInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE messages SET hasAttachments = '0' WHERE messageId = ?");
			statement.setInt(1, msgId);
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Error removing attachments in message:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final void deleteMessageInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM messages WHERE messageId = ?");
			statement.setInt(1, msgId);
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Error deleting message:" + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		_messages.remove(msgId);
		IdFactory.getInstance().releaseId(msgId);
	}
	
	public static MailManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final MailManager _instance = new MailManager();
	}
}