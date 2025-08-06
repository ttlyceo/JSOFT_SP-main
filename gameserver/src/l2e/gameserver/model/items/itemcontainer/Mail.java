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
package l2e.gameserver.model.items.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;

public class Mail extends ItemContainer
{
	private final int _ownerId;
	private int _messageId;
	
	public Mail(int objectId, int messageId)
	{
		_ownerId = objectId;
		_messageId = messageId;
	}
	
	@Override
	public String getName()
	{
		return "Mail";
	}

	@Override
	public Player getOwner()
	{
		return null;
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.MAIL;
	}

	public int getMessageId()
	{
		return _messageId;
	}

	public void setNewMessageId(int messageId)
	{
		_messageId = messageId;
		for (final ItemInstance item : _items)
		{
			if (item == null)
			{
				continue;
			}

			item.setItemLocation(getBaseLocation(), messageId);
		}

		updateDatabase();
	}

	@Override
	protected void addItem(ItemInstance item)
	{
		super.addItem(item);
		item.setItemLocation(getBaseLocation(), _messageId);
	}
	
	@Override
	public void updateDatabase()
	{
		for (final ItemInstance item : _items)
		{
			if (item != null)
			{
				item.updateDatabase(true);
			}
		}
	}

	@Override
	public void restore()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet inv = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time, visual_itemId, agathion_energy, is_event FROM items WHERE owner_id=? AND loc=? AND loc_data=?");
			statement.setInt(1, getOwnerId());
			statement.setString(2, getBaseLocation().name());
			statement.setInt(3, getMessageId());
			inv = statement.executeQuery();

			ItemInstance item;
			while (inv.next())
			{
				item = ItemInstance.restoreFromDb(getOwnerId(), inv);
				if (item == null)
				{
					continue;
				}
				GameObjectsStorage.addItem(item);
				
				if (item.isStackable() && getItemByItemId(item.getId()) != null)
				{
					addItem("Restore", item, null, null);
				}
				else
				{
					addItem(item);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("could not restore container:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, inv);
		}
	}

	@Override
	public int getOwnerId()
	{
		return _ownerId;
	}
}