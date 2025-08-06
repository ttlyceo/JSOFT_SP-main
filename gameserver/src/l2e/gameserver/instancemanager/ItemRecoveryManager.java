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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.ItemRecovery;
import l2e.gameserver.model.items.instance.ItemInstance;

/**
 * Rework by LordWinter 30.01.2021
 */
public class ItemRecoveryManager extends LoggerObject
{
	private final Map<Integer, Map<Integer, ItemRecovery>> _itemList = new ConcurrentHashMap<>();
	
	public ItemRecoveryManager()
	{
		_itemList.clear();
        load();
    }

	public void load()
	{
		int count = 0;
        ItemRecovery item;
		final var now = System.currentTimeMillis();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM item_recovery WHERE time > 0 AND time < ?");
			statement.setLong(1, now);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("SELECT * FROM item_recovery");
			rset = statement.executeQuery();
			while (rset.next())
			{
				item = new ItemRecovery();
				item.setCharId(rset.getInt("charId"));
				item.setItemId(rset.getInt("item_id"));
				item.setObjectId(rset.getInt("object_id"));
				item.setCount(rset.getLong("count"));
				item.setEnchantLevel(rset.getInt("enchant_level"));
				item.setAugmentationId(rset.getInt("augmentation"));
				item.setElementals(rset.getString("elementals"));
				item.setTime(rset.getLong("time"));
				if (!_itemList.containsKey(item.getCharId()))
				{
					_itemList.put(item.getCharId(), new ConcurrentHashMap<>());
				}
				_itemList.get(item.getCharId()).put(item.getObjectId(), item);
				count++;
			}
		}
		catch (final Exception e)
		{
			warn("Could not load recovery items: " + e.getMessage());
        }
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		info("Loaded " + count + " delete items.");
	}
	
	public List<ItemRecovery> getAllRemoveItems(int playerObjId)
	{
		if (_itemList != null && !_itemList.isEmpty() && _itemList.containsKey(playerObjId))
		{
			final var it = _itemList.get(playerObjId).values().iterator();
			while (it.hasNext())
			{
				final ItemRecovery value = it.next();
				if (value != null && value.getTime() < System.currentTimeMillis())
				{
					deleteItem(value.getObjectId());
					it.remove();
				}
			}
			return _itemList.get(playerObjId).values().stream().toList();
		}
		return null;
	}
	
	private Map<Integer, ItemRecovery> getAllRecoveryItems(int playerObjId)
	{
		if (_itemList != null && !_itemList.isEmpty() && _itemList.containsKey(playerObjId))
		{
			final var it = _itemList.get(playerObjId).values().iterator();
			while (it.hasNext())
			{
				final ItemRecovery value = it.next();
				if (value != null && value.getTime() < System.currentTimeMillis())
				{
					deleteItem(value.getObjectId());
					it.remove();
				}
			}
			return _itemList.get(playerObjId);
		}
		return null;
	}
	
	public ItemRecovery getRecoveryItem(int playerObjId, int itemId)
	{
		if (_itemList != null && !_itemList.isEmpty() && _itemList.containsKey(playerObjId))
		{
			final var it = _itemList.get(playerObjId).values().iterator();
			while (it.hasNext())
			{
				final ItemRecovery value = it.next();
				if (value != null)
				{
					if (value.getTime() < System.currentTimeMillis())
					{
						it.remove();
					}
					if (value.getItemId() == itemId)
					{
						return value;
					}
				}
			}
		}
		return null;
	}

	private void deleteItem(int objId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM item_recovery WHERE object_id=?");
			statement.setInt(1, objId);
            statement.execute();
		}
		catch (final Exception e)
		{
			warn("Could not delete recovery item: " + e.getMessage());
        }
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
    }

	public boolean recoveryItem(int objectId, Player player)
	{
		if (!_itemList.containsKey(player.getObjectId()))
		{
			return false;
		}
		
		final var items = getAllRecoveryItems(player.getObjectId());
		if (items == null || items.isEmpty())
		{
			return false;
		}
		
		final var item = items.get(objectId);
		if (item != null)
		{
			final var itemRecovery = new ItemInstance(IdFactory.getInstance().getNextId(), item.getItemId());
			if (item.getEnchantLevel() != 0)
			{
				itemRecovery.setEnchantLevel(item.getEnchantLevel());
			}
			
			if (item.getAugmentationId() != -1)
			{
				itemRecovery.setAugmentation(new Augmentation(item.getAugmentationId()));
			}
			
			if (item.getElementals() != null && !item.getElementals().isEmpty())
			{
				final String[] elements = item.getElementals().split(";");
				for (final String el : elements)
				{
					final String[] element = el.split(":");
					if (element != null)
					{
						itemRecovery.setElementAttr(Byte.parseByte(element[0]), Integer.parseInt(element[1]));
					}
				}
			}
			
			itemRecovery.setCount(item.getCount());
			player.addItem("Recovery Item", itemRecovery, player, true);
			deleteItem(item.getObjectId());
			_itemList.get(player.getObjectId()).remove(item.getObjectId());
			return true;
		}
		return false;
    }
	
	public void saveToRecoveryItem(Player player, ItemInstance item, long count)
	{
		if (!_itemList.containsKey(player.getObjectId()))
		{
			_itemList.put(player.getObjectId(), new ConcurrentHashMap<>());
		}
		
		var itemRec = getRecoveryItem(player.getObjectId(), item.getId());
		if (itemRec == null || !item.isStackable())
		{
			itemRec = new ItemRecovery();
			itemRec.setCharId(player.getObjectId());
			itemRec.setItemId(item.getId());
			itemRec.setObjectId(IdFactory.getInstance().getNextId());
			itemRec.setCount(count);
			itemRec.setEnchantLevel(item.getEnchantLevel());
			itemRec.setAugmentationId(item.getAugmentation() != null ? item.getAugmentation().getAttributes() : -1);
			String elements = "";
			if (item.getElementals() != null)
			{
				for (final Elementals elm : item.getElementals())
				{
					elements += "" + elm.getElement() + ":" + elm.getValue() + ";";
				}
			}
			itemRec.setElementals(elements);
			itemRec.setTime(System.currentTimeMillis() + (Config.RECOVERY_ITEMS_HOURS * 3600000L));
		}
		else
		{
			itemRec.setCount(itemRec.getCount() + count);
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO item_recovery (charId, item_id, object_id, count, enchant_level, augmentation, elementals, time) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE count=?");
			statement.setInt(1, itemRec.getCharId());
			statement.setInt(2, itemRec.getItemId());
			statement.setInt(3, itemRec.getObjectId());
			statement.setLong(4, itemRec.getCount());
			statement.setInt(5, itemRec.getEnchantLevel());
			statement.setInt(6, itemRec.getAugmentationId());
			statement.setString(7, itemRec.getElementals());
			statement.setLong(8, itemRec.getTime());
			statement.setLong(9, itemRec.getCount());
			statement.executeUpdate();
			_itemList.get(itemRec.getCharId()).put(itemRec.getObjectId(), itemRec);
		}
		catch (final SQLException e)
		{
			warn("Could not save recovery item: " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

    public static ItemRecoveryManager getInstance()
    {
		return SingletonHolder._instance;
    }

    private static class SingletonHolder
    {
		protected static final ItemRecoveryManager _instance = new ItemRecoveryManager();
    }
}