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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class ContactList
{
	private final Logger _log = LoggerFactory.getLogger(getClass());
	private final Player activeChar;
	private final List<String> _contacts = new CopyOnWriteArrayList<>();
	
	private final String QUERY_ADD = "INSERT INTO character_contacts (charId, contactId) VALUES (?, ?)";
	private final String QUERY_REMOVE = "DELETE FROM character_contacts WHERE charId = ? and contactId = ?";
	private final String QUERY_LOAD = "SELECT contactId FROM character_contacts WHERE charId = ?";

	public ContactList(Player player)
	{
		activeChar = player;
		restore();
	}
	
	public void restore()
	{
		_contacts.clear();
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(QUERY_LOAD);
			statement.setInt(1, activeChar.getObjectId());
			rset = statement.executeQuery();
			
			int contactId;
			String contactName;
			
			while (rset.next())
			{
				contactId = rset.getInt(1);
				contactName = CharNameHolder.getInstance().getNameById(contactId);
				if ((contactName == null) || contactName.equals(activeChar.getName(null)) || (contactId == activeChar.getObjectId()))
				{
					continue;
				}
				_contacts.add(contactName);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error found in " + activeChar.getName(null) + "'s ContactsList: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public boolean add(String name)
	{
		SystemMessage sm;
		
		final int contactId = CharNameHolder.getInstance().getIdByName(name);
		if (_contacts.contains(name))
		{
			activeChar.sendPacket(SystemMessageId.NAME_ALREADY_EXIST_ON_CONTACT_LIST);
			return false;
		}
		else if (activeChar.getName(null).equals(name))
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_ADD_YOUR_NAME_ON_CONTACT_LIST);
			return false;
		}
		else if (_contacts.size() >= 100)
		{
			activeChar.sendPacket(SystemMessageId.CONTACT_LIST_LIMIT_REACHED);
			return false;
		}
		else if (contactId < 1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.NAME_S1_NOT_EXIST_TRY_ANOTHER_NAME);
			sm.addString(name);
			activeChar.sendPacket(sm);
			return false;
		}
		else
		{
			for (final String contactName : _contacts)
			{
				if (contactName.equalsIgnoreCase(name))
				{
					activeChar.sendPacket(SystemMessageId.NAME_ALREADY_EXIST_ON_CONTACT_LIST);
					return false;
				}
			}
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(QUERY_ADD);
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			_contacts.add(name);
			
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SUCCESSFULLY_ADDED_TO_CONTACT_LIST);
			sm.addString(name);
			activeChar.sendPacket(sm);
		}
		catch (final Exception e)
		{
			_log.warn("Error found in " + activeChar.getName(null) + "'s ContactsList: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}
	
	public void remove(String name)
	{
		final int contactId = CharNameHolder.getInstance().getIdByName(name);
		
		if (!_contacts.contains(name))
		{
			activeChar.sendPacket(SystemMessageId.NAME_NOT_REGISTERED_ON_CONTACT_LIST);
			return;
		}
		
		else if (contactId < 1)
		{
			return;
		}
		
		_contacts.remove(name);
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(QUERY_REMOVE);
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SUCCESFULLY_DELETED_FROM_CONTACT_LIST);
			sm.addString(name);
			activeChar.sendPacket(sm);
		}
		catch (final Exception e)
		{
			_log.warn("Error found in " + activeChar.getName(null) + "'s ContactsList: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public List<String> getAllContacts()
	{
		return _contacts;
	}
}