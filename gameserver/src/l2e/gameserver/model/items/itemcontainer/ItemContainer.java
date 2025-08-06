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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.math.SafeMath;
import l2e.gameserver.Config;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;

public abstract class ItemContainer
{
	protected static final Logger _log = LoggerFactory.getLogger(ItemContainer.class);
	
	protected final List<ItemInstance> _items = new CopyOnWriteArrayList<>();
	
	protected ItemContainer()
	{
	}
	
	protected abstract Creature getOwner();
	
	protected abstract ItemLocation getBaseLocation();
	
	public String getName()
	{
		return "ItemContainer";
	}
	
	public int getOwnerId()
	{
		return getOwner() == null ? 0 : getOwner().getObjectId();
	}
	
	public int getSize()
	{
		return _items.size();
	}
	
	public ItemInstance[] getItems()
	{
		return _items.toArray(new ItemInstance[_items.size()]);
	}
	
	public ItemInstance getItemByItemId(int itemId)
	{
		for (final var item : _items)
		{
			if ((item != null) && (item.getId() == itemId))
			{
				return item;
			}
		}
		return null;
	}
	
	public boolean haveItemsCountNotEquip(int itemId, long count)
	{
		long amount = 0;
		
		for (final var item : _items)
		{
			if ((item != null) && (item.getId() == itemId))
			{
				if (item.isEquipable() && item.isEquipped())
				{
					continue;
				}
				
				if (item.isEquipable())
				{
					amount++;
					if (amount >= count)
					{
						break;
					}
				}
				else
				{
					amount = item.getCount();
				}
			}
		}
		return amount >= count ? true : false;
	}
	
	public List<ItemInstance> getItemsByItemId(int itemId)
	{
		final List<ItemInstance> returnList = new LinkedList<>();
		
		for (final var item : _items)
		{
			if ((item != null) && (item.getId() == itemId))
			{
				returnList.add(item);
			}
		}
		return returnList;
	}
	
	public ItemInstance getItemByItemId(int itemId, ItemInstance itemToIgnore)
	{
		for (final var item : _items)
		{
			if ((item != null) && (item.getId() == itemId) && !item.equals(itemToIgnore))
			{
				return item;
			}
		}
		return null;
	}
	
	public ItemInstance getItemByObjectId(int objectId)
	{
		for (final var item : _items)
		{
			if ((item != null) && (item.getObjectId() == objectId))
			{
				return item;
			}
		}
		return null;
	}
	
	public long getInventoryItemCount(int itemId, int enchantLevel)
	{
		return getInventoryItemCount(itemId, enchantLevel, true);
	}
	
	public long getInventoryItemCount(int itemId, int enchantLevel, boolean includeEquipped)
	{
		long count = 0;
		
		for (final var item : _items)
		{
			if ((item.getId() == itemId) && ((item.getEnchantLevel() == enchantLevel) || (enchantLevel < 0)) && (includeEquipped || !item.isEquipped()))
			{
				if (item.isStackable())
				{
					count = item.getCount();
				}
				else
				{
					count++;
				}
			}
		}
		return count;
	}
	
	public long getInventoryItemCount(int itemId, int enchantLevel, Augmentation augment, Elementals[] elementals, boolean includeEquipped, boolean checkParams)
	{
		long count = 0;
		
		for (final var item : _items)
		{
			if (item.getId() == itemId)
			{
				if (!checkParams)
				{
					if (item.isStackable())
					{
						count = item.getCount();
					}
					else
					{
						count++;
					}
				}
				else
				{
					if (item.getEnchantLevel() == enchantLevel && item.getAugmentation() == augment)
					{
						if (item.getElementals() != null && elementals != null)
						{
							boolean isValid = true;
							for (final var el : elementals)
							{
								final var e = item.getElemental(el.getElement());
								if (e == null || e.getValue() != el.getValue())
								{
									isValid = false;
								}
							}
							
							if (!isValid)
							{
								continue;
							}
						}
						
						if (item.isStackable())
						{
							count = item.getCount();
						}
						else
						{
							count++;
						}
					}
				}
			}
		}
		return count;
	}
	
	public ItemInstance getInventoryNeedItem(int itemId, int enchantLevel, Augmentation augment, Elementals[] elementals, boolean includeEquipped, boolean checkParams)
	{
		for (final var item : _items)
		{
			if (item.getId() == itemId)
			{
				if (!checkParams)
				{
					return item;
				}
				else
				{
					if (item.getEnchantLevel() == enchantLevel && item.getAugmentation() == augment)
					{
						if (item.getElementals() != null && elementals != null)
						{
							boolean isValid = true;
							for (final var el : elementals)
							{
								final var e = item.getElemental(el.getElement());
								if (e == null || e.getValue() != el.getValue())
								{
									isValid = false;
								}
							}
							
							if (!isValid)
							{
								continue;
							}
						}
						return item;
					}
				}
			}
		}
		return null;
	}
	
	public ItemInstance addItem(ItemInstance item, String owner, String log)
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
		
		if (getItemByObjectId(item.getObjectId()) != null)
		{
			return null;
		}
		
		if (item.isStackable())
		{
			final int itemId = item.getId();
			result = getItemByItemId(itemId);
			if (result != null)
			{
				synchronized (result)
				{
					result.setCount(SafeMath.addAndLimit(item.getCount(), result.getCount()));
					result.updateDatabase();
					removeItem(item);
				}
			}
		}
		
		if (result == null)
		{
			_items.add(item);
			result = item;
			
			addItem(result);
		}
		return result;
	}
	
	public ItemInstance addItem(String process, ItemInstance item, Player actor, Object reference)
	{
		final var olditem = getItemByItemId(item.getId());
		
		if ((olditem != null) && olditem.isStackable())
		{
			final long count = item.getCount();
			olditem.changeCount(process, count, actor, reference);
			olditem.setLastChange(ItemInstance.MODIFIED);
			
			ItemsParser.getInstance().destroyItem(process, item, actor, reference);
			item.updateDatabase();
			item = olditem;
			
			if ((item.getId() == PcInventory.ADENA_ID) && (count < (10000 * Config.RATE_DROP_ADENA)))
			{
				if ((GameTimeController.getInstance().getGameTicks() % 5) == 0)
				{
					item.updateDatabase();
				}
			}
			else
			{
				item.updateDatabase();
			}
		}
		else
		{
			item.setOwnerId(process, getOwnerId(), actor, reference);
			item.setItemLocation(getBaseLocation());
			item.setLastChange((ItemInstance.ADDED));
			
			addItem(item);
			item.updateDatabase();
		}
		
		if (actor != null && item != null)
		{
			final var newItem = actor.getInventory().getItemByItemId(item.getId());
			if (newItem != null)
			{
				if (newItem.isEtcItem())
				{
					actor.checkToEquipArrows(newItem);
				}
				else if (newItem.isTalisman())
				{
					actor.checkCombineTalisman(newItem, false);
				}
			}
		}
		refreshWeight();
		return item;
	}
	
	public ItemInstance addWaheHouseItem(String process, ItemInstance item, Player actor, Object reference)
	{
		final var olditem = getItemByItemId(item.getId());
		
		if ((olditem != null) && olditem.isStackable())
		{
			final long count = item.getCount();
			olditem.changeCount(process, count, actor, reference);
			olditem.setLastChange(ItemInstance.MODIFIED);
			
			ItemsParser.getInstance().destroyItem(process, item, actor, reference);
			item.updateDatabase();
			item = olditem;
			
			if ((item.getId() == PcInventory.ADENA_ID) && (count < (10000 * Config.RATE_DROP_ADENA)))
			{
				if ((GameTimeController.getInstance().getGameTicks() % 5) == 0)
				{
					item.updateDatabase();
				}
			}
			else
			{
				item.updateDatabase();
			}
		}
		else
		{
			item.setOwnerId(process, getOwnerId(), actor, reference);
			item.setItemLocation(getBaseLocation());
			item.setLastChange((ItemInstance.ADDED));
			
			addItem(item);
			item.updateDatabase();
		}
		refreshWeight();
		return item;
	}
	
	public ItemInstance addItem(String process, int itemId, long count, Player actor, Object reference)
	{
		var item = getItemByItemId(itemId);
		
		if ((item != null) && item.isStackable())
		{
			item.changeCount(process, count, actor, reference);
			item.setLastChange(ItemInstance.MODIFIED);
			final double adenaRate = Config.RATE_DROP_ADENA;
			if ((itemId == PcInventory.ADENA_ID) && (count < (10000 * adenaRate)))
			{
				if ((GameTimeController.getInstance().getGameTicks() % 5) == 0)
				{
					item.updateDatabase();
				}
			}
			else
			{
				item.updateDatabase();
			}
		}
		else
		{
			for (int i = 0; i < count; i++)
			{
				final var template = ItemsParser.getInstance().getTemplate(itemId);
				if (template == null)
				{
					_log.warn((actor != null ? "[" + actor.getName(null) + "] " : "") + "Invalid ItemId requested: ", itemId);
					return null;
				}
				
				item = ItemsParser.getInstance().createItem(process, itemId, template.isStackable() ? count : 1, actor, reference);
				item.setOwnerId(getOwnerId());
				item.setItemLocation(getBaseLocation());
				item.setLastChange(ItemInstance.ADDED);
				
				addItem(item);
				item.updateDatabase();
				
				if (template.isStackable() || !Config.MULTIPLE_ITEM_DROP)
				{
					break;
				}
			}
		}
		
		if (actor != null && item != null)
		{
			final var newItem = actor.getInventory().getItemByItemId(item.getId());
			if (newItem != null)
			{
				if (newItem.isEtcItem())
				{
					actor.checkToEquipArrows(newItem);
				}
				else if (newItem.isTalisman())
				{
					actor.checkCombineTalisman(newItem, false);
				}
			}
		}
		refreshWeight();
		return item;
	}
	
	public ItemInstance addWareHouseItem(String process, int itemId, long count, Player actor, Object reference)
	{
		var item = getItemByItemId(itemId);
		
		if ((item != null) && item.isStackable())
		{
			item.changeCount(process, count, actor, reference);
			item.setLastChange(ItemInstance.MODIFIED);
			final double adenaRate = Config.RATE_DROP_ADENA;
			if ((itemId == PcInventory.ADENA_ID) && (count < (10000 * adenaRate)))
			{
				if ((GameTimeController.getInstance().getGameTicks() % 5) == 0)
				{
					item.updateDatabase();
				}
			}
			else
			{
				item.updateDatabase();
			}
		}
		else
		{
			for (int i = 0; i < count; i++)
			{
				final var template = ItemsParser.getInstance().getTemplate(itemId);
				if (template == null)
				{
					_log.warn((actor != null ? "[" + actor.getName(null) + "] " : "") + "Invalid ItemId requested: ", itemId);
					return null;
				}
				
				item = ItemsParser.getInstance().createItem(process, itemId, template.isStackable() ? count : 1, actor, reference);
				item.setOwnerId(getOwnerId());
				item.setItemLocation(getBaseLocation());
				item.setLastChange(ItemInstance.ADDED);
				
				addItem(item);
				item.updateDatabase();
				
				if (template.isStackable() || !Config.MULTIPLE_ITEM_DROP)
				{
					break;
				}
			}
		}
		refreshWeight();
		return item;
	}
	
	public ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, Player actor, Object reference)
	{
		if (target == null)
		{
			return null;
		}
		
		final var sourceitem = getItemByObjectId(objectId);
		if (sourceitem == null)
		{
			return null;
		}
		var targetitem = sourceitem.isStackable() ? target.getItemByItemId(sourceitem.getId()) : null;
		
		synchronized (sourceitem)
		{
			if (getItemByObjectId(objectId) != sourceitem)
			{
				return null;
			}
			
			if (count > sourceitem.getCount())
			{
				count = sourceitem.getCount();
			}
			
			if ((sourceitem.getCount() == count) && (targetitem == null))
			{
				removeItem(sourceitem);
				target.addItem(process, sourceitem, actor, reference);
				targetitem = sourceitem;
			}
			else
			{
				if (sourceitem.getCount() > count)
				{
					sourceitem.changeCount(process, -count, actor, reference);
				}
				else
				{
					removeItem(sourceitem);
					ItemsParser.getInstance().destroyItem(process, sourceitem, actor, reference);
				}
				
				if (targetitem != null)
				{
					targetitem.changeCount(process, count, actor, reference);
				}
				else
				{
					targetitem = target.addItem(process, sourceitem.getId(), count, actor, reference);
				}
			}
			
			sourceitem.updateDatabase(true);
			if ((targetitem != sourceitem) && (targetitem != null))
			{
				targetitem.updateDatabase();
			}
			if (sourceitem.isAugmented())
			{
				sourceitem.getAugmentation().removeBonus(actor);
			}
			refreshWeight();
			target.refreshWeight();
		}
		return targetitem;
	}
	
	public ItemInstance destroyItem(String process, ItemInstance item, Player actor, Object reference)
	{
		return destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	public ItemInstance destroyItem(String process, ItemInstance item, long count, Player actor, Object reference)
	{
		synchronized (item)
		{
			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(ItemInstance.MODIFIED);
				
				if ((process != null) || ((GameTimeController.getInstance().getGameTicks() % 10) == 0))
				{
					item.updateDatabase();
				}
				refreshWeight();
			}
			else
			{
				if (item.getCount() < count)
				{
					return null;
				}
				
				final boolean removed = removeItem(item);
				if (!removed)
				{
					return null;
				}
				
				ItemsParser.getInstance().destroyItem(process, item, actor, reference);
				
				item.updateDatabase();
				refreshWeight();
			}
		}
		return item;
	}
	
	public ItemInstance destroyItem(String process, int objectId, long count, Player actor, Object reference)
	{
		final var item = getItemByObjectId(objectId);
		if (item == null)
		{
			return null;
		}
		return destroyItem(process, item, count, actor, reference);
	}
	
	public ItemInstance destroyItemByItemId(String process, int itemId, long count, Player actor, Object reference)
	{
		final var item = getItemByItemId(itemId);
		if (item == null)
		{
			return null;
		}
		return destroyItem(process, item, count, actor, reference);
	}
	
	public boolean destroyItemByItemId(int itemId, long count, Player owner, String log)
	{
		ItemInstance item;
		if ((item = getItemByItemId(itemId)) == null)
		{
			return false;
		}
		
		synchronized (item)
		{
			destroyItem(log, item, count, owner, null);
		}
		return true;
	}
	
	public ItemInstance destroyItemByObjectId(int objectId, long count, Player owner, Object reference)
	{
		ItemInstance item;
		if ((item = getItemByObjectId(objectId)) == null)
		{
			return null;
		}
		
		synchronized (item)
		{
			return destroyItem("Remove", item, count, owner, reference);
		}
	}
	
	public void destroyAllItems(String process, Player actor, Object reference)
	{
		for (final var item : _items)
		{
			if (item != null)
			{
				destroyItem(process, item, actor, reference);
			}
		}
	}
	
	public long getAdena()
	{
		long count = 0;
		
		for (final var item : _items)
		{
			if ((item != null) && (item.getId() == PcInventory.ADENA_ID))
			{
				count = item.getCount();
				return count;
			}
		}
		return count;
	}
	
	protected void addItem(ItemInstance item)
	{
		_items.add(item);
	}
	
	public boolean removeItem(ItemInstance item)
	{
		return _items.remove(item);
	}
	
	protected void refreshWeight()
	{
	}
	
	public void deleteMe()
	{
		if (getOwner() != null)
		{
			for (final var item : _items)
			{
				if (item != null)
				{
					item.updateDatabase(true);
					item.deleteMe();
					GameObjectsStorage.removeItem(item);
				}
			}
		}
		_items.clear();
	}
	
	public void updateDatabase()
	{
		if (getOwner() != null)
		{
			for (final var item : _items)
			{
				if (item != null)
				{
					item.updateDatabase(true);
				}
			}
		}
	}
	
	public void restore()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet inv = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time, visual_itemId, agathion_energy, is_event FROM items WHERE owner_id=? AND (loc=?)");
			statement.setInt(1, getOwnerId());
			statement.setString(2, getBaseLocation().name());
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
				final Player owner = getOwner() == null ? null : getOwner().getActingPlayer();
				if (item.isStackable() && (getItemByItemId(item.getId()) != null))
				{
					addItem("Restore", item, owner, null);
				}
				else
				{
					addItem(item);
				}
			}
			refreshWeight();
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
	
	public boolean validateCapacity(long slots)
	{
		return true;
	}
	
	public boolean validateWeight(long weight)
	{
		return true;
	}
	
	public boolean validateCapacityByItemId(int itemId, long count)
	{
		final var template = ItemsParser.getInstance().getTemplate(itemId);
		return (template == null) || (template.isStackable() ? validateCapacity(1) : validateCapacity(count));
	}
	
	public boolean validateWeightByItemId(int itemId, long count)
	{
		final var template = ItemsParser.getInstance().getTemplate(itemId);
		return (template == null) || validateWeight(template.getWeight() * count);
	}
	
	public boolean destroyItemByItemId(int itemId, long count, String log)
	{
		if (getOwner().isPlayer())
		{
			return destroyItemByItemId(itemId, count, getOwner().getActingPlayer(), log);
		}
		return false;
	}
	
	public final boolean hasSelfResurrection()
	{
		for (final var item : _items)
		{
			if ((item != null) && (item.getItem().isSelfResurrection()))
			{
				return true;
			}
		}
		return false;
	}
}