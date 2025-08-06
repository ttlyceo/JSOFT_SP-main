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
package l2e.gameserver.model.items;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.items.itemcontainer.ItemContainer;

public class ItemAuction extends ItemContainer
{
	private static final Logger _log = LoggerFactory.getLogger(ItemAuction.class);
	
	protected ItemAuction()
	{
		restore();
	}

	public void deleteItemFromList(ItemInstance item)
	{
		_items.remove(item);
	}
	
	@Override
	public String getName()
	{
		return "Auction";
	}

	@Override
	public Player getOwner()
	{
		return null;
	}
	
	@Override
	public ItemInstance addItem(String process, ItemInstance item, Player owner, Object reference)
	{
		if (item == null)
		{
			return null;
		}

		if (item.getCount() < 1)
		{
			return null;
		}

		ItemInstance result = null;

		try
		{
			_items.add(item);
			result = item;
			item.setLastChange(ItemInstance.ADDED);
			addItem(result);
		}
		finally
		{}
		return result;
	}
	
	public ItemInstance addFullItem(ItemInstance item)
	{
		if (item == null)
		{
			return null;
		}

		if (item.getCount() < 1)
		{
			return null;
		}

		ItemInstance result = null;

		try
		{
			_items.add(item);
			result = item;
			item.setLastChange(ItemInstance.MODIFIED);
			addItem(result);
		}
		finally
		{}
		return result;
	}

	@Override
	public void addItem(ItemInstance item)
	{
		item.setItemLocation(getBaseLocation());
		item.updateDatabase(true);
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.AUCTION;
	}
	
	@Override
	public ItemInstance getItemByObjectId(int objectId)
	{
		ItemInstance item;
		for (int i = 0; i < _items.size(); i++)
		{
			item = _items.get(i);
			if (item.getObjectId() == objectId)
			{
				return item;
			}
		}
		return null;
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
	
	public void updateItem(int objectId, int newOwnerId)
	{
		try
		{
			ItemInstance item;
			if ((item = getItemByObjectId(objectId)) == null)
			{
				_log.warn("item is null in auction storage, obj id:" + objectId);
				return;
			}

			synchronized (item)
			{
				item.setOwnerId(newOwnerId);
				item.setItemLocation(ItemLocation.INVENTORY);
				item.setLastChange((ItemInstance.MODIFIED));
				item.updateDatabase();
				deleteItemFromList(item);
			}
			
		}
		finally
		{}
	}
	
	public void changeCount(ItemInstance item, long count)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE items SET count=? WHERE object_id = ?");
			statement.setLong(1, count);
			statement.setInt(2, item.getObjectId());
			statement.executeUpdate();

			deleteItemFromList(item);
			synchronized (item)
			{
				item.setCount(count);
				item.setLastChange(ItemInstance.MODIFIED);
				item.updateDatabase();
				_items.add(item);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not update item " + this + " in DB: Reason: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		item.updateDatabase(true);
	}

	public void removeItemFromDb(int objectId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM items WHERE object_id = ?");
			statement.setInt(1, objectId);
			statement.executeUpdate();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
			statement.setInt(1, objectId);
			statement.executeUpdate();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ?");
			statement.setInt(1, objectId);
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("Could not delete item " + this + " in DB: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	@Override
	public void restore()
	{
		getItemsByLocation(ItemLocation.AUCTION);
	}

	protected void getItemsByLocation(ItemLocation loc)
	{
		_items.clear();

		ItemInstance inst = null;
		int objectId, item_id, enchant_level, custom_type1, custom_type2, visual_itemId;
		long count;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, custom_type1, custom_type2, visual_itemId FROM items WHERE loc = ?");
			statement.setString(1, loc.name());
			rset = statement.executeQuery();
			while (rset.next())
			{
				objectId = rset.getInt(1);
				item_id = rset.getInt("item_id");
				count = rset.getLong("count");
				enchant_level = rset.getInt("enchant_level");
				custom_type1 = rset.getInt("custom_type1");
				custom_type2 = rset.getInt("custom_type2");
				visual_itemId = rset.getInt("visual_itemId");
				
				final Item item = ItemsParser.getInstance().getTemplate(item_id);
				if (item == null)
				{
					_log.error("Item item_id=" + item_id + " not known, object_id=" + objectId);
					return;
				}
				inst = new ItemInstance(objectId, item);
				inst.setCount(count);
				inst.setEnchantLevel(enchant_level);
				inst.setCustomType1(custom_type1);
				inst.setCustomType2(custom_type2);
				inst.setVisualItemId(visual_itemId);
				if (inst.isEquipable())
				{
					inst.restoreAttributes();
				}
				_items.add(inst);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error while restore items from loc:" + loc.toString(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public static final ItemAuction getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemAuction _instance = new ItemAuction();
	}
}